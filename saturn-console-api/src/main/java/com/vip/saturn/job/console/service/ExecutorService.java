package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import java.io.File;
import java.util.List;

/**
 * @author xiaopeng.he
 */
public interface ExecutorService {

	List<String> getAliveExecutorNames();

	boolean jobIncExceeds(int maxJobNum, int inc) throws SaturnJobConsoleException;

	int getMaxJobNum();

	RequestResult addJobs(JobConfig jobConfig);

	@Deprecated
	/**
	 * Use JobOperationService.removeJob(String jobName)
	 */
	String removeJob(String jobName);

	File getExportJobFile() throws SaturnJobConsoleException;

	RequestResult shardAllAtOnce() throws SaturnJobConsoleException;

}
