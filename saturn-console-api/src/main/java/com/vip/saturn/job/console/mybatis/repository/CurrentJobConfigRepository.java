package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrentJobConfigRepository {

	int deleteByPrimaryKey(Long id);

	int insert(JobConfig4DB currentJobConfig);

	int insertSelective(JobConfig4DB currentJobConfig);

	JobConfig4DB selectByPrimaryKey(Long id);

	int updateByPrimaryKeySelective(JobConfig4DB currentJobConfig);

	int updateByPrimaryKey(JobConfig4DB currentJobConfig);

	void updatePreferList(JobConfig4DB currentJobConfig);

	int selectCount(JobConfig4DB currentJobConfig);

	List<JobConfig4DB> findConfigsByNamespace(@Param("namespace") String namespace);

	List<JobConfig4DB> findConfigsByNamespaceWithCondition(@Param("namespace") String namespace,
			@Param("condition") Map<String, Object> condition, @Param("pageable") Pageable pageable);

	int countConfigsByNamespaceWithCondition(@Param("namespace") String namespace,
			@Param("condition") Map<String, Object> condition);

	int countEnabledUnSystemJobsByNamespace(@Param("namespace") String namespace, @Param("isEnabled")int isEnabled);
	
	List<String> findConfigNamesByNamespace(@Param("namespace") String namespace);

	List<String> findConfigGroupsByNamespace(@Param("namespace") String namespace);

	JobConfig4DB findConfigByNamespaceAndJobName(@Param("namespace") String namespace,
			@Param("jobName") String jobName);

	List<JobConfig4DB> selectPage(@Param("currentJobConfig") JobConfig4DB currentJobConfig,
			@Param("pageable") Pageable pageable);

	int deleteAll(int limitNum);

}