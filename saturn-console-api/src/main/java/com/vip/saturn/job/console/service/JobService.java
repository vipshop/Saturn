package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.ExecutorProvided;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.vo.DependencyJob;
import com.vip.saturn.job.console.vo.JobInfo;

import java.util.List;

/**
 * @author hebelala
 */
public interface JobService {

	List<JobInfo> getJobs(String namespace) throws SaturnJobConsoleException;

	List<String> getGroups(String namespace) throws SaturnJobConsoleException;

	List<DependencyJob> getDependingJobs(String namespace, String jobName) throws SaturnJobConsoleException;

	List<DependencyJob> getDependedJobs(String namespace, String jobName) throws SaturnJobConsoleException;

	void enableJob(String namespace, String jobName) throws SaturnJobConsoleException;

	void disableJob(String namespace, String jobName) throws SaturnJobConsoleException;

	void removeJob(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * 获取该作业可选择的优先Executor
	 */
	List<ExecutorProvided> getExecutors(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * 获取该域下所有online的物理机和容器
	 */
	List<ExecutorProvided> getOnlineExecutors(String namespace) throws SaturnJobConsoleException;

	void setPreferList(String namespace, String jobName, String preferList) throws SaturnJobConsoleException;

}
