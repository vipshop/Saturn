package com.vip.saturn.job.console.service.impl.refresh.analyzer;

import com.vip.saturn.job.console.domain.AbnormalJob;
import com.vip.saturn.job.console.domain.ExecutorProvided;
import com.vip.saturn.job.console.domain.ExecutorProvidedType;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author timmy.hu
 */
public class UnableFailoverJobAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(UnableFailoverJobAnalyzer.class);

	private JobService jobService;

	private List<AbnormalJob> unableFailoverJobList = new ArrayList<AbnormalJob>();

	/**
	 * 查找无法高可用的作业
	 */
	public void analyze(CuratorFrameworkOp curatorFrameworkOp, String jobName, String jobDegree,
			RegistryCenterConfiguration config) {
		AbnormalJob unableFailoverJob = new AbnormalJob(jobName, config.getNamespace(), config.getNameAndNamespace(),
				config.getDegree());
		if (isUnableFailoverJob(curatorFrameworkOp, unableFailoverJob)) {
			unableFailoverJob.setJobDegree(jobDegree);
			addUnableFailoverJob(unableFailoverJob);
		}
	}

	private synchronized void addUnableFailoverJob(AbnormalJob unableFailoverJob) {
		unableFailoverJobList.add(unableFailoverJob);
	}

	/**
	 * 无法高可用的情况： 1、勾选只使用优先executor，preferList只有一个物理机器（剔除offline、deleted的物理机） 2、没有勾选只使用优先executor，没有选择容器资源，可供选择的preferList只有一个物理机器（剔除offline、deleted的物理机，剔除容器资源）
	 */
	private boolean isUnableFailoverJob(CuratorFrameworkOp curatorFrameworkOp, AbnormalJob unableFailoverJob) {
		try {
			String jobName = unableFailoverJob.getJobName();
			String preferList = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList"));
			List<ExecutorProvided> preferListProvided = jobService
					.getCandidateExecutors(curatorFrameworkOp.getCuratorFramework().getNamespace(), jobName);
			if (CollectionUtils.isEmpty(preferListProvided)) {
				return false;
			}
			List<String> preferListArr = toPerferListArr(preferList);
			boolean containerSelected = false;
			int count = 0;
			if (onlyUsePreferList(curatorFrameworkOp, jobName)) {
				for (ExecutorProvided executorProvided : preferListProvided) {
					if (preferListArr.contains(executorProvided.getExecutorName())) {
						if (ExecutorProvidedType.DOCKER.equals(executorProvided.getType())) {
							containerSelected = true;
							break;
						} else if (ExecutorProvidedType.ONLINE.equals(executorProvided.getType())) {
							count++;
						}
					}
				}
			} else {
				for (ExecutorProvided executorProvided : preferListProvided) {
					if (preferListArr.contains(executorProvided.getExecutorName())
							&& ExecutorProvidedType.DOCKER.equals(executorProvided.getType())) {
						containerSelected = true;
						break;
					}
					if (ExecutorProvidedType.ONLINE.equals(executorProvided.getType())) {
						count++;
						if (count > 1) {
							break;
						}
					}
				}
			}
			return !containerSelected && count == 1;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	private boolean onlyUsePreferList(CuratorFrameworkOp curatorFrameworkOp, String jobName) {
		String useDispreferListStr = curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, "useDispreferList"));
		return useDispreferListStr == null ? false : !Boolean.valueOf(useDispreferListStr);
	}

	private List<String> toPerferListArr(String preferList) {
		List<String> preferListArr = new ArrayList<>();
		if (StringUtils.isBlank(preferList)) {
			return preferListArr;
		}
		String[] split = preferList.split(",");
		for (String prefer : split) {
			String tmp = prefer.trim();
			if (tmp.length() > 0 && !preferListArr.contains(tmp)) {
				preferListArr.add(tmp);
			}
		}
		return preferListArr;
	}

	public List<AbnormalJob> getUnableFailoverJobList() {
		return new ArrayList<AbnormalJob>(unableFailoverJobList);
	}

	public void setJobService(JobService jobService) {
		this.jobService = jobService;
	}
}
