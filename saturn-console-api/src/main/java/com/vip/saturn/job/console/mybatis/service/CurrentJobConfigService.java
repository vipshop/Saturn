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
}