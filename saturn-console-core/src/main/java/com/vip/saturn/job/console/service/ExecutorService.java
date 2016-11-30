package com.vip.saturn.job.console.service;

import java.io.File;
import java.util.List;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * 
 * @author xiaopeng.he
 *
 */
public interface ExecutorService {

	List<String> getAliveExecutorNames();
	
	RequestResult addJobs(JobConfig jobConfig);

	String removeJob(String jobName);

	File getExportJobFile() throws SaturnJobConsoleException;
	
}
