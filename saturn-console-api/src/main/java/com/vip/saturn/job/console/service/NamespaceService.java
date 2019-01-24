package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.util.List;

/**
 * @author rayleung
 */
public interface NamespaceService {

	/**
	 * 把源域的作业导入到目标域
	 * @param srcNamespace
	 * @param destNamespace
	 * @param createdBy
	 * @return Jobs that import successfully
	 * @throws SaturnJobConsoleException
	 */
	List<String> importJobsFromNamespaceToNamespace(String srcNamespace, String destNamespace, String createdBy)
			throws SaturnJobConsoleException;

}
