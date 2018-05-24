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

package com.vip.saturn.job.internal.config;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.execution.ExecutionContextService;
import com.vip.saturn.job.internal.execution.ExecutionService;
import com.vip.saturn.job.internal.failover.FailoverService;
import com.vip.saturn.job.internal.listener.AbstractJobListener;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;
import com.vip.saturn.job.internal.storage.JobNodePath;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationListenerManager extends AbstractListenerManager {
	static Logger log = LoggerFactory.getLogger(ConfigurationListenerManager.class);

	private boolean isShutdown = false;

	private ExecutionContextService executionContextService;

	private ExecutionService executionService;

	private FailoverService failoverService;

	private ConfigurationService configurationService;

	public ConfigurationListenerManager(JobScheduler jobScheduler) {
		super(jobScheduler);
		jobConfiguration = jobScheduler.getCurrentConf();
		jobName = jobConfiguration.getJobName();
		executionContextService = jobScheduler.getExecutionContextService();
		executionService = jobScheduler.getExecutionService();
		failoverService = jobScheduler.getFailoverService();
		configurationService = jobScheduler.getConfigService();
	}

	@Override
	public void start() {
		zkCacheManager.addTreeCacheListener(new CronPathListener(),
				JobNodePath.getNodeFullPath(jobName, ConfigurationNode.CRON), 0);
		zkCacheManager.addTreeCacheListener(new EnabledPathListener(),
				JobNodePath.getNodeFullPath(jobName, ConfigurationNode.ENABLED), 0);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		isShutdown = true;
		zkCacheManager.closeTreeCache(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.CRON), 0);
		zkCacheManager.closeTreeCache(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.ENABLED), 0);
	}

	class EnabledPathListener extends AbstractJobListener {
		/**
		 *
		 * if it's a msgJob, job should go down and up as the enabled value changed from false to true.<br>
		 * therefor, we don't care about whether the channel/queue name is changed.
		 */
		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			if (isShutdown) {
				return;
			}
			if (ConfigurationNode.isEnabledPath(jobName, path) && Type.NODE_UPDATED == event.getType()) {
				Boolean isJobEnabled = Boolean.valueOf(new String(event.getData().getData()));
				log.info("[{}] msg={} 's enabled change to {}", jobName, jobName, isJobEnabled);
				jobConfiguration.reloadConfig();
				if (isJobEnabled) {
					if (!isJobNotNull()) {
						return;
					}

					if (jobScheduler.getReportService() != null) {
						jobScheduler.getReportService().clearInfoMap();
					}
					failoverService.removeFailoverInfo();
					jobScheduler.getJob().enableJob();
					configurationService.notifyJobEnabledIfNecessary();
				} else {
					if (!isJobNotNull()) {
						return;
					}

					jobScheduler.getJob().disableJob();
					failoverService.removeFailoverInfo(); // clear failover info when disable job.
					configurationService.notifyJobDisabled();
				}
			}
		}

		private boolean isJobNotNull() {
			return jobScheduler != null && jobScheduler.getJob() != null;
		}
	}

	/**
	 * Just for the updateJobCron api
	 */
	class CronPathListener extends AbstractJobListener {

		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			if (isShutdown) {
				return;
			}
			if (ConfigurationNode.isCronPath(jobName, path) && Type.NODE_UPDATED == event.getType()) {
				log.info("[{}] msg={} 's cron update", jobName, jobName);

				String cronFromZk = jobConfiguration.getCronFromZk();
				if (!jobScheduler.getPreviousConf().getCron().equals(cronFromZk)) {
					jobScheduler.getPreviousConf().setCron(cronFromZk);
					jobScheduler.rescheduleJob(cronFromZk);
					executionService.updateNextFireTime(executionContextService.getShardingItems());
				}
			}
		}
	}

}
