package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;

/**
 * @author Ray Leung
 */
public interface StatisticsRefreshObserver {

	/**
	 * 在循环分析作业前通知Observer
	 * @param config
	 */
	void beforeAnalyzeJobs(RegistryCenterConfiguration config);

	/**
	 * 在循环分析作业结束后通知Observer
	 * @param config
	 */
	void afterAnalyzeJobs(RegistryCenterConfiguration config);
}