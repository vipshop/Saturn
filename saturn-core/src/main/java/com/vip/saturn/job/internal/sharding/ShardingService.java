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
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 作业分片服务.
 * 
 * 
 */
public class ShardingService extends AbstractSaturnService {
	static Logger log = LoggerFactory.getLogger(ShardingService.class);

	private LeaderElectionService leaderElectionService;

	private ServerService serverService;

	private ExecutionService executionService;

	private NamespaceShardingContentService namespaceShardingContentService;

	public final static String SHARDING_UN_NECESSARY = "0";

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
	 * @return 是否需要重分片
	 */
	public boolean isNeedSharding() {
		return getJobNodeStorage().isJobNodeExisted(ShardingNode.NECESSARY)
				&& !SHARDING_UN_NECESSARY.equals(getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.NECESSARY));
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
			log.error(e.getMessage(), e);
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
			log.error(e.getMessage(), e);
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
		if (getDataStat == null || SHARDING_UN_NECESSARY.equals(getDataStat.getData())) {
			return;
		}
		if (blockUntilShardingComplatedIfNotLeader()) {
			return;
		}
		waitingOtherJobCompleted();
		getJobNodeStorage().fillEphemeralJobNode(ShardingNode.PROCESSING, "");
		try {
			clearShardingInfo();
			int retryCount = 3;
			while (!isShutdown) {
				boolean needRetry = false;
				int version = getDataStat.getVersion();
				Map<String, List<Integer>> shardingItems = namespaceShardingContentService.getShardContent(jobName,
						getDataStat.getData());
				try {
					CuratorTransactionFinal curatorTransactionFinal = getJobNodeStorage().getClient().inTransaction()
							.check().forPath("/").and();
					for (Entry<String, List<Integer>> entry : shardingItems.entrySet()) {
						curatorTransactionFinal.create()
								.forPath(
										JobNodePath.getNodeFullPath(jobName,
												ShardingNode.getShardingNode(entry.getKey())),
								ItemUtils.toItemsString(entry.getValue()).getBytes(StandardCharsets.UTF_8)).and();
					}
					curatorTransactionFinal.setData().withVersion(version)
							.forPath(JobNodePath.getNodeFullPath(jobName, ShardingNode.NECESSARY),
									SHARDING_UN_NECESSARY.getBytes(StandardCharsets.UTF_8))
							.and();
					curatorTransactionFinal.commit();
				} catch (BadVersionException e) {
					needRetry = true;
					retryCount--;
				} catch (Exception e) {
					log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, "Commit shards failed"), e);
				}
				if (needRetry) {
					if (retryCount >= 0) {
						log.info(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName,
								"Bad version because of concurrency, will retry to get shards later"));
						Thread.sleep(200L); // NOSONAR
						getDataStat = getNecessaryDataStat();
					} else {
						log.warn(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName,
								"Bad version because of concurrency, give up to retry"));
						break;
					}
				} else {
					break;
				}
			}
		} catch (Exception e) {
			log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, e.getMessage()), e);
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
		for (;;) {
			if (isShutdown) {
				throw new JobShuttingDownException();
			}
			if (leaderElectionService.isLeader()) {
				return false;
			}
			if (!(isNeedSharding() || getJobNodeStorage().isJobNodeExisted(ShardingNode.PROCESSING))) {
				return true;
			}
			log.debug("[{}] msg=Sleep short time until sharding completed", jobName);
			BlockUtils.waitingShortTime();
		}
	}

	private void waitingOtherJobCompleted() {
		while (!isShutdown && executionService.hasRunningItems()) {
			log.info("[{}] msg=Sleep short time until other job completed.", jobName);
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
		if (!getJobNodeStorage().isJobNodeExisted(ShardingNode.getShardingNode(executorName))) {
			return Collections.<Integer> emptyList();
		}
		return ItemUtils
				.toItemList(getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.getShardingNode(executorName)));
	}

	@Override
	public void shutdown() {
		isShutdown = true;
		necessaryWatcher = null; // cannot registerNecessaryWatcher
	}

}
