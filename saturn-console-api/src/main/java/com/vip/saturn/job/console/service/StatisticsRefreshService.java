package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author timmy.hu
 */
public interface StatisticsRefreshService {

	void refresh(String zkClusterKey, boolean isForce) throws SaturnJobConsoleException;

}
