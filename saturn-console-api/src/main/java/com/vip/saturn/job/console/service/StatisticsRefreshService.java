package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author timmy.hu
 */
public interface StatisticsRefreshService {

	void refreshStatistics(String zkClusterKey) throws SaturnJobConsoleException;

	void refreshStatistics2DB(boolean force);

	void refreshStatistics2DB(String zkClusterKey) throws SaturnJobConsoleException;

}
