package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author timmy.hu
 */
public interface StatisticsRefreshService {

	void refreshBySameIdcConsole(String zkClusterKey) throws SaturnJobConsoleException;

	void refreshDirectly(String zkClusterKey) throws SaturnJobConsoleException;

}
