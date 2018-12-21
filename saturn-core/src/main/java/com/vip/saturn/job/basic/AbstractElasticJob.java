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

import com.vip.saturn.job.exception.JobException;
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
import com.vip.saturn.job.trigger.Trigger;
import com.vip.saturn.job.trigger.Triggered;
import com.vip.saturn.job.utils.LogUtils;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.curator.framework.CuratorFramework;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
public abstract class AbstractElasticJob implements Stoppable {
	private static Logger log = LoggerFactory.getLogger(AbstractElasticJob.class);

	private volatile boolean stopped = false;
	private volatile boolean forceStopped = false;
	private volatile boolean aborted = false;
	private volatile boolean running = false;

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
	 * 重置作业调用一次的生命周期内的变量
	 */
	private void reset() {
		stopped = false;
		forceStopped = false;
		aborted = false;
		running = true;
	}

	@Override
	public void shutdown() {
		if (scheduler != null) {
			// 关闭调度器
			scheduler.shutdown();
			// 关闭执行业务的线程池，使得不能再提交新的业务任务
			jobScheduler.shutdownExecutorService();
			// 检查调度器任务是否完成，如果没有，中止业务
			if (!scheduler.isTerminated()) {
				if (configService.getJobType().isShell() && !configService.isJobEnabled()) {
					// 如果Shell作业业务进程有子进程，我们可能不能完全中止其子进程。
					// 所以，对于禁用的Shell作业，不中止业务。因为，作业处于禁用状态，说明是人为介入的可控状态。
					// 另外，在该Executor再次启动该作业前，会检查该作业是否正在运行，如果正在运行并且仍然处于禁用状态，则会相应的持久化相关状态到zk，防止重入；如果正在运行并且已经处于启用状态，则会中止其进程。
					LogUtils.warn(log, jobName, "the job is the disabled shell job, will not be aborted");
				} else {
					abort();
					scheduler.awaitTermination(500L);
				}
			}
		}
	}

	public ExecutorService getExecutorService() {
		return jobScheduler.getExecutorService();
	}

	protected void init() {
		Class<? extends Trigger> triggerClass = configService.getJobType().getTriggerClass();
		Trigger trigger = null;
		try {
			trigger = triggerClass.newInstance();
			trigger.init(this);
		} catch (Exception e) {
			LogUtils.error(log, jobName, "Trigger init failed", e);
			throw new JobException(e);
		}
		scheduler = new SaturnScheduler(this, trigger);
		scheduler.start();
		getExecutorService();
	}

	public final void execute(final Triggered triggered) {
		LogUtils.debug(log, jobName, "Saturn start to execute job [{}]", jobName);
		// 对每一个jobScheduler，作业对象只有一份，多次使用，所以每次开始执行前先要reset
		reset();

		if (configService == null) {
			LogUtils.warn(log, jobName, "configService is null");
			return;
		}

		JobExecutionMultipleShardingContext shardingContext = null;
		try {
			if (!configService.isEnabledReport() || failoverService.getLocalHostFailoverItems().isEmpty()) {
				shardingService.shardingIfNecessary();
			}

			if (!configService.isJobEnabled()) {
				LogUtils.debug(log, jobName, "{} is disabled, cannot be continued, do nothing about business.",
						jobName);
				return;
			}

			shardingContext = executionContextService.getJobExecutionShardingContext(triggered);
			if (shardingContext.getShardingItems() == null || shardingContext.getShardingItems().isEmpty()) {
				LogUtils.debug(log, jobName, "{} 's items of the executor is empty, do nothing about business.",
						jobName);
				callbackWhenShardingItemIsEmpty(shardingContext);
				return;
			}

			if (configService.isInPausePeriod()) {
				LogUtils.info(log, jobName,
						"the job {} current running time is in pausePeriod, do nothing about business.", jobName);
				return;
			}

			executeJobInternal(shardingContext);

			if (isFailoverSupported() && configService.isFailover() && !stopped && !forceStopped && !aborted) {
				failoverService.failoverIfNecessary();
			}

			LogUtils.debug(log, jobName, "Saturn finish to execute job [{}], sharding context:{}.", jobName,
					shardingContext);
		} catch (Exception e) {
			LogUtils.warn(log, jobName, e.getMessage(), e);
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
			runDownStream(shardingContext);
		}
	}

	private void runDownStream(final JobExecutionMultipleShardingContext shardingContext) {
		if (configService.isLocalMode()) {
			return;
		}
		JobType jobType = configService.getJobType();
		if (!(jobType.isCron() || jobType.isPassive())) {
			return;
		}
		if (shardingContext.getShardingTotalCount() != 1) {
			return;
		}
		List<String> downStream = configService.getDownStream();
		if (downStream.isEmpty()) {
			return;
		}
		if (!mayRunDownStream(shardingContext)) {
			return;
		}
		String downStreamDataStr = scheduler.getTrigger().serializeDownStreamData(shardingContext.getTriggered());
		String logMessagePrefix = "call runDownStream api";
		int size = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.size();
		for (int i = 0; i < size; i++) {
			String consoleUri = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.get(i);
			String targetUrl = consoleUri + "/rest/v1/" + namespace + "/jobs/" + jobName + "/runDownStream";
			LogUtils.info(log, jobName, "{}, target url is {}", logMessagePrefix, targetUrl);
			CloseableHttpClient httpClient = null;
			try {
				httpClient = HttpClientBuilder.create().build();
				HttpPost request = new HttpPost(targetUrl);
				final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000)
						.setSocketTimeout(10000).build();
				request.setConfig(requestConfig);
				request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
				request.setEntity(new StringEntity(downStreamDataStr));
				CloseableHttpResponse httpResponse = httpClient.execute(request);
				StatusLine statusLine = httpResponse.getStatusLine();
				if (statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_OK) {
					HttpEntity entity = httpResponse.getEntity();
					String result = entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
					LogUtils.info(log, jobName, "{}, result is {}", logMessagePrefix, result);
					return;
				} else {
					LogUtils.info(log, jobName, "{} failed, StatusLine is {}", logMessagePrefix, statusLine);
				}
			} catch (Exception e) {
				LogUtils.error(log, jobName, "{} error", logMessagePrefix, e);
			} finally {
				HttpClientUtils.closeQuietly(httpClient);
			}
		}
	}

	protected boolean mayRunDownStream(final JobExecutionMultipleShardingContext shardingContext) {
		return true;
	}

	/**
	 * 如果不存在该分片的running节点，又不是关闭了enabledReport的话，不继续执行；如果所有该executor分片running节点属于当前zk，继续执行；
	 * @param item 分片信息
	 * @return 是否继续执行完complete节点，清空failover信息
	 */
	private boolean checkIfZkLostAfterExecution(final Integer item) {
		CuratorFramework curatorFramework = (CuratorFramework) executionService.getCoordinatorRegistryCenter()
				.getRawClient();
		try {
			String runningPath = JobNodePath.getNodeFullPath(jobName, ExecutionNode.getRunningNode(item));
			Stat itemStat = curatorFramework.checkExists().forPath(runningPath);
			long sessionId = curatorFramework.getZookeeperClient().getZooKeeper().getSessionId();
			// 有itemStat的情况
			if (itemStat != null) {
				long ephemeralOwner = itemStat.getEphemeralOwner();
				if (ephemeralOwner != sessionId) {
					LogUtils.info(log, jobName,
							"item={} 's running node doesn't belong to current zk, node sessionid is {}, current zk "
									+ "sessionid is {}", item, ephemeralOwner, sessionId);
					return false;
				} else {
					return true;
				}
			}
			// 如果itemStat是空，要么是已经failover完了，要么是没有节点failover；两种情况都返回false
			LogUtils.info(log, jobName, "item={} 's running node is not exists, zk sessionid={} ", item, sessionId);

			return false;
		} catch (Throwable e) {
			LogUtils.error(log, jobName, e.getMessage(), e);
			return false;
		}
	}

	protected abstract void executeJob(final JobExecutionMultipleShardingContext shardingContext);

	public void callbackWhenShardingItemIsEmpty(final JobExecutionMultipleShardingContext shardingContext) {
	}

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

	public void enableJob() {
		scheduler.getTrigger().enableJob();
	}

	public void disableJob() {
		scheduler.getTrigger().disableJob();
	}

	public void onResharding() {
		scheduler.getTrigger().onResharding();
	}

	public boolean isFailoverSupported() {
		return scheduler.getTrigger().isFailoverSupported();
	}

	public abstract void onForceStop(int item);

	public abstract void onTimeout(int item);

	public abstract void onNeedRaiseAlarm(int item, String alarmMessage);

	public void notifyJobEnabled() {
	}

	public void notifyJobDisabled() {
	}

	public boolean isStopped() {
		return stopped;
	}

	public boolean isForceStopped() {
		return forceStopped;
	}

	public boolean isAborted() {
		return aborted;
	}

	public boolean isRunning() {
		return running;
	}

	public ConfigurationService getConfigService() {
		return configService;
	}

	public void setConfigService(ConfigurationService configService) {
		this.configService = configService;
	}

	public ShardingService getShardingService() {
		return shardingService;
	}

	public void setShardingService(ShardingService shardingService) {
		this.shardingService = shardingService;
	}

	public ExecutionContextService getExecutionContextService() {
		return executionContextService;
	}

	public void setExecutionContextService(ExecutionContextService executionContextService) {
		this.executionContextService = executionContextService;
	}

	public ExecutionService getExecutionService() {
		return executionService;
	}

	public void setExecutionService(ExecutionService executionService) {
		this.executionService = executionService;
	}

	public FailoverService getFailoverService() {
		return failoverService;
	}

	public void setFailoverService(FailoverService failoverService) {
		this.failoverService = failoverService;
	}

	public ServerService getServerService() {
		return serverService;
	}

	public void setServerService(ServerService serverService) {
		this.serverService = serverService;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public SaturnScheduler getScheduler() {
		return scheduler;
	}

	public void setScheduler(SaturnScheduler scheduler) {
		this.scheduler = scheduler;
	}

	public JobScheduler getJobScheduler() {
		return jobScheduler;
	}

	public void setJobScheduler(JobScheduler jobScheduler) {
		this.jobScheduler = jobScheduler;
	}

	public SaturnExecutorService getSaturnExecutorService() {
		return saturnExecutorService;
	}

	public void setSaturnExecutorService(SaturnExecutorService saturnExecutorService) {
		this.saturnExecutorService = saturnExecutorService;
	}

	public ReportService getReportService() {
		return reportService;
	}

	public void setReportService(ReportService reportService) {
		this.reportService = reportService;
	}

	public String getJobVersion() {
		return jobVersion;
	}

	public void setJobVersion(String jobVersion) {
		this.jobVersion = jobVersion;
	}

}
