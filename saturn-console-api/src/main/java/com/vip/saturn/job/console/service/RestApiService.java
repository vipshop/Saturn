package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RestApiJobInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.integrate.entity.AlarmInfo;

import java.util.List;
import java.util.Map;

/**
 * @author hebelala
 */
public interface RestApiService {

	/**
	 * Create a new job.
	 *
	 * @param jobConfig construct from the request.
	 * @throws SaturnJobConsoleException for below scenarios: <ul> <li>namespace or jobName is not found (statusCode =
	 * 404)</li> <li>Job with the same name is already created. (statusCode = 400）</li> <li>Other exceptions (statusCode
	 * = 500)</li> </ul>
	 */
	void createJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException;

	/**
	 * Get the job info by namespace and jobName pair.
	 *
	 * @throws SaturnJobConsoleException for below scenarios: <ul> <li>namespace or jobName is not found (statusCode =
	 * 404)</li> <li>Other exceptions (statusCode = 500)</li> </ul>
	 */
	RestApiJobInfo getRestAPIJobInfo(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * Get the jobs info under the namespace
	 */
	List<RestApiJobInfo> getRestApiJobInfos(String namespace) throws SaturnJobConsoleException;

	/**
	 * Enable the job if the job is disable.
	 *
	 * Nothing will return once the the job is enable successfully;
	 *
	 * @throws SaturnJobConsoleException for below scenarios: <ul> <li>The job was already enabled (statusCode =
	 * 201)</li> <li>The update interval time cannot less than 3 seconds (statusCode = 403)</li> <li>Enable the job
	 * after creation within 10 seconds (statusCode = 403)</li> <li>Other exceptions (statusCode = 500)</li> </ul>
	 */
	void enableJob(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * Disable the job if the job is enable.<br>
	 *
	 * Nothing will return when disable successfully;
	 *
	 * @throws SaturnJobConsoleException for below scenarios: <ul> <li>The job was already disabled (statusCode =
	 * 201)</li> <li>The update interval time cannot less than 3 seconds (statusCode = 403)</li> <li>Other exceptions
	 * (statusCode = 500)</li> </ul>
	 */
	void disableJob(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * udpdate the cron expression config.<br>
	 *
	 * @throws SaturnJobConsoleException for below scenarios: <ul> <li>The update interval time cannot less than 3
	 * seconds (statusCode = 403)</li> <li>Other exceptions (statusCode = 500)</li> </ul>
	 */
	void updateJobCron(String namespace, String jobName, String cron, Map<String, String> params)
			throws SaturnJobConsoleException;

	/**
	 * Run the job immediately.
	 */
	void runJobAtOnce(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * Stop the job immediately. The job status will change to STOPPING.
	 */
	void stopJobAtOnce(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * Delete the job.
	 */
	void deleteJob(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * Raise alarm for specified job/shard exception.
	 *
	 * @throws SaturnJobConsoleException for below scenarios: <ul> <li>namespace or jobName is not found (statusCode =
	 * 404)</li> <li>Job with the same name is already created. (statusCode = 400)</li> </ul>
	 */
	void raiseAlarm(String namespace, String jobName, String executorName, Integer shardItem, AlarmInfo alarmInfo)
			throws SaturnJobConsoleException;

	/**
	 * Raise alarm for executor restart event.
	 */
	void raiseExecutorRestartAlarm(String namespace, String executorName, AlarmInfo alarmInfo)
			throws SaturnJobConsoleException;

	/**
	 * update the job
	 */
	void updateJob(String namespace, String jobName, JobConfig jobConfig) throws SaturnJobConsoleException;
}
