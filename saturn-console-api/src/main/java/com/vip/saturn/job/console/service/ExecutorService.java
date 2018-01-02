package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ServerAllocationInfo;
import com.vip.saturn.job.console.domain.ServerBriefInfo;
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
	 * 获取单个Executors信息；
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 * @return executor信息, 如果不存在则返回<code>null</code>；
	 */
	ServerBriefInfo getExecutor(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 获取Executor状态。
	 *
	 * @param executorName 目标executor
	 * @param namespace 域
	 * @return <code>ServerStatus</code>
	 */
	ServerStatus getExecutorStatus(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 获取Executor所分配的分片信息；
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 * @return executor分配信息
	 */
	ServerAllocationInfo getExecutorAllocation(String namespace, String executorName) throws SaturnJobConsoleException;

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
	void trafficExtraction(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 恢复流量
	 *
	 * @param namespace 域
	 * @param executorName 目标executor
	 */
	void trafficRecovery(String namespace, String executorName) throws SaturnJobConsoleException;

	/**
	 * 重shard域下所有作业分片
	 *
	 * @param namespace 域
	 */
	void shardAll(String namespace) throws SaturnJobConsoleException;

}
