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

package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CurrentJobConfigService {

	int create(JobConfig4DB currentJobConfig) throws SaturnJobConsoleException;

	int deleteByPrimaryKey(Long id) throws SaturnJobConsoleException;

	int updateByPrimaryKey(JobConfig4DB currentJobConfig) throws SaturnJobConsoleException;

	void batchUpdatePreferList(List<JobConfig4DB> jobConfigs) throws SaturnJobConsoleException;

	List<JobConfig4DB> findConfigsByNamespace(String namespace) throws SaturnJobConsoleException;

	List<JobConfig4DB> findConfigsByNamespaceWithCondition(String namespace, Map<String, Object> condition,
			Pageable pageable) throws SaturnJobConsoleException;

	int countConfigsByNamespaceWithCondition(String namespace, Map<String, Object> condition)
			throws SaturnJobConsoleException;

	int countEnabledUnSystemJobsByNamespace(String namespace) throws SaturnJobConsoleException;

	JobConfig4DB findConfigByNamespaceAndJobName(String namespace, String jobName) throws SaturnJobConsoleException;

	List<String> findConfigNamesByNamespace(String namespace) throws SaturnJobConsoleException;

	void updateNewAndSaveOld2History(final JobConfig4DB newJobConfig, final JobConfig4DB oldJobConfig,
			final String userName) throws SaturnJobConsoleException;

	void updateStream(JobConfig4DB currentJobConfig) throws SaturnJobConsoleException;

	List<JobConfig4DB> findConfigByQueue(String queue);

	void batchSetGroups(String namespace, List<String> jobNames, String groupName, String userName);

	void addToGroups(String namespace, List<String> jobNames, String groupName, String userName);

	/**
	 * 查找具有有效作业的域名列表
	 */
	List<String> findHasValidJobNamespaces(String jobType, int isEnabled);

	/**
	 * 查找有效作业的cron配置
	 */
	List<JobConfig4DB> findValidJobsCronConfig();
}