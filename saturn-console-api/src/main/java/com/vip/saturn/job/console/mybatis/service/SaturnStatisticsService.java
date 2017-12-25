
package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;

public interface SaturnStatisticsService {

	int create(SaturnStatistics SaturnStatistics);

	int createSelective(SaturnStatistics SaturnStatistics);

	SaturnStatistics findByPrimaryKey(Integer id);

	int updateByPrimaryKey(SaturnStatistics SaturnStatistics);

	int updateByPrimaryKeySelective(SaturnStatistics SaturnStatistics);

	int deleteByPrimaryKey(Integer id);

	SaturnStatistics findStatisticsByNameAndZkList(String name, String zkConnectionString);

	int selectCount(SaturnStatistics SaturnStatistics);

}