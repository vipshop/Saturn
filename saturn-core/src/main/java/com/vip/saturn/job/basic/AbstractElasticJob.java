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

package com.vip.saturn.job.basic;

import com.vip.saturn.job.executor.SaturnExecutorService;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.internal.control.ReportService;
import com.vip.saturn.job.internal.execution.ExecutionContextService;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.execution.ExecutionService;
import com.vip.saturn.job.internal.failover.FailoverService;
import com.vip.saturn.job.internal.server.ServerService;
import com.vip.saturn.job.internal.sharding.ShardingService;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.trigger.SaturnScheduler;
import com.vip.saturn.job.trigger.SaturnTrigger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 弹性化分布式作业的基类.
 * @author dylan.xue
 */
public abstract class AbstractElasticJob implements Stopable {
	private static Logger log = LoggerFactory.getLogger(AbstractElasticJob.class);

	private boolean stopped = false;

	private boolean forceStopped = false;

	private boolean aborted = false;

	protected ConfigurationService configService;

	protected ShardingService shardingService;

	protected ExecutionContextService executionContextService;

	protected ExecutionService executionService;

	protected FailoverService failoverService;

	protected ServerService serverService;

	protected String executorName;

	protected String jobName;

	protected String namespace;

	protected SaturnScheduler scheduler;

	protected JobScheduler jobScheduler;

	protected SaturnExecutorService saturnExecutorService;

	protected ReportService reportService;

	protected String jobVersion;

	/**
	 * vms job这个状态无效。
	 */
	protected boolean running;

	private void reset() {
		stopped = false;
		forceStopped = false;
		aborted = false;
		running = true;
	}

	@Override
	public void shutdown() {
		if (scheduler != null) {
			scheduler.shutdown();
		}
	}

	public ExecutorService getExecutorService() {
		return jobScheduler.getExecutorService();
	}

	protected void init() {
		scheduler = getTrigger().build(this);
		getExecutorService();
	}

	public final void execute() {
		log.trace("Saturn start to execute job [{}].", jobName);
		// 对每一个jobScheduler，作业对象只有一份，多次使用，所以每次开始执行前先要reset
		reset();

		if (configService == null) {
			log.warn("configService is null");
			return;
		}

		JobExecutionMultipleShardingContext shardingContext = null;
		try {
			if (!configService.isEnabledReport() || failoverService.getLocalHostFailoverItems().isEmpty()) {
				shardingService.shardingIfNecessary();
			}

			if (!configService.isJobEnabled()) {
				if (log.isDebugEnabled()) {
					log.debug("{} is disabled, cannot be continued, do nothing about business.", jobName);
				}
				return;
			}

			shardingContext = executionContextService.getJobExecutionShardingContext();
			if (shardingContext.getShardingItems() == null || shardingContext.getShardingItems().isEmpty()) {
				if (log.isDebugEnabled()) {
					log.debug("{} 's items of the executor is empty, do nothing about business.", jobName);
				}
				callbackWhenShardingItemIsEmpty(shardingContext);
				return;
			}

			if (configService.isInPausePeriod()) {
				log.info("the job {} current running time is in pausePeriod, do nothing about business.", jobName);
				return;
			}

			executeJobInternal(shardingContext);

			if (isFailoverSupported() && configService.isFailover() && !stopped && !forceStopped && !aborted) {
				failoverService.failoverIfNecessary();
			}

			log.trace("Saturn finish to execute job [{}], sharding context:{}.", jobName, shardingContext);
		} catch (Exception e) {
			log.warn(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, e.getMessage()), e);
		} finally {
			running = false;
		}
	}

	private void executeJobInternal(final JobExecutionMultipleShardingContext shardingContext) throws Exception {

		executionService.registerJobBegin(shardingContext);

		try {
			executeJob(shardingContext);
		} finally {
			List<Integer> shardingItems = shardingContext.getShardingItems();
			if (!shardingItems.isEmpty()) {
				Date nextFireTimePausePeriodEffected = jobScheduler.getNextFireTimePausePeriodEffected();
				boolean isEnabledReport = configService.isEnabledReport();
				for (int item : shardingItems) {
					if (isEnabledReport && !checkIfZkLostAfterExecution(item)) {
						continue;
					}
					if (!aborted) {
						executionService
								.registerJobCompletedByItem(shardingContext, item, nextFireTimePausePeriodEffected);
					}
					if (isFailoverSupported() && configService.isFailover()) {
						failoverService.updateFailoverComplete(item);
					}
				}
			}
			afterMainThreadDone(shardingContext);
		}
	}

	/**
	 * 如果不存在该分片的running节点，又不是关闭了enabledReport的话，不继续执行；如果所有该executor分片running节点属于当前zk，继续执行；
	 * @param item 分片信息
	 * @return 是否继续执行完complete节点，清空failover信息
	 */
	private boolean checkIfZkLostAfterExecution(final Integer item) {
		CuratorFramework curatorFramework = (CuratorFramework) executionService.getCoordinatorRegistryCenter().getRawClient();
		try {
			String runningPath = JobNodePath.getNodeFullPath(jobName, ExecutionNode.getRunningNode(item));
			Stat itemStat = curatorFramework.checkExists().forPath(runningPath);
			long sessionId = curatorFramework.getZookeeperClient().getZooKeeper().getSessionId();
			// 有itemStat的情况
			if (itemStat != null) {
				long ephemeralOwner = itemStat.getEphemeralOwner();
				if (ephemeralOwner != sessionId) {
					log.info("[{}] msg=item={} 's running node doesn't belong to current zk, node sessionid is {}, current zk sessionid is {}",
							jobName, item, ephemeralOwner, sessionId);
					return false;
				} else {
					return true;
				}
			}
			// 如果itemStat是空，要么是已经failover完了，要么是没有节点failover；两种情况都返回false
			log.info("[{}] msg=item={} 's running node is not exists, zk sessionid={} ", jobName, item, sessionId);
			return false;
		} catch (Throwable e) {
			log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, e.getMessage()), e);
			return false;
		}
	}

	protected abstract void executeJob(final JobExecutionMultipleShardingContext shardingContext);

	/**
	 * 当涉及到主线程开子线程异步执行时，在主线程完成后提供的回调
	 */
	public void afterMainThreadDone(final JobExecutionMultipleShardingContext shardingContext) {
	}

	public void callbackWhenShardingItemIsEmpty(final JobExecutionMultipleShardingContext shardingContext) {
	}

	public abstract boolean isFailoverSupported();

	@Override
	public void stop() {
		stopped = true;
	}

	@Override
	public void forceStop() {
		forceStopped = true;
	}

	@Override
	public void abort() {
		aborted = true;
	}

	@Override
	public void resume() {
		stopped = false;
	}

	public abstract SaturnTrigger getTrigger();

	public abstract void enableJob();

	public abstract void disableJob();

	public abstract void onResharding();

	public abstract void onForceStop(int item);

	public abstract void onTimeout(int item);

	public abstract void onNeedRaiseAlarm(int item, String alarmMessage);

	public void notifyJobEnabled() {
	}

	public void notifyJobDisabled() {
	}

	/**
	 * 设置shardingService
	 * @param shardingService
	 */
	protected void setShardingService(ShardingService shardingService) {
		this.shardingService = shardingService;
	}

	protected void setExecutionContextService(ExecutionContextService executionContextService) {
		this.executionContextService = executionContextService;
	}

	protected void setExecutionService(ExecutionService executionService) {
		this.executionService = executionService;
	}

	protected void setFailoverService(FailoverService failoverService) {
		this.failoverService = failoverService;
	}

	protected void setServerService(ServerService serverService) {
		this.serverService = serverService;
	}

	protected void setReportService(ReportService reportService) {
		this.reportService = reportService;
	}

	/**
	 * 获取executorName
	 * @return
	 */
	public String getExecutorName() {
		return executorName;
	}

	protected void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	/**
	 * 获取jobName
	 * @return
	 */
	public String getJobName() {
		return jobName;
	}

	protected void setJobName(String jobName) {
		this.jobName = jobName;
	}

	/**
	 * 获取namespace
	 * @return
	 */
	public String getNamespace() {
		return namespace;
	}

	protected void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	protected SaturnScheduler getScheduler() {
		return scheduler;
	}

	protected void setScheduler(SaturnScheduler scheduler) {
		this.scheduler = scheduler;
	}

	protected JobScheduler getJobScheduler() {
		return jobScheduler;
	}

	public void setJobScheduler(JobScheduler jobScheduler) {
		this.jobScheduler = jobScheduler;
	}

	/**
	 * 获取 saturnExecutorService
	 * @return
	 */
	public SaturnExecutorService getSaturnExecutorService() {
		return saturnExecutorService;
	}

	protected void setSaturnExecutorService(SaturnExecutorService saturnExecutorService) {
		this.saturnExecutorService = saturnExecutorService;
	}

	/**
	 * 作业是否running
	 * @return
	 */
	public boolean isRunning() {
		return running;
	}

	protected void setRunning(boolean running) {
		this.running = running;
	}

	/**
	 * 获取configService
	 * @return
	 */
	public ConfigurationService getConfigService() {
		return configService;
	}

	public final void setConfigService(final ConfigurationService configService) {
		this.configService = configService;
	}

	public String getJobVersion() {
		return jobVersion;
	}

	public void setJobVersion(String jobVersion) {
		this.jobVersion = jobVersion;
	}

}
