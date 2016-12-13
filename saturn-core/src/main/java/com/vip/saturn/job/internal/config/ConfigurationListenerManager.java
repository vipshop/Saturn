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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.execution.ExecutionContextService;
import com.vip.saturn.job.internal.execution.ExecutionService;
import com.vip.saturn.job.internal.listener.AbstractJobListener;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;

public class ConfigurationListenerManager extends AbstractListenerManager {
	static Logger log = LoggerFactory.getLogger(ConfigurationListenerManager.class);

	private boolean isShutdown = false;

	private ExecutionContextService executionContextService;

	private ExecutionService executionService;

	public ConfigurationListenerManager(JobScheduler jobScheduler) {
		super(jobScheduler);
		jobConfiguration = jobScheduler.getCurrentConf();
        jobName = jobConfiguration.getJobName();
        executionContextService = jobScheduler.getExecutionContextService();
        executionService = jobScheduler.getExecutionService();
	}

	@Override
	public void start() {
		addDataListener(new CronPathListener(), jobName);
		addDataListener(new EnabledPathListener(), jobName);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		isShutdown = true;
	}

	class EnabledPathListener extends AbstractJobListener {
		/**
		 * 
		 * if it's a msgJob, job should go down and up as the enabled value changed from false to true.<br>
		 * therefor, we don't care about whether the channel/queue name is changed.
		 */
		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			if(isShutdown) return;
			if (ConfigurationNode.isEnabledPath(jobName, path) && Type.NODE_UPDATED == event.getType()) {
				Boolean isJobEnabled = Boolean.valueOf(new String(event.getData().getData()));
				log.info("[{}] msg={} 's enabled change to {}", jobName, jobName, isJobEnabled);
				jobConfiguration.reloadConfig();
				if (isJobEnabled) {
					if(jobScheduler != null && jobScheduler.getJob() != null){
						jobScheduler.getJob().enableJob();
					}					
				} else {
					if(jobScheduler != null && jobScheduler.getJob() != null){
						jobScheduler.getJob().disableJob();
					}
				}
			}
		}

	}

	class CronPathListener extends AbstractJobListener {

		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			if(isShutdown) return;
			if (ConfigurationNode.isCronPath(jobName, path) && Type.NODE_UPDATED == event.getType()) {
				log.info("[{}] msg={} 's cron update", jobName, jobName);

				String cronFromZk = jobConfiguration.getCronFromZk();
				if (!jobScheduler.getPreviousConf().getCron().equals(cronFromZk)) {
					jobScheduler.getPreviousConf().setCron(cronFromZk);
					jobScheduler.rescheduleJob(cronFromZk);
					executionService
							.updateNextFireTimeAndPausePeriodEffected(executionContextService.getShardingItems());
				}
			}
		}
	}

}
