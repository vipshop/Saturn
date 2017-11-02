package com.vip.saturn.job.console.mybatis.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.vip.saturn.job.console.domain.JobSettings;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;

public interface CurrentJobConfigService {

	int create(CurrentJobConfig currentJobConfigModel) throws Exception;

	int createSelective(CurrentJobConfig currentJobConfigModel) throws Exception;

	int deleteByPrimaryKey(Long id) throws Exception;

	CurrentJobConfig findByPrimaryKey(Long id) throws Exception;

	int selectCount(CurrentJobConfig currentJobConfigModel) throws Exception;

	int updateByPrimaryKey(CurrentJobConfig currentJobConfigModel) throws Exception;

	int updateByPrimaryKeySelective(CurrentJobConfig currentJobConfigModel) throws Exception;

	void batchUpdatePreferList(List<CurrentJobConfig> jobConfigs) throws SaturnJobConsoleException;

	List<CurrentJobConfig> findConfigsByNamespace(String namespace);

	CurrentJobConfig findConfigByNamespaceAndJobName(String namespace, String jobName);

	List<CurrentJobConfig> selectPage(CurrentJobConfig historyjobconfig, Pageable pageable) throws Exception;

	void updateConfigAndSave2History(final CurrentJobConfig jobconfig, final JobSettings jobSettings,
			final String userName) throws Exception;

	void updateConfigAndSave2History(final CurrentJobConfig newJobconfig, final CurrentJobConfig oldJobconfig,
			final String userName) throws Exception;

	int deleteAll(int limitNum);
}