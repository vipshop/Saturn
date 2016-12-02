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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.executor.SaturnExecutorService;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.internal.execution.ExecutionContextService;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.execution.ExecutionService;
import com.vip.saturn.job.internal.failover.FailoverService;
import com.vip.saturn.job.internal.offset.OffsetService;
import com.vip.saturn.job.internal.server.ServerService;
import com.vip.saturn.job.internal.server.ServerStatus;
import com.vip.saturn.job.internal.sharding.ShardingService;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.threads.ExtendableThreadPoolExecutor;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.threads.TaskQueue;
import com.vip.saturn.job.trigger.SaturnScheduler;
import com.vip.saturn.job.trigger.SaturnTrigger;

/**
 * 弹性化分布式作业的基类.
 * @author dylan.xue
 */
public abstract class AbstractElasticJob implements Stopable {
	private static Logger log = LoggerFactory.getLogger(AbstractElasticJob.class);

	private ExecutorService executorService;

	private boolean stopped = false;

	private boolean forceStopped = false;

	private boolean aborted = false;

	protected ConfigurationService configService;

	protected ShardingService shardingService;

	protected ExecutionContextService executionContextService;

	protected ExecutionService executionService;

	protected FailoverService failoverService;

	protected OffsetService offsetService;

	protected ServerService serverService;

	protected String executorName;

	protected String jobName;

	protected String namespace;

	protected SaturnScheduler scheduler;

	protected JobScheduler jobScheduler;

	protected SaturnExecutorService saturnExecutorService;
	
	private ExecutorService zkExecutionService;
	
	private int runCount = 0;
	
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
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown();
		}
			
		if(zkExecutionService != null && !zkExecutionService.isShutdown()){
			zkExecutionService.shutdown();
		}
	}

	protected ExecutorService getExecutorService() {
		if (executorService != null) {
			return executorService;
		}
		ThreadFactory factory = new SaturnThreadFactory(jobName);
		executorService = new ExtendableThreadPoolExecutor(0, 100, 2, TimeUnit.MINUTES, new TaskQueue(), factory);
		return executorService;
	}

	protected void init() throws SchedulerException {
		scheduler = getTrigger().build(this);
		getExecutorService();
		zkExecutionService = Executors.newSingleThreadExecutor();
	}

	public final void execute() {
		log.trace("Quartz run-job entrance, job execution context:{}.");
		// 对每一个jobScheduler，作业对象只有一份，多次使用，所以每次开始执行前先要reset
		reset();

		if (configService == null) {
			log.warn("configService is null");
			return;
		}

		JobExecutionMultipleShardingContext shardingContext = null;
		try {
			if (failoverService.getLocalHostFailoverItems().isEmpty()) {
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
				executionService
						.updateNextFireTimeAndPausePeriodEffectedIfNecessary(shardingContext.getShardingItems());
				return;
			}
			executeJobInternal(shardingContext);

			if (isFailoverSupported() && configService.isFailover() && !stopped && !forceStopped && !aborted) {// NOSONAR
				failoverService.failoverIfNecessary();
			}

			log.trace("Elastic job: execute normal completed, sharding context:{}.", shardingContext);
		} catch (Exception e) {
			log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, e.getMessage()), e);
		} finally {
			running = false;
		}
	}

	private void executeJobInternal(final JobExecutionMultipleShardingContext shardingContext)
			throws JobExecutionException {
		final int version = ++runCount;
		zkExecutionService.submit(new Runnable(){
			@Override
			public void run() {
				if(version < runCount){
					return;
				}
				if (shouldUploadRunningData()) {
					executionService.registerJobBegin(shardingContext);
				}
			}
		});

		try {
			executeJob(shardingContext);
		} finally {
			zkExecutionService.submit(new Runnable(){
				
				@Override
				public void run() {
					if(version < runCount){
						return;
					}
					
					boolean updateServerStatus = false;
					for (int item : shardingContext.getShardingItems()) {
						if (!continueAfterExecution(item)) {
							continue;// NOSONAR
						}
						if (shouldUploadRunningData() && !aborted) {
							if (!updateServerStatus) {
								serverService.updateServerStatus(ServerStatus.READY);// server状态只需更新一次
								updateServerStatus = true;
							}
							executionService.registerJobCompleted(shardingContext, item);
						}
						if (isFailoverSupported() && configService.isFailover()) {
							failoverService.updateFailoverComplete(item);
						}
					}
				}
			});
			afterMainThreadDone(shardingContext);
		}
	}

	/**
	 * 如果不存在该分片的running节点，不继续执行；如果所有该executor分片running节点属于当前zk，继续执行；
	 * @param item 分片信息
	 * @return 是否继续执行完complete节点，清空failover信息
	 */
	private boolean continueAfterExecution(final Integer item) {
		// 如果zk disconnected, 直接返回false；即不属于当前zk创建的
		if (!((CuratorFramework) executionService.getCoordinatorRegistryCenter().getRawClient()).getZookeeperClient()
				.isConnected()) {
			return false;
		}
		try {
			long sessionId = ((CuratorFramework) executionService.getCoordinatorRegistryCenter().getRawClient())
					.getZookeeperClient().getZooKeeper().getSessionId();
			String runningPath = JobNodePath.getNodeFullPath(jobName, ExecutionNode.getRunningNode(item));
			Stat itemStat = ((CuratorFramework) executionService.getCoordinatorRegistryCenter().getRawClient())
					.checkExists().forPath(runningPath);

			if (itemStat != null) {
				if (itemStat.getEphemeralOwner() != sessionId) {
					log.info("[{}] msg=item={} 's running node doesn't belong to current zk, zk sessionid={} ", jobName,
							itemStat.getEphemeralOwner(), sessionId);
					return false;
				}
			} else {
				// 如果itemStat是空，要么是已经failover完了，要么是没有节点failover；两种情况都返回false;
				log.info("[{}] msg=item={} 's running node is not exists, zk sessionid={} ", jobName, item, sessionId);
				return false;
			}
			return true;
		} catch (Exception e) {
			log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, e.getMessage()), e);
			return false;
		}
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

	protected void setOffsetService(OffsetService offsetService) {
		this.offsetService = offsetService;
	}

	protected void setServerService(ServerService serverService) {
		this.serverService = serverService;
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

	protected abstract void executeJob(final JobExecutionMultipleShardingContext shardingContext);

	/**
	 * 当涉及到主线程开子线程异步执行时，在主线程完成后提供的回调
	 */
	public void afterMainThreadDone(final JobExecutionMultipleShardingContext shardingContext) {
	}

	public void callbackWhenShardingItemIsEmpty(final JobExecutionMultipleShardingContext shardingContext) {
	}

	public abstract boolean shouldUploadRunningData();

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

	public final void setConfigService(final ConfigurationService configService) {
		this.configService = configService;
	}

	public abstract SaturnTrigger getTrigger();

	public abstract void enableJob();

	public abstract void disableJob();

	public abstract void onResharding();

	public abstract void onForceStop(int item);

	public abstract void onTimeout(int item);

}
