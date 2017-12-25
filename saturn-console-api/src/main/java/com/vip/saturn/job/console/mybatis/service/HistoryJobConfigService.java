package com.vip.saturn.job.console.mybatis.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.vip.saturn.job.console.mybatis.entity.HistoryJobConfig;

public interface HistoryJobConfigService {

	int create(HistoryJobConfig historyJobConfigModel) throws Exception;

	int createSelective(HistoryJobConfig historyJobConfigModel) throws Exception;

	int deleteByPrimaryKey(Long id) throws Exception;

	HistoryJobConfig findByPrimaryKey(Long id) throws Exception;

	int selectCount(HistoryJobConfig historyJobConfigModel) throws Exception;

	int updateByPrimaryKey(HistoryJobConfig historyJobConfigModel) throws Exception;

	int updateByPrimaryKeySelective(HistoryJobConfig historyJobConfigModel) throws Exception;

	List<HistoryJobConfig> selectPage(HistoryJobConfig historyjobconfig, Pageable pageable) throws Exception;

}