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

package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CurrentJobConfigRepository {

	int deleteByPrimaryKey(Long id);

	int deleteByNamespace(String namespace);

	int insert(JobConfig4DB currentJobConfig);

	int updateByPrimaryKey(JobConfig4DB currentJobConfig);

	void updatePreferList(JobConfig4DB currentJobConfig);

	void updateStream(JobConfig4DB currentJobConfig);

	List<JobConfig4DB> findConfigsByNamespace(@Param("namespace") String namespace);

	List<JobConfig4DB> findConfigsByNamespaceWithCondition(@Param("namespace") String namespace,
			@Param("condition") Map<String, Object> condition, @Param("pageable") Pageable pageable);

	int countConfigsByNamespaceWithCondition(@Param("namespace") String namespace,
			@Param("condition") Map<String, Object> condition);

	int countEnabledUnSystemJobsByNamespace(@Param("namespace") String namespace, @Param("isEnabled") int isEnabled);

	List<String> findConfigNamesByNamespace(@Param("namespace") String namespace);

	JobConfig4DB findConfigByNamespaceAndJobName(@Param("namespace") String namespace,
			@Param("jobName") String jobName);

	List<JobConfig4DB> findConfigsByQueue(@Param("queueName") String queueName);

	void batchSetGroups(@Param("namespace") String namespace, @Param("jobNames") List<String> jobNames, @Param("groupName") String groupName, @Param("lastUpdateBy") String lastUpdateBy);

	void addToGroups(@Param("namespace") String namespace, @Param("jobNames") List<String> jobNames, @Param("groupName") String groupName, @Param("lastUpdateBy") String lastUpdateBy);

	/**
	 * 查找具有有效作业的域名列表
	 */
	List<String> findHasValidJobNamespaces(@Param("jobType") String jobType, @Param("isEnabled") int isEnabled);

	/**
	 * 查找有效作业的cron配置
	 */
	List<JobConfig4DB> findValidJobsCronConfig();
}