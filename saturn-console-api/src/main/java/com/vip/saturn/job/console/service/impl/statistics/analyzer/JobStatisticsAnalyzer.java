package com.vip.saturn.job.console.service.impl.statistics.analyzer;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.JobStatistics;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author timmy.hu
 */
public class JobStatisticsAnalyzer {

	private Map<String/** {jobname}-{domain} */
			, JobStatistics> jobMap = new ConcurrentHashMap<>();

	public JobStatistics analyze(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String job, boolean localMode,
			RegistryCenterConfiguration config, ExecutorInfoAnalyzer executorInfoAnalyzer) throws Exception {
		String jobDomainKey = job + "-" + config.getNamespace();
		JobStatistics jobStatistics = jobMap.get(jobDomainKey);
		if (jobStatistics == null) {
			jobStatistics = new JobStatistics(job, config.getNamespace(), config.getNameAndNamespace());
			jobMap.put(jobDomainKey, jobStatistics);
		}

		String jobDegree = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(job, "jobDegree"));
		if (Strings.isNullOrEmpty(jobDegree)) {
			jobDegree = "0";
		}
		jobStatistics.setJobDegree(Integer.parseInt(jobDegree));

		int processCountOfThisJobAllTime = getProcessCountAllTime(curatorFrameworkOp, job);
		int errorCountOfThisJobAllTime = getErrorCountAllTime(curatorFrameworkOp, job);

		// loadLevel of this job
		int loadLevel = Integer
				.parseInt(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(job, "loadLevel")));
		int shardingTotalCount = Integer.parseInt(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(job, "shardingTotalCount")));
		List<String> servers = null;
		if (curatorFrameworkOp.checkExists(JobNodePath.getServerNodePath(job))) {
			servers = curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(job));
			executorInfoAnalyzer.analyzeServer(curatorFrameworkOp, servers, job, config.getNameAndNamespace(), config,
					loadLevel, jobStatistics);
		}
		// local-mode job = server count(regardless server status)
		if (localMode) {
			jobStatistics.setTotalLoadLevel(servers == null ? 0 : (servers.size() * loadLevel));
		} else {
			jobStatistics.setTotalLoadLevel(loadLevel * shardingTotalCount);
		}
		jobStatistics.setErrorCountOfAllTime(errorCountOfThisJobAllTime);
		jobStatistics.setProcessCountOfAllTime(processCountOfThisJobAllTime);
		jobMap.put(jobDomainKey, jobStatistics);
		return jobStatistics;
	}

	public static int getProcessCountAllTime(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName) {
		String processCountOfThisJobAllTimeStr = curatorFrameworkOp.getData(JobNodePath.getProcessCountPath(jobName));
		return StringUtils.isBlank(processCountOfThisJobAllTimeStr) ? 0
				: Integer.parseInt(processCountOfThisJobAllTimeStr);
	}

	public static int getErrorCountAllTime(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName) {
		String errorCountOfThisJobAllTimeStr = curatorFrameworkOp.getData(JobNodePath.getErrorCountPath(jobName));
		return StringUtils.isBlank(errorCountOfThisJobAllTimeStr) ? 0 : Integer.parseInt(errorCountOfThisJobAllTimeStr);
	}

	public Map<String, JobStatistics> getJobMap() {
		return jobMap;
	}

	public List<JobStatistics> getJobList() {
		return new ArrayList<JobStatistics>(jobMap.values());
	}
}
