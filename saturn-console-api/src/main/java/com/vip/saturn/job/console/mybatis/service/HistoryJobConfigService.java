package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HistoryJobConfigService {

	int create(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	int createSelective(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	int deleteByPrimaryKey(Long id) throws SaturnJobConsoleException;

	JobConfig4DB findByPrimaryKey(Long id) throws SaturnJobConsoleException;

	int selectCount(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	int updateByPrimaryKey(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	int updateByPrimaryKeySelective(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	List<JobConfig4DB> selectPage(JobConfig4DB historyJobConfig, Pageable pageable) throws SaturnJobConsoleException;

}