package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.ExecutorProvided;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.vo.DependencyJob;
import com.vip.saturn.job.console.vo.JobInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
	List<ExecutorProvided> getCandidateExecutors(String namespace, String jobName) throws SaturnJobConsoleException;

	void setPreferList(String namespace, String jobName, String preferList) throws SaturnJobConsoleException;

	void validateJobConfig(JobConfig jobConfig) throws SaturnJobConsoleException;

	void addJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException;

	int getMaxJobNum() throws SaturnJobConsoleException;

	boolean jobIncExceeds(String namespace, int maxJobNum, int inc) throws SaturnJobConsoleException;

	List<CurrentJobConfig> getUnSystemJobs(String namespace) throws SaturnJobConsoleException;

	void persistJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException;

	void persistJobCopied(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException;

	void importJobs(String namespace, MultipartFile file) throws SaturnJobConsoleException;

	File exportJobs(String namespace) throws SaturnJobConsoleException;

	JobConfig getJobConfig(String namespace, String jobName);

}
