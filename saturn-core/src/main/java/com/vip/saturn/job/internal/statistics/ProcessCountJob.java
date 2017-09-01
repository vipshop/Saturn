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

package com.vip.saturn.job.internal.statistics;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.analyse.AnalyseService;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.server.ServerService;

/**
 * 统计处理数据数量的作业.
 * 
 * 
 */
public class ProcessCountJob implements Runnable {

	private final JobConfiguration jobConfiguration;

	private final ServerService serverService;

	private final AnalyseService analyseService;

	public ProcessCountJob(final JobScheduler jobScheduler) {
		jobConfiguration = jobScheduler.getCurrentConf();
		serverService = jobScheduler.getServerService();
		analyseService = jobScheduler.getAnalyseService();
	}

	@Override
	public void run() {
		String jobName = jobConfiguration.getJobName();
		serverService.persistProcessSuccessCount(
				ProcessCountStatistics.getProcessSuccessCount(serverService.getExecutorName(), jobName));
		serverService.persistProcessFailureCount(
				ProcessCountStatistics.getProcessFailureCount(serverService.getExecutorName(), jobName));
		analyseService.persistTotalCount();
		analyseService.persistErrorCount();
	}
}
