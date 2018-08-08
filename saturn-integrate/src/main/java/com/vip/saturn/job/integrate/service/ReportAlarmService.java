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
