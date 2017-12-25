package com.vip.saturn.job.console.mybatis.repository;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;

@Repository
public interface SaturnStatisticsRepository {

	int deleteByPrimaryKey(Integer id);

	SaturnStatistics selectByPrimaryKey(Integer id);

	int updateByPrimaryKey(SaturnStatistics saturnStatistics);

	int updateByPrimaryKeySelective(SaturnStatistics saturnStatistics);

	int insert(SaturnStatistics saturnStatistics);

	int insertSelective(SaturnStatistics saturnStatistics);

	int selectCount(SaturnStatistics saturnStatistics);

	SaturnStatistics findStatisticsByNameAndZkList(@Param("name") String name, @Param("zklist") String zklist);

	List<SaturnStatistics> selectPage(@Param("saturnStatistics") SaturnStatistics saturnStatistics,
			@Param("pageable") Pageable pageable);

}