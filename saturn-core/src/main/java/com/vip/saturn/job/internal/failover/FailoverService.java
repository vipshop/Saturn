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
package com.vip.saturn.job.internal.failover;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.internal.storage.LeaderExecutionCallback;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 作业失效转移服务.
 *
 * @author dylan.xue
 */
public class FailoverService extends AbstractSaturnService {

	private static Logger log = LoggerFactory.getLogger(FailoverService.class);

	public FailoverService(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	@Override
	public void start() {
	}

	/**
	 * 设置失效的分片项标记.
	 *
	 * @param item 崩溃的作业项
	 */
	public void createCrashedFailoverFlag(final int item) {
		if (!isFailoverAssigned(item)) {
			try {
				getJobNodeStorage().getClient().create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT)
						.forPath(JobNodePath.getNodeFullPath(jobName, FailoverNode.getItemsNode(item)));
				log.info("{} - {} create failover flag of item {}", executorName, jobName, item);
			} catch (KeeperException.NodeExistsException e) { // NOSONAR
				log.debug("{} - {} create failover flag of item {} failed, because it is already existing",
						executorName, jobName, item);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public boolean isFailoverAssigned(final Integer item) {
		return getJobNodeStorage().isJobNodeExisted(FailoverNode.getExecutionFailoverNode(item));
	}

	/**
	 * 如果需要失效转移, 则设置作业失效转移.
	 */
	public void failoverIfNecessary() {
		if (!needFailover()) {
			return;
		}
		getJobNodeStorage().executeInLeader(FailoverNode.LATCH, new FailoverLeaderExecutionCallback(), 1,
				TimeUnit.MINUTES, new FailoverTimeoutLeaderExecutionCallback());
	}

	private boolean needFailover() {
		return getJobNodeStorage().isJobNodeExisted(FailoverNode.ITEMS_ROOT)
				&& !getJobNodeStorage().getJobNodeChildrenKeys(FailoverNode.ITEMS_ROOT).isEmpty()
				&& getJobNodeStorage().isJobNodeExisted(ConfigurationNode.ENABLED)
				&& Boolean.parseBoolean(getJobNodeStorage().getJobNodeData(ConfigurationNode.ENABLED));
	}

	/**
	 * 更新执行完毕失效转移的分片项状态.
	 *
	 * @param item 执行完毕失效转移的分片项列表
	 */
	public void updateFailoverComplete(final Integer item) {
		getJobNodeStorage().removeJobNodeIfExisted(FailoverNode.getExecutionFailoverNode(item));
	}

	/**
	 * 获取运行在本作业服务器的失效转移序列号.
	 *
	 * @return 运行在本作业服务器的失效转移序列号
	 */
	public List<Integer> getLocalHostFailoverItems() {
		List<String> items = getJobNodeStorage().getJobNodeChildrenKeys(ExecutionNode.ROOT);
		List<Integer> result = new ArrayList<>(items.size());
		for (String each : items) {
			int item = Integer.parseInt(each);
			String node = FailoverNode.getExecutionFailoverNode(item);
			if (getJobNodeStorage().isJobNodeExisted(node)
					&& executorName.equals(getJobNodeStorage().getJobNodeDataDirectly(node))) {
				result.add(item);
			}
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * 获取运行在本作业服务器的被失效转移的序列号.
	 *
	 * @return 运行在本作业服务器的被失效转移的序列号
	 */
	public List<Integer> getLocalHostTakeOffItems() {
		List<Integer> shardingItems = jobScheduler.getShardingService().getLocalHostShardingItems();
		List<Integer> result = new ArrayList<>(shardingItems.size());
		for (int each : shardingItems) {
			if (getJobNodeStorage().isJobNodeExisted(FailoverNode.getExecutionFailoverNode(each))) {
				result.add(each);
			}
		}
		return result;
	}

	/**
	 * 删除作业失效转移信息.
	 */
	public void removeFailoverInfo() {
		getJobNodeStorage().removeJobNodeIfExisted(FailoverNode.ITEMS_ROOT);
		for (String each : getJobNodeStorage().getJobNodeChildrenKeys(ExecutionNode.ROOT)) {
			getJobNodeStorage().removeJobNodeIfExisted(FailoverNode.getExecutionFailoverNode(Integer.parseInt(each)));
		}
	}

	class FailoverLeaderExecutionCallback implements LeaderExecutionCallback {

		@Override
		public void execute() {
			if (!needFailover()) {
				return;
			}
			if (jobScheduler == null) {
				return;
			}
			if (coordinatorRegistryCenter.isExisted(SaturnExecutorsNode.getExecutorNoTrafficNodePath(executorName))) {
				return;
			}
			if (!jobScheduler.getConfigService().getPreferList().contains(executorName)
					&& !jobScheduler.getConfigService().isUseDispreferList()) {
				return;
			}
			List<String> items = getJobNodeStorage().getJobNodeChildrenKeys(FailoverNode.ITEMS_ROOT);
			if (items != null && !items.isEmpty()) {
				int crashedItem = Integer
						.parseInt(getJobNodeStorage().getJobNodeChildrenKeys(FailoverNode.ITEMS_ROOT).get(0));
				log.info("[{}] msg=Elastic job: failover job begin, crashed item:{}.", jobName, crashedItem);
				getJobNodeStorage().fillEphemeralJobNode(FailoverNode.getExecutionFailoverNode(crashedItem),
						executorName);
				getJobNodeStorage().removeJobNodeIfExisted(FailoverNode.getItemsNode(crashedItem));
				jobScheduler.triggerJob();
			}
		}
	}

	class FailoverTimeoutLeaderExecutionCallback implements LeaderExecutionCallback {

		@Override
		public void execute() {
			log.warn("Failover leader election timeout with a minute");
		}
	}
}
