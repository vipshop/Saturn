package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryJobConfigRepository {

	int deleteByPrimaryKey(Long id);

	int insert(JobConfig4DB historyJobConfig);

	int insertSelective(JobConfig4DB historyJobConfig);

	JobConfig4DB selectByPrimaryKey(Long id);

	int updateByPrimaryKeySelective(JobConfig4DB historyJobConfig);

	int updateByPrimaryKey(JobConfig4DB historyJobConfig);

	int selectCount(JobConfig4DB historyJobConfig);

	List<JobConfig4DB> selectPage(@Param("historyJobConfig") JobConfig4DB historyJobConfig,
			@Param("pageable") Pageable pageable);
}