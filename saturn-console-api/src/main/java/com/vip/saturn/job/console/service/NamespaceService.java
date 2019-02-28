package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.util.List;
import java.util.Map;

/**
 * @author rayleung
 */
public interface NamespaceService {

	/**
	 * 把源域的作业导入到目标域
	 * @param srcNamespace
	 * @param destNamespace
	 * @param createdBy
	 * @return 导入成功和导入失败作业列表
	 * @throws SaturnJobConsoleException
	 */
	Map<String, List> importJobsFromNamespaceToNamespace(String srcNamespace, String destNamespace, String createdBy)
			throws SaturnJobConsoleException;

	/**
	 * 删除域
	 * @param namespace
	 */
	boolean deleteNamespace(String namespace) throws SaturnJobConsoleException;

}
