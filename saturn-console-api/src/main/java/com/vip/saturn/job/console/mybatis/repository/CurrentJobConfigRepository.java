package com.vip.saturn.job.console.mybatis.repository;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;

@Repository
public interface CurrentJobConfigRepository {

	int deleteByPrimaryKey(Long id);

	int insert(CurrentJobConfig currentjobconfig);

	int insertSelective(CurrentJobConfig currentjobconfig);

	CurrentJobConfig selectByPrimaryKey(Long id);

	int updateByPrimaryKeySelective(CurrentJobConfig currentjobconfig);

	int updateByPrimaryKey(CurrentJobConfig currentjobconfig);
	
	void updatePreferList(CurrentJobConfig currentjobconfig);

	int selectCount(CurrentJobConfig currentjobconfig);

	List<CurrentJobConfig> findConfigsByNamespace(@Param("namespace") String namespace);

	CurrentJobConfig findConfigByNamespaceAndJobName(@Param("namespace") String namespace,
			@Param("jobName") String jobName);

	List<CurrentJobConfig> selectPage(@Param("currentJobConfig") CurrentJobConfig currentjobconfig,
			@Param("pageable") Pageable pageable);

	int deleteAll(int limitNum);

}