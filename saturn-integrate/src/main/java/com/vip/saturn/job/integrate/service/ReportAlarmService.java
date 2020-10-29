/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.integrate.service;

import com.vip.saturn.job.integrate.entity.AlarmInfo;
import com.vip.saturn.job.integrate.exception.ReportAlarmException;

import java.util.List;
import java.util.Map;

/**
 * Report alarm service. Recommend use async thread to report, when it's heavy.
 * @author hebelala
 */
public interface ReportAlarmService {

	/**
	 * The NamespaceShardingService execute allSharding error
	 *
	 * @param namespace The domain or namespace
	 * @param hostValue The NamespaceShardingService thread leader's hostValue
	 */
	void allShardingError(String namespace, String hostValue) throws ReportAlarmException;

	/**
	 * Dashboard refresh data, find the container instances is mismatch
	 *
	 * @param namespace The domain or namespace
	 * @param taskId The taskId of container source
	 * @param configInstances The instances configured
	 * @param runningInstances The running instances
	 */
	void dashboardContainerInstancesMismatch(String namespace, String taskId, int configInstances, int runningInstances)
			throws ReportAlarmException;

	/**
	 * Dashboard refresh data, find the abnormal job
	 *
	 * @param namespace The domain or namespace
	 * @param jobName The abnormal job's name
	 * @param timeZone The timeZone of job
	 * @param shouldFiredTime The time that job should be fired
	 */
	void dashboardAbnormalJob(String namespace, String jobName, String timeZone, long shouldFiredTime)
			throws ReportAlarmException;

	/**
	 * Dashboard refresh data, find the abnormal jobs
	 * @param namespace The domain or namespace
	 * @param jobList The job list
	 */
	void dashboardAbnormalBatchJobs(String namespace, List<Map<String, String>> jobList) throws ReportAlarmException;

	/**
	 * Dashboard refresh data, find that the job is timeout
	 *
	 * @param namespace The domain or namespace
	 * @param jobName The timeout job's name
	 * @param timeoutItems The timeout items of the job
	 * @param timeout4AlarmSeconds The timeout4AlarmSeconds of job configured
	 */
	void dashboardTimeout4AlarmJob(String namespace, String jobName, List<Integer> timeoutItems,
			int timeout4AlarmSeconds) throws ReportAlarmException;

	/**
	 * Dashboard refresh data, find that the job is long time disabled
	 *
	 * @param namespace The domain or namespace
	 * @param jobName The timeout job's name
	 * @param disableTime The time when job disabled
	 * @param disableTimeoutSeconds The disableTimeoutSeconds of job configured
	 */
	void dashboardLongTimeDisabledJob(String namespace, String jobName, long disableTime,
			int disableTimeoutSeconds) throws ReportAlarmException;

	/**
	 * Alarm for executor restart.
	 *
	 * @param namespace
	 * @param executorName
	 * @param restartTime
	 */
	void executorRestart(String namespace, String executorName, String restartTime) throws ReportAlarmException;

	/**
	 * Raise customized alarm by Saturn job.
	 *
	 * @param namespace The domain or namespace
	 * @param jobName The job name
	 * @param shardItem The shardItem number
	 * @param alarmInfo The alarm info.
	 * @throws ReportAlarmException
	 */
	void raise(String namespace, String jobName, String executorName, Integer shardItem, AlarmInfo alarmInfo)
			throws ReportAlarmException;
}
