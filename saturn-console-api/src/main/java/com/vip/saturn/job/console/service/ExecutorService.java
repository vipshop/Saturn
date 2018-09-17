package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.ServerAllocationInfo;
import com.vip.saturn.job.console.domain.ServerBriefInfo;
import com.vip.saturn.job.console.domain.ServerRunningInfo;
import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.io.File;
import java.util.List;

/**
 * Executor 相关的服务
 *
 * @author xiaopeng.he
 * @author kfchu
 */
public interface ExecutorService {

	/**
	 * 获取所有Executors信息；
	 *
	 * @param namespace 域
	 * @return executor信息, 如果不存在则返回空的<code>java.util.List</code>；
	 */
	List<ServerBriefInfo> getExecutors(String namespace) throws SaturnJobConsoleException;

	/**
	 * 获取特定状态的Executors信息；
	 *
	 * @param namespace 域
	 * @return executor信息, 如果不存在则返回空的<code>java.util.List</code>；
	 */
	List<ServerBriefInfo> getExecutors(String namespace, ServerStatus serverStatus) throws SaturnJobConsoleException;

	/**
	 * 获取单个Executors信息；
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 * @return executor信息, 如果不存在则返回<code>null</code>；
	 */
	ServerBriefInfo getExecutor(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 获取Executor所分配的分片信息；
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 * @return executor分配信息
	 */
	ServerAllocationInfo getExecutorAllocation(String namespace, String executorName)
			throws SaturnJobConsoleException;


	/**
	 * 获取Executor上所有正在运行的作业分片信息.
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 * @return executor所有运行中的分片信息
	 * @throws SaturnJobConsoleException
	 */
	ServerRunningInfo getExecutorRunningInfo(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 移除离线的executor.
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 */
	void removeExecutor(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 摘取流量
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 */
	void extractTraffic(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 恢复流量
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 */
	void recoverTraffic(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 重shard域下所有作业分片
	 *
	 * @param namespace 域
	 */
	void shardAll(String namespace) throws SaturnJobConsoleException;

	/**
	 * 一键dump，包括threaddump和gc.log备份，文件不作返回。
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 */
	void dump(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 一键dump，包括threaddump和gc.log备份，并返回文件到前台。
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 */
	File dumpAsFile(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 一键restart。
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 */
	void restart(String namespace, String executorName) throws SaturnJobConsoleException;
}
