package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.DependencyJob;
import com.vip.saturn.job.console.domain.ExecutionInfo;
import com.vip.saturn.job.console.domain.ExecutorProvided;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobInfo;
import com.vip.saturn.job.console.domain.JobOverviewVo;
import com.vip.saturn.job.console.domain.JobServer;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author hebelala
 */
public interface JobService {

	JobOverviewVo getJobOverviewVo(String namespace) throws SaturnJobConsoleException;

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

	void addJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException;

	void copyJob(String namespace, JobConfig jobConfig, String copyingJobName) throws SaturnJobConsoleException;

	int getMaxJobNum() throws SaturnJobConsoleException;

	boolean jobIncExceeds(String namespace, int maxJobNum, int inc) throws SaturnJobConsoleException;

	List<JobConfig> getUnSystemJobs(String namespace) throws SaturnJobConsoleException;

	List<String> getUnSystemJobNames(String namespace) throws SaturnJobConsoleException;

	void persistJobFromDB(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException;

	void importJobs(String namespace, MultipartFile file) throws SaturnJobConsoleException;

	File exportJobs(String namespace) throws SaturnJobConsoleException;

	JobConfig getJobConfigFromZK(String namespace, String jobName) throws SaturnJobConsoleException;

	JobConfig getJobConfig(String namespace, String jobName) throws SaturnJobConsoleException;

	JobStatus getJobStatus(String namespace, String jobName) throws SaturnJobConsoleException;

	JobInfo getJobInfo(String namespace, String jobName) throws SaturnJobConsoleException;

	void updateJobConfig(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException;

	List<String> getAllJobNamesFromZK(String namespace) throws SaturnJobConsoleException;

	void updateJobCron(String namespace, String jobName, String cron, Map<String, String> customContext)
			throws SaturnJobConsoleException;

	List<JobServer> getJobServers(String namespace, String jobName) throws SaturnJobConsoleException;

	// TODO
	void runAtOnce(String namespace, String jobName, String executorName) throws SaturnJobConsoleException;

	// TODO
	void stopAtOnce(String namespace, String jobName, String executorName) throws SaturnJobConsoleException;

	/**
	 * 获取作业运行状态
	 */
	List<ExecutionInfo> getExecutionStatus(String namespace, String jobName) throws SaturnJobConsoleException;
}
