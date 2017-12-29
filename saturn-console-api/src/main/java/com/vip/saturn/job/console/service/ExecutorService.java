package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ServerAllocationInfo;
import com.vip.saturn.job.console.domain.ServerBriefInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import java.io.File;
import java.util.List;

/**
 * @author xiaopeng.he
 */
public interface ExecutorService {

	List<ServerBriefInfo> getExecutors(String namespace);

	ServerAllocationInfo getExecutorAllocation(String namespace, String executorName);

	void trafficExtraction(String namespace, String executorName) throws SaturnJobConsoleException;

	void trafficRecovery(String namespace, String executorName) throws SaturnJobConsoleException;

	List<String> getAliveExecutorNames();

	boolean jobIncExceeds(int maxJobNum, int inc) throws SaturnJobConsoleException;

	int getMaxJobNum();

	RequestResult addJobs(JobConfig jobConfig);

	/**
	 * Use JobOperationService.removeJob(String jobName)
	 */
	@Deprecated
	String removeJob(String jobName);

	File getExportJobFile() throws SaturnJobConsoleException;

	RequestResult shardAllAtOnce() throws SaturnJobConsoleException;

}
