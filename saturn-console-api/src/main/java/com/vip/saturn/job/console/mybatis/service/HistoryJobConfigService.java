package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HistoryJobConfigService {

	int create(JobConfig4DB historyJobConfig) throws Exception;

	int createSelective(JobConfig4DB historyJobConfig) throws Exception;

	int deleteByPrimaryKey(Long id) throws Exception;

	JobConfig4DB findByPrimaryKey(Long id) throws Exception;

	int selectCount(JobConfig4DB historyJobConfig) throws Exception;

	int updateByPrimaryKey(JobConfig4DB historyJobConfig) throws Exception;

	int updateByPrimaryKeySelective(JobConfig4DB historyJobConfig) throws Exception;

	List<JobConfig4DB> selectPage(JobConfig4DB historyJobConfig, Pageable pageable) throws Exception;

}