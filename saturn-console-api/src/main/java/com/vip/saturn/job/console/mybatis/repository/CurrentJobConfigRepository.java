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
}