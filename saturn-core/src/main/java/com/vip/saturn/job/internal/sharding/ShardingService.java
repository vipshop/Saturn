/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */
package com.vip.saturn.job.internal.sharding;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.exception.JobShuttingDownException;
import com.vip.saturn.job.internal.election.LeaderElectionService;
import com.vip.saturn.job.internal.execution.ExecutionService;
import com.vip.saturn.job.internal.server.ServerService;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.sharding.service.NamespaceShardingContentService;
import com.vip.saturn.job.utils.BlockUtils;
import com.vip.saturn.job.utils.ItemUtils;
import com.vip.saturn.job.utils.LogUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 作业分片服务.
 *
 *
 */
public class ShardingService extends AbstractSaturnService {
	public static final String SHARDING_UN_NECESSARY = "0";

	private static final Logger log = LoggerFactory.getLogger(ShardingService.class);

	private LeaderElectionService leaderElectionService;

	private ServerService serverService;

	private ExecutionService executionService;

	private NamespaceShardingContentService namespaceShardingContentService;

	private volatile boolean isShutdown;

	private CuratorWatcher necessaryWatcher;

	public ShardingService(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	@Override
	public synchronized void start() {
		leaderElectionService = jobScheduler.getLeaderElectionService();
		serverService = jobScheduler.getServerService();
		executionService = jobScheduler.getExecutionService();
		namespaceShardingContentService = new NamespaceShardingContentService(
				(CuratorFramework) coordinatorRegistryCenter.getRawClient());
	}

	/**
	 * 判断是否需要重分片.
	 * necessar节点的内容来自于AbstractAsyncShardingTask::run()
	 * @return 是否需要重分片
	 */
	public boolean isNeedSharding() {
		return getJobNodeStorage().isJobNodeExisted(ShardingNode.NECESSARY) && !SHARDING_UN_NECESSARY
				.equals(getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.NECESSARY));
	}

	public void registerNecessaryWatcher(CuratorWatcher necessaryWatcher) {
		this.necessaryWatcher = necessaryWatcher;
		registerNecessaryWatcher();
	}

	public void registerNecessaryWatcher() {
		try {
			if (necessaryWatcher != null) {
				getJobNodeStorage().getClient().checkExists().usingWatcher(necessaryWatcher)
						.forPath(JobNodePath.getNodeFullPath(jobName, ShardingNode.NECESSARY));
			}
		} catch (Exception e) {
			LogUtils.error(log, jobName, e.getMessage(), e);
		}
	}

	private GetDataStat getNecessaryDataStat() {
		String data = null;
		int version = -1;
		try {
			Stat stat = new Stat();
			byte[] bs = null;
			if (necessaryWatcher != null) {
				bs = getJobNodeStorage().getClient().getData().storingStatIn(stat).usingWatcher(necessaryWatcher)
						.forPath(JobNodePath.getNodeFullPath(jobName, ShardingNode.NECESSARY));
			} else {
				bs = getJobNodeStorage().getClient().getData().storingStatIn(stat)
						.forPath(JobNodePath.getNodeFullPath(jobName, ShardingNode.NECESSARY));
			}
			if (bs != null) {
				data = new String(bs, "UTF-8");
			}
			version = stat.getVersion();
		} catch (Exception e) {
			LogUtils.error(log, jobName, e.getMessage(), e);
		}
		return new GetDataStat(data, version);
	}

	/**
	 * 如果需要分片且当前节点为主节点, 则作业分片.
	 */
	public synchronized void shardingIfNecessary() throws JobShuttingDownException {
		if (isShutdown) {
			return;
		}
		GetDataStat getDataStat = null;
		if (getJobNodeStorage().isJobNodeExisted(ShardingNode.NECESSARY)) {
			getDataStat = getNecessaryDataStat();
		}
		// sharding necessary内容为空，或者内容是"0"则返回，否则，需要进行sharding处理
		if (getDataStat == null || SHARDING_UN_NECESSARY.equals(getDataStat.getData())) {
			return;
		}
		// 如果不是leader，则等待leader处理完成（这也是一个死循环，知道满足跳出循环的条件：1. 被shutdown 2. 无须sharding而且不处于processing状态）
		if (blockUntilShardingComplatedIfNotLeader()) {
			return;
		}
		// 如果有作业分片处于running状态则等待（无限期）
		waitingOtherJobCompleted();
		// 建立一个临时节点，标记sharding处理中
		getJobNodeStorage().fillEphemeralJobNode(ShardingNode.PROCESSING, "");
		try {
			// 删除作业下面的所有JobServer的sharding节点
			clearShardingInfo();

			int maxRetryTime = 3;
			int retryCount = 0;
			while (!isShutdown) {
				int version = getDataStat.getVersion();
				// 首先尝试从job/leader/sharding/necessary节点获取，如果失败，会从$SaturnExecutors/sharding/content下面获取
				// key is executor, value is sharding items
				Map<String, List<Integer>> shardingItems = namespaceShardingContentService
						.getShardContent(jobName, getDataStat.getData());
				try {
					// 所有jobserver的（检查+创建），加上设置sharding necessary内容为0，都是一个事务
					CuratorTransactionFinal curatorTransactionFinal = getJobNodeStorage().getClient().inTransaction()
							.check().forPath("/").and();
					for (Entry<String, List<Integer>> entry : shardingItems.entrySet()) {
						curatorTransactionFinal.create().forPath(
								JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(entry.getKey())),
								ItemUtils.toItemsString(entry.getValue()).getBytes(StandardCharsets.UTF_8)).and();
					}
					curatorTransactionFinal.setData().withVersion(version)
							.forPath(JobNodePath.getNodeFullPath(jobName, ShardingNode.NECESSARY),
									SHARDING_UN_NECESSARY.getBytes(StandardCharsets.UTF_8)).and();
					curatorTransactionFinal.commit();
					break;
				} catch (BadVersionException e) {
					LogUtils.warn(log, jobName, "zookeeper bad version exception happens", e);
					if (++retryCount <= maxRetryTime) {
						LogUtils.info(log, jobName,
								"bad version because of concurrency, will retry to get shards from sharding/necessary later");
						Thread.sleep(200L); // NOSONAR
						getDataStat = getNecessaryDataStat();
					}
				} catch (Exception e) {
					LogUtils.warn(log, jobName, "commit shards failed", e);
					/**
					 * 已知场景：
					 *   异常为NoNodeException，域下作业数量大，业务容器上下线。
					 *   原因是，大量的sharding task导致计算结果有滞后，同时某些server被删除，导致commit失败，报NoNode异常。
					 *
					 * 是否需要重试：
					 *   如果作业一直处于启用状态，necessary最终会被更新正确，这时不需要主动重试。 如果重试，可能导致提前拿到数据，后面再重新拿一次数据，不过也没多大问题。
					 *   如果作业在中途禁用了，那么necessary将不会被更新，这时从necessary拿到的数据是过时的，仍然会commit失败，这时需要从content获取数据来重试。
					 *   如果是其他未知场景导致的commit失败，也是可以尝试从content获取数据来重试。
					 *   所以，为了保险起见，均从content获取数据来重试。
					 */
					if (++retryCount <= maxRetryTime) {
						LogUtils.info(log, jobName,
								"unexpected error, will retry to get shards from sharding/content later");
						// 睡一下，没必要马上重试。减少对zk的压力。
						Thread.sleep(500L); // NOSONAR
						/**
						 * 注意：
						 *   data为GET_SHARD_FROM_CONTENT_NODE_FLAG，会从sharding/content下获取数据。
						 *   version使用necessary的version。
						 */
						getDataStat = new GetDataStat(NamespaceShardingContentService.GET_SHARD_FROM_CONTENT_NODE_FLAG,
								version);
					}
				}
				if (retryCount > maxRetryTime) {
					LogUtils.warn(log, jobName, "retry time exceed {}, will give up to get shards", maxRetryTime);
					break;
				}
			}
		} catch (Exception e) {
			LogUtils.error(log, jobName, e.getMessage(), e);
		} finally {
			getJobNodeStorage().removeJobNodeIfExisted(ShardingNode.PROCESSING);
		}
	}

	/**
	 * <p>如果不是leader，等待leader分片完成，返回true；如果期间变为leader，返回false。
	 * <p>
	 *     TODO：如果业务运行时间很快，以至于在followers waitingShortTime的100ms期间，其他分片已经运行完，并且leader crash并当前follower变为leader了。
	 *     		这种情况，如果返回false，则当前线程作为leader获取分片完，会再次跑业务。 这可能不被期望。
	 *     		所以，最好是能有全局的作业级别轮次锁定。
	 * @return true or false
	 * @throws JobShuttingDownException
	 */
	private boolean blockUntilShardingComplatedIfNotLeader() throws JobShuttingDownException {
		for (; ; ) {
			if (isShutdown) {
				throw new JobShuttingDownException();
			}
			if (leaderElectionService.isLeader()) {
				return false;
			}
			if (!(isNeedSharding() || getJobNodeStorage().isJobNodeExisted(ShardingNode.PROCESSING))) {
				return true;
			}
			LogUtils.debug(log, jobName, "Sleep short time until sharding completed");
			BlockUtils.waitingShortTime();
		}
	}

	private void waitingOtherJobCompleted() {
		while (!isShutdown && executionService.hasRunningItems()) {
			LogUtils.info(log, jobName, "Sleep short time until other job completed.");
			BlockUtils.waitingShortTime();
		}
	}

	private void clearShardingInfo() {
		for (String each : serverService.getAllServers()) {
			getJobNodeStorage().removeJobNodeIfExisted(ShardingNode.getShardingNode(each));
		}
	}

	/**
	 * 获取运行在本作业服务器的分片序列号.
	 *
	 * @return 运行在本作业服务器的分片序列号
	 */
	public List<Integer> getLocalHostShardingItems() {
		String value = getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.getShardingNode(executorName));
		return ItemUtils.toItemList(value);
	}

	@Override
	public void shutdown() {
		isShutdown = true;
		necessaryWatcher = null; // cannot registerNecessaryWatcher
	}

}
