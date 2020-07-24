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

package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.BatchJobResult;
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
	void runJobAtOnce(String namespace, String jobName, Map<String, Object> triggeredData) throws SaturnJobConsoleException;

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

	/**
	 * Trigger the job's downStream to run
	 */
	List<BatchJobResult> runDownStream(String namespace, String jobName, Map<String, Object> triggeredData)
			throws SaturnJobConsoleException;

	/**
	 * Get the enabled jobs brief info under the namespace
	 */
	List<JobConfig> getJobConfigList(String namespace) throws SaturnJobConsoleException;
}
