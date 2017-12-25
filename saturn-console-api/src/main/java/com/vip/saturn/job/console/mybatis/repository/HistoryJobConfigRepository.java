package com.vip.saturn.job.console.mybatis.repository;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.HistoryJobConfig;

@Repository
public interface HistoryJobConfigRepository {
	int deleteByPrimaryKey(Long id);

	int insert(HistoryJobConfig historyjobconfig);

	int insertSelective(HistoryJobConfig historyjobconfig);

	HistoryJobConfig selectByPrimaryKey(Long id);

	int updateByPrimaryKeySelective(HistoryJobConfig historyjobconfig);

	int updateByPrimaryKey(HistoryJobConfig historyjobconfig);

	int selectCount(HistoryJobConfig historyjobconfig);

	List<HistoryJobConfig> selectPage(@Param("historyjobconfig") HistoryJobConfig historyjobconfig,
			@Param("pageable") Pageable pageable);
}