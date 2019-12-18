package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author Ray Leung
 */
public interface NamespaceAndJobService {

	/**
	 * 同步新建域和拷贝作业
	 */
	void createNamespaceAndCloneJobs(String srcNamespace, String namespace, String zkClusterName, String createBy)
			throws SaturnJobConsoleException;

	/**
	 * 异步新建域和拷贝作业
	 */
	void asyncCreateNamespaceAndCloneJobs(String srcNamespace, String namespace, String zkClusterName, String createBy)
			throws SaturnJobConsoleException;

}
