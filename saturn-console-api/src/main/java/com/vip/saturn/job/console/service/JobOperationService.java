/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service;

import java.util.Map;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;

public interface JobOperationService {

	void runAtOnceByJobnameAndExecutorName(String jobName, String executorName);

	void runAtOnceByJobnameAndExecutorName(String jobName, String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp);

	void stopAtOnceByJobnameAndExecutorName(String jobName, String executorName);

	void stopAtOnceByJobnameAndExecutorName(String jobName, String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp);

	void setJobEnabledState(String jobName, boolean state) throws SaturnJobConsoleException;

	void updateJobCron(String jobName, String cron, Map<String, String> customContext) throws SaturnJobConsoleException;

	void validateJobConfig(JobConfig jobConfig) throws SaturnJobConsoleException;

	void persistJob(JobConfig jobConfig, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException;

	void deleteJob(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException;

	void copyAndPersistJob(JobConfig jobConfig, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws Exception;

	void persistJobFromDB(JobConfig jobConfig, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException;

}
