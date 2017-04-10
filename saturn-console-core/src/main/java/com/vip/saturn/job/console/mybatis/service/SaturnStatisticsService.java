
package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;

public interface SaturnStatisticsService{
	
	public int create(SaturnStatistics SaturnStatistics);
	
	public int createSelective(SaturnStatistics SaturnStatistics);
	
	public SaturnStatistics findByPrimaryKey(Integer id);
	
	public int updateByPrimaryKey(SaturnStatistics SaturnStatistics);
	
	public int updateByPrimaryKeySelective(SaturnStatistics SaturnStatistics);
	
	public int deleteByPrimaryKey(Integer id);
	
	SaturnStatistics findStatisticsByNameAndZkList(String name, String zklist );
	
	public int selectCount(SaturnStatistics SaturnStatistics);
	
}