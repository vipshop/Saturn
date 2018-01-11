package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.io.File;
import java.util.List;

/**
 * 
 * @author hebelala
 *
 */
public interface ExecutorService {

	List<String> getAliveExecutorNames();

	boolean jobIncExceeds(int maxJobNum, int inc) throws SaturnJobConsoleException;

	int getMaxJobNum();

	RequestResult addJobs(JobConfig jobConfig);

	/**
	 * Use JobOperationService.removeJob(String jobName)
	 */
	@Deprecated
	void removeJob(String jobName) throws SaturnJobConsoleException;

	File getExportJobFile() throws SaturnJobConsoleException;

	RequestResult shardAllAtOnce() throws SaturnJobConsoleException;

}
