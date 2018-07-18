package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import java.util.Map;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CurrentJobConfigService {

	int create(JobConfig4DB currentJobConfig) throws Exception;

	int createSelective(JobConfig4DB currentJobConfig) throws Exception;

	int deleteByPrimaryKey(Long id) throws Exception;

	JobConfig4DB findByPrimaryKey(Long id) throws Exception;

	int selectCount(JobConfig4DB currentJobConfig) throws Exception;

	int updateByPrimaryKey(JobConfig4DB currentJobConfig) throws Exception;

	int updateByPrimaryKeySelective(JobConfig4DB currentJobConfig) throws Exception;

	void batchUpdatePreferList(List<JobConfig4DB> jobConfigs) throws SaturnJobConsoleException;

	List<JobConfig4DB> findConfigsByNamespace(String namespace);

	List<JobConfig4DB> findConfigsByNamespaceWithCondition(String namespace, Map<String, Object> condition,
			Pageable pageable);

	int countConfigsByNamespaceWithCondition(String namespace, Map<String, Object> condition);

	int countEnabledUnSystemJobsByNamespace(String namespace);

	JobConfig4DB findConfigByNamespaceAndJobName(String namespace, String jobName);

	List<String> findConfigNamesByNamespace(String namespace);

	List<JobConfig4DB> selectPage(JobConfig4DB currentJobConfig, Pageable pageable) throws Exception;

	void updateNewAndSaveOld2History(final JobConfig4DB newJobConfig, final JobConfig4DB oldJobConfig,
			final String userName) throws Exception;

	int deleteAll(int limitNum);
}