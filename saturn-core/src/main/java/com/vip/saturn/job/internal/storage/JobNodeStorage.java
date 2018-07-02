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

package com.vip.saturn.job.internal.storage;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.SaturnConstant;
import com.vip.saturn.job.exception.JobException;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.server.ServerNode;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.reg.exception.RegExceptionHandler;
import com.vip.saturn.job.reg.zookeeper.ZookeeperConfiguration;
import com.vip.saturn.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.vip.saturn.job.utils.BlockUtils;

/**
 * 作业节点数据访问类.
 * 
 * <p>
 * 作业节点是在普通的节点前加上作业名称的前缀.
 * </p>
 * 
 * 
 */
public class JobNodeStorage {

	private static Logger log = LoggerFactory.getLogger(JobNodeStorage.class);

	private static final int MAX_DELETE_RETRY_TIMES = 10;

	private final CoordinatorRegistryCenter coordinatorRegistryCenter;

	private final JobConfiguration jobConfiguration;

	private String executorName;

	private final String jobName;

	public JobNodeStorage(final CoordinatorRegistryCenter coordinatorRegistryCenter,
			final JobConfiguration jobConfiguration) {
		this.coordinatorRegistryCenter = coordinatorRegistryCenter;
		this.jobConfiguration = jobConfiguration;
		this.jobName = jobConfiguration.getJobName();
		if (coordinatorRegistryCenter != null) {
			executorName = coordinatorRegistryCenter.getExecutorName();
		}
	}

	/**
	 * 判断作业节点是否存在.
	 * 
	 * @param node 作业节点名称
	 * @return 作业节点是否存在
	 */
	public boolean isJobNodeExisted(final String node) {
		return coordinatorRegistryCenter.isExisted(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node));
	}

	/**
	 * 判断作业是否存在.
	 * 
	 * @param jobName 作业节点名称
	 * @return 作业是否存在
	 */
	public boolean isJobExisted(final String jobName) {
		return coordinatorRegistryCenter.isExisted(JobNodePath.getJobNameFullPath(jobName));
	}

	/**
	 * 获取作业节点数据.
	 * 
	 * @param node 作业节点名称
	 * @return 作业节点数据值
	 */
	public String getJobNodeData(final String node) {
		return coordinatorRegistryCenter.get(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node));
	}

	/**
	 * 直接从注册中心而非本地缓存获取作业节点数据.
	 * 
	 * @param node 作业节点名称
	 * @return 作业节点数据值
	 */
	public String getJobNodeDataDirectly(final String node) {
		return coordinatorRegistryCenter.getDirectly(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node));
	}

	/**
	 * 直接从注册中心而非本地缓存获取作业节点数据.可用于相同namespace下的其他作业。
	 * 
	 * @param jobName 作业名
	 * @param node 作业节点名称
	 * @return 作业节点数据值
	 */
	public String getJobNodeDataDirectly(String jobName, final String node) {
		return coordinatorRegistryCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, node));
	}

	/**
	 * 获取作业节点子节点名称列表.
	 * 
	 * @param node 作业节点名称
	 * @return 作业节点子节点名称列表
	 */
	public List<String> getJobNodeChildrenKeys(final String node) {
		return coordinatorRegistryCenter
				.getChildrenKeys(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node));
	}

	/**
	 * 如果不存在则创建作业节点.
	 * 
	 * @param node 作业节点名称
	 */
	public void createJobNodeIfNeeded(final String node) {
		coordinatorRegistryCenter.persist(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node), "");
	}

	public JobConfiguration getJobConfiguration() {
		return jobConfiguration;
	}

	public void createOrUpdateJobNodeWithValue(final String node, final String value) {
		coordinatorRegistryCenter.persist(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node), value);
	}

	/**
	 * 如果节点存在，则删除作业节点
	 * 
	 * @param node 作业节点名称
	 */
	public void removeJobNodeIfExisted(final String node) {
		if (isJobNodeExisted(node)) {
			coordinatorRegistryCenter.remove(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node));
		}
	}

	/**
	 * 删除作业节点
	 *
	 * @param node 作业节点名称
	 */
	public void removeJobNode(final String node) {
		coordinatorRegistryCenter.remove(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node));
	}


	/**
	 * 如果节点不存在或允许覆盖则填充节点数据.
	 * 
	 * @param node 作业节点名称
	 * @param value 作业节点数据值
	 */
	public void fillJobNodeIfNullOrOverwrite(final String node, final Object value) {
		if (null == value) {
			log.info("[{}] msg=job node value is null, node:{}", jobName, node);
			return;
		}
		if (!isJobNodeExisted(node) || (!value.toString().equals(getJobNodeDataDirectly(node)))) {
			coordinatorRegistryCenter.persist(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node),
					value.toString());
		}
	}

	/**
	 * 填充临时节点数据.
	 * 
	 * @param node 作业节点名称
	 * @param value 作业节点数据值
	 */
	public void fillEphemeralJobNode(final String node, final Object value) {
		coordinatorRegistryCenter.persistEphemeral(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node),
				value.toString());
	}

	/**
	 * 更新节点数据.
	 * 
	 * @param node 作业节点名称
	 * @param value 作业节点数据值
	 */
	public void updateJobNode(final String node, final Object value) {
		coordinatorRegistryCenter.update(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node),
				value.toString());
	}

	/**
	 * 跟新作业节点数据。可用于同一个namespace下的其他作业。
	 * 
	 * @param jobName 作业名
	 * @param node 作业节点名称
	 * @param value 待替换的数据
	 */
	public void updateJobNode(final String jobName, final String node, final Object value) {
		coordinatorRegistryCenter.update(JobNodePath.getNodeFullPath(jobName, node), value.toString());
	}

	/**
	 * 替换作业节点数据.
	 * 
	 * @param node 作业节点名称
	 * @param value 待替换的数据
	 */
	public void replaceJobNode(final String node, final Object value) {
		coordinatorRegistryCenter.persist(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), node),
				value.toString());
	}

	/**
	 * 替换作业节点数据.
	 * 
	 * @param jobName 作业名
	 * @param node 作业节点名称
	 * @param value 待替换的数据
	 */
	public void replaceJobNode(final String jobName, final String node, final Object value) {
		coordinatorRegistryCenter.persist(JobNodePath.getNodeFullPath(jobName, node), value.toString());
	}

	/**
	 * 在事务中执行操作.
	 * 
	 * @param callback 执行操作的回调
	 */
	public void executeInTransaction(final TransactionExecutionCallback callback) {
		try {
			CuratorTransactionFinal curatorTransactionFinal = getClient().inTransaction().check().forPath("/").and();
			callback.execute(curatorTransactionFinal);
			curatorTransactionFinal.commit();
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			RegExceptionHandler.handleException(ex);
		}
	}

	/**
	 * 在主节点执行操作.
	 * 
	 * @param latchNode 分布式锁使用的作业节点名称
	 * @param callback 执行操作的回调
	 */
	public void executeInLeader(final String latchNode, final LeaderExecutionCallback callback) {
		try (LeaderLatch latch = new LeaderLatch(getClient(),
				JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), latchNode))) {
			latch.start();
			latch.await();
			callback.execute();
			// CHECKSTYLE:OFF
		} catch (final Exception e) {
			log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, e.getMessage()), e);
			// CHECKSTYLE:ON
			if (e instanceof InterruptedException) {// NOSONAR
				Thread.currentThread().interrupt();
			} else {
				throw new JobException(e);
			}
		}
	}

	public void executeInLeader(final String latchNode, final LeaderExecutionCallback callback, final long timeout,
			final TimeUnit unit, final LeaderExecutionCallback timeoutCallback) {
		try (LeaderLatch latch = new LeaderLatch(getClient(),
				JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), latchNode))) {
			latch.start();
			if (latch.await(timeout, unit)) {
				callback.execute();
			} else {
				if (timeoutCallback != null) {
					timeoutCallback.execute();
				}
			}
			// CHECKSTYLE:OFF
		} catch (final Exception e) {
			log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, e.getMessage()), e);
			// CHECKSTYLE:ON
			if (e instanceof InterruptedException) {// NOSONAR
				Thread.currentThread().interrupt();
			} else {
				throw new JobException(e);
			}
		}
	}

	public CuratorFramework getClient() {
		return (CuratorFramework) coordinatorRegistryCenter.getRawClient();
	}

	/**
	 * 获取当前运行execution分片列表
	 * @return 当前运行execution分片列表
	 */
	public List<String> getRunningItems() {
		return coordinatorRegistryCenter
				.getChildrenKeys(JobNodePath.getNodeFullPath(jobConfiguration.getJobName(), "execution"));
	}

	/**
	 * 删除ZK结点
	 */
	public void deleteJobNode() {
		ZookeeperConfiguration zkConfig = ((ZookeeperRegistryCenter) coordinatorRegistryCenter).getZkConfig();
		ZookeeperRegistryCenter newZk = new ZookeeperRegistryCenter(zkConfig);
		newZk.init();
		try {
			newZk.remove(ServerNode.getServerNode(jobName, executorName));
			for (int i = 0; i < MAX_DELETE_RETRY_TIMES; i++) {
				String fullPath = JobNodePath.getJobNameFullPath(jobConfiguration.getJobName());
				if (!newZk.isExisted(fullPath)) {
					return;
				}
				List<String> servers = newZk.getChildrenKeys(ServerNode.getServerRoot(jobName));
				if (servers == null || servers.isEmpty()) {
					if (tryToRemoveNode(newZk, fullPath)) {
						return;
					}
				}
				BlockUtils.waitingShortTime();
			}
		} finally {
			newZk.close();
		}
	}

	private boolean tryToRemoveNode(ZookeeperRegistryCenter newZk, String fullPath) {
		try {
			newZk.remove(fullPath);
			return true;
		} catch (Exception e) {
			log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, e.getMessage()), e);
		}
		return false;
	}

	public boolean isConnected() {
		return coordinatorRegistryCenter.isConnected();
	}
}
