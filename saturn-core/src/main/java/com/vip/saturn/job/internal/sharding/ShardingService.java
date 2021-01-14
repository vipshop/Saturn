/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.internal.sharding;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.SaturnConstant;
import com.vip.saturn.job.exception.JobShuttingDownException;
import com.vip.saturn.job.internal.election.LeaderElectionService;
import com.vip.saturn.job.internal.execution.ExecutionService;
import com.vip.saturn.job.internal.server.ServerService;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.sharding.service.NamespaceShardingContentService;
import com.vip.saturn.job.utils.BlockUtils;
import com.vip.saturn.job.utils.ItemUtils;
import com.vip.saturn.job.utils.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
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
		// sharding neccessary内容为空，或者内容是"0"则返回，否则，需要进行sharding处理
		if (getDataStat == null || SHARDING_UN_NECESSARY.equals(getDataStat.getData())) {
			return;
		}
		// 如果不是leader，则等待leader处理完成（这也是一个死循环，知道满足跳出循环的条件：1. 被shutdown 2. 无须sharding而且不处于processing状态）
		if (blockUntilShardingComplatedIfNotLeader()) {
			return;
		}
		// 如果有作业分片处于running状态则等待（无限期）
		waitingOtherJobCompleted();
		// 建立一个临时节点，标记shardig处理中
		getJobNodeStorage().fillEphemeralJobNode(ShardingNode.PROCESSING, "");
		try {
			// 删除作业下面的所有JobServer的sharding节点
			clearShardingInfo();

			int retryCount = 3;
			while (!isShutdown) {
				boolean needRetry = false;
				int version = getDataStat.getVersion();
				// 首先尝试从job/leader/sharding/neccessary节点获取，如果失败，会从$SaturnExecutors/sharding/content下面获取
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
				} catch (BadVersionException e) {
					LogUtils.warn(log, jobName, "zookeeper bad version exception happens.", e);
					needRetry = true;
					retryCount--;
				} catch (Exception e) {
					// 可能多个sharding task导致计算结果有滞后，但是server机器已经被删除，导致commit失败
					// 实际上可能不影响最终结果，仍然能正常分配分片，因为还会有resharding事件被响应
					// 修改日志级别为warn级别，避免不必要的告警
					LogUtils.warn(log, jobName, "Commit shards failed", e);
				}
				if (needRetry) {
					if (retryCount >= 0) {
						LogUtils.info(log, jobName,
								"Bad version because of concurrency, will retry to get shards later");
						Thread.sleep(200L); // NOSONAR
						getDataStat = getNecessaryDataStat();
					} else {
						LogUtils.warn(log, jobName, "Bad version because of concurrency, give up to retry");
						break;
					}
				} else {
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
	 * 如果不是leader，等待leader分片完成，返回true；如果期间变为leader，返回false。
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
	 *
	 * @param getLocalHostFailoverItems
	 * @throws Exception
	 */
	public synchronized void removeAndCreateShardingInfo(List<Integer> getLocalHostFailoverItems) throws Exception {
		LogUtils.info(log, jobName, "removeAndCreateShardingInfo start.");
		//加分布式锁，防止多个executor对同一个节点进行更新
		CuratorFramework client = getJobNodeStorage().getClient();
		InterProcessMutex mutex = new InterProcessMutex(client,"/saturnSharding/lock");
		try {
			mutex.acquire();
			// 所有jobserver的（检查+创建），加上设置sharding necessary内容为0，都是一个事务
			CuratorTransactionFinal curatorTransactionFinal = getJobNodeStorage().getClient().inTransaction()
					.check().forPath("/").and();
			//遍历所有的servers/executorName下分片序号，判断失败的分片是否在该executorName下，是则删除
			Set<Integer> getLocalHostFailoverItemSets = new HashSet<>(getLocalHostFailoverItems);
			for (String each : serverService.getAllServers()) {
				if(StringUtils.equals(each,executorName)){
					continue;
				}
				String value = getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.getShardingNode(each));
				if(StringUtils.isEmpty(value)){
					continue;
				}
				List<Integer> getShardingItemsByexecutorName = ItemUtils.toItemList(value);
				Set<Integer> getShardingItemSetsByexecutorName = new HashSet<>(getShardingItemsByexecutorName);
				getShardingItemSetsByexecutorName.removeAll(getLocalHostFailoverItemSets);
				getJobNodeStorage().removeJobNodeIfExisted(ShardingNode.getShardingNode(each));
				curatorTransactionFinal.create().forPath(
						JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(each)),
						ItemUtils.toItemsString(new ArrayList<>(getShardingItemSetsByexecutorName)).getBytes(StandardCharsets.UTF_8)).and();
			}
			LogUtils.info(log, jobName, "removeAndCreateShardingInfo delete LocalHostFailoverItems.");
			//在local executorName下新加该失败的分片
			String getLocalHostItems = getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.getShardingNode(executorName));
			List<Integer> getLocalHostItemsList = new ArrayList<>();
			if(StringUtils.isEmpty(getLocalHostItems)){
				getLocalHostItemsList = getLocalHostFailoverItems;
			} else {
				getLocalHostItemsList = ItemUtils.toItemList(getLocalHostItems);
				getLocalHostItemsList.addAll(getLocalHostFailoverItems);
			}
			getJobNodeStorage().removeJobNodeIfExisted(ShardingNode.getShardingNode(executorName));
			curatorTransactionFinal.create().forPath(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executorName)),
					ItemUtils.toItemsString(getLocalHostItemsList).getBytes(StandardCharsets.UTF_8)).and();
			curatorTransactionFinal.commit();
			LogUtils.info(log, jobName, "removeAndCreateShardingInfo append LocalHostFailoverItems.");
		} catch (Exception e) {
			// 可能多个sharding task导致计算结果有滞后，但是server机器已经被删除，导致commit失败
			// 实际上可能不影响最终结果，仍然能正常分配分片，因为还会有resharding事件被响应
			// 修改日志级别为warn级别，避免不必要的告警
			LogUtils.warn(log, jobName, "Commit shards failed", e);
		} finally {
			mutex.release();
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
