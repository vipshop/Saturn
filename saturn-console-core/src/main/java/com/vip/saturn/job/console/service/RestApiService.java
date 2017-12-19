package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.NamespaceDomainInfo;
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
	 *
	 * @throws SaturnJobConsoleException for below scenarios:
	 * <ul>
	 * <li>namespace or jobName is not found (statusCode = 404)</li>
	 * <li>Job with the same name is already created. (statusCode = 400）</li>
	 * <li>Other exceptions (statusCode = 500)</li>
	 * </ul>
	 */
	void createJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException;

	/**
	 *
	 * Get the job info by namespace and jobName pair.
	 *
	 * @param namespace
	 * @param jobName
	 * @return
	 * @throws SaturnJobConsoleException for below scenarios:
	 * <ul>
	 * <li>namespace or jobName is not found (statusCode = 404)</li>
	 * <li>Other exceptions (statusCode = 500)</li>
	 * </ul>
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
	 * @throws SaturnJobConsoleException for below scenarios:
	 * <ul>
	 * <li>The job was already enabled (statusCode = 201)</li>
	 * <li>The update interval time cannot less than 3 seconds (statusCode = 403)</li>
	 * <li>Enable the job after creation within 10 seconds (statusCode = 403)</li>
	 * <li>Other exceptions (statusCode = 500)</li>
	 * </ul>
	 */
	void enableJob(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * Disable the job if the job is enable.<br>
	 *
	 * Nothing will return when disable successfully;
	 *
	 * @throws SaturnJobConsoleException for below scenarios:
	 * <ul>
	 * <li>The job was already disabled (statusCode = 201)</li>
	 * <li>The update interval time cannot less than 3 seconds (statusCode = 403)</li>
	 * <li>Other exceptions (statusCode = 500)</li>
	 * </ul>
	 */
	void disableJob(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * udpdate the cron expression config.<br>
	 *
	 * @throws SaturnJobConsoleException for below scenarios:
	 * <ul>
	 * <li>The update interval time cannot less than 3 seconds (statusCode = 403)</li>
	 * <li>Other exceptions (statusCode = 500)</li>
	 * </ul>
	 */
	void updateJobCron(String namespace, String jobName, String cron, Map<String, String> params)
			throws SaturnJobConsoleException;

	/**
	 * Run the job immediately.
	 *
	 * @param namespace
	 * @param jobName
	 * @throws SaturnJobConsoleException
	 */
	void runJobAtOnce(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * Stop the job immediately. The job status will change to STOPPING.
	 *
	 * @param namespace
	 * @param jobName
	 * @throws SaturnJobConsoleException
	 */
	void stopJobAtOnce(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * Delete the job.
	 *
	 * @param namespace
	 * @param jobName
	 * @throws SaturnJobConsoleException
	 */
	void deleteJob(String namespace, String jobName) throws SaturnJobConsoleException;


	/**
	 * Create Namespace.
	 *
	 * @param namespaceDomainInfo
	 * @throws SaturnJobConsoleException
	 */
	void createNamespace(NamespaceDomainInfo namespaceDomainInfo) throws SaturnJobConsoleException;

	/**
	 * Update Namespace.
	 *
	 * @param namespaceDomainInfo
	 * @throws SaturnJobConsoleException
	 */
	void updateNamespace(NamespaceDomainInfo namespaceDomainInfo) throws SaturnJobConsoleException;


	/**
	 * Get namespace by key.
	 *
	 * @param namespace
	 * @return
	 * @throws SaturnJobConsoleException
	 */
	NamespaceDomainInfo getNamespace(String namespace) throws SaturnJobConsoleException;

	/**
	 * Raise alarm for specified job/shard exception.
	 *
	 * @param namespace
	 * @param jobName
	 * @param executorName
	 * @param shardItem
	 * @param alarmInfo
	 * @throws SaturnJobConsoleException for below scenarios:
	 * <ul>
	 * <li>namespace or jobName is not found (statusCode = 404)</li>
	 * <li>Job with the same name is already created. (statusCode = 400)</li>
	 * </ul>
	 */
	void raiseAlarm(String namespace, String jobName, String executorName, Integer shardItem, AlarmInfo alarmInfo)
			throws SaturnJobConsoleException;

	/**
	 * Raise alarm for executor restart event.
	 *
	 * @param namespace
	 * @param executorName
	 * @param alarmInfo
	 */
	void raiseExecutorRestartAlarm(String namespace, String executorName, AlarmInfo alarmInfo) throws SaturnJobConsoleException;
}
