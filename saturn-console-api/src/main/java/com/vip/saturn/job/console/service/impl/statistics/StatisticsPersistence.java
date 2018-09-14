package com.vip.saturn.job.console.service.impl.statistics;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.mybatis.service.SaturnStatisticsService;
import com.vip.saturn.job.console.service.helper.DashboardServiceHelper;
import com.vip.saturn.job.console.service.impl.statistics.analyzer.StatisticsModel;
import com.vip.saturn.job.console.utils.StatisticsTableKeyConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author timmy.hu
 */
public class StatisticsPersistence {

	private static final Logger log = LoggerFactory.getLogger(StatisticsPersistence.class);

	@Resource
	private SaturnStatisticsService saturnStatisticsService;

	public void persist(StatisticsModel statisticsModel, ZkCluster zkCluster) {
		List<JobStatistics> jobList = statisticsModel.getJobStatisticsAnalyzer().getJobList();
		List<ExecutorStatistics> executorList = statisticsModel.getExecutorInfoAnalyzer().getExecutorList();

		// 全域当天处理总数，失败总数
		saveOrUpdateDomainProcessCount(
				new ZkStatistics(statisticsModel.getZkClusterDailyCountAnalyzer().getTotalCount(),
						statisticsModel.getZkClusterDailyCountAnalyzer().getErrorCount()),
				zkCluster.getZkAddr());

		// 失败率Top10的域列表
		saveOrUpdateTop10FailDomain(statisticsModel.getDomainStatisticsAnalyzer().getDomainList(),
				zkCluster.getZkAddr());

		// 稳定性最差的Top10的域列表
		saveOrUpdateTop10UnstableDomain(statisticsModel.getDomainStatisticsAnalyzer().getDomainList(),
				zkCluster.getZkAddr());

		// 稳定性最差的Top10的executor列表
		saveOrUpdateTop10FailExecutor(executorList, zkCluster.getZkAddr());

		// 根据失败率Top10的作业列表
		saveOrUpdateTop10FailJob(jobList, zkCluster.getZkAddr());

		// 最活跃作业Top10的作业列表(即当天执行次数最多的作业)
		saveOrUpdateTop10ActiveJob(jobList, zkCluster.getZkAddr());

		// 负荷最重的Top10的作业列表
		saveOrUpdateTop10LoadJob(jobList, zkCluster.getZkAddr());

		// 负荷最重的Top10的Executor列表
		saveOrUpdateTop10LoadExecutor(executorList, zkCluster.getZkAddr());

		// 异常作业列表 (如下次调度时间已经过了，但是作业没有被调度)
		saveOrUpdateAbnormalJob(statisticsModel.getOutdatedNoRunningJobAnalyzer().getOutdatedNoRunningJobs(),
				zkCluster.getZkAddr());

		// 超时告警的作业列表
		saveOrUpdateTimeout4AlarmJob(statisticsModel.getTimeout4AlarmJobAnalyzer().getTimeout4AlarmJobList(),
				zkCluster.getZkAddr());

		// 无法高可用的作业列表
		saveOrUpdateUnableFailoverJob(statisticsModel.getUnableFailoverJobAnalyzer().getUnableFailoverJobList(),
				zkCluster.getZkAddr());

		// 不同版本的域数量
		saveOrUpdateVersionDomainNumber(statisticsModel.getExecutorInfoAnalyzer().getVersionDomainNumber(),
				zkCluster.getZkAddr());

		// 不同版本的executor数量
		saveOrUpdateVersionExecutorNumber(statisticsModel.getExecutorInfoAnalyzer().getVersionExecutorNumber(),
				zkCluster.getZkAddr());

		// 不同作业等级的作业数量
		saveOrUpdateJobRankDistribution(jobList, zkCluster.getZkAddr());

		// 容器executor数量
		saveOrUpdateExecutorInDockerCount(statisticsModel.getExecutorInfoAnalyzer().getExeInDocker(),
				zkCluster.getZkAddr());

		// 物理机executor数量
		saveOrUpdateExecutorNotInDockerCount(statisticsModel.getExecutorInfoAnalyzer().getExeNotInDocker(),
				zkCluster.getZkAddr());

		// 作业数量
		saveOrUpdateJobCount(jobList.size(), zkCluster.getZkAddr());
	}

	private void saveOrUpdateJobCount(int jobCount, String zkAddr) {
		try {
			String jobCountString = JSON.toJSONString(jobCount);
			SaturnStatistics jobCountFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.JOB_COUNT, zkAddr);
			if (jobCountFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.JOB_COUNT, zkAddr,
						jobCountString);
				saturnStatisticsService.create(ss);
			} else {
				jobCountFromDB.setResult(jobCountString);
				saturnStatisticsService.updateByPrimaryKey(jobCountFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateExecutorNotInDockerCount(int exeNotInDocker, String zkAddr) {
		try {
			String exeNotInDockerString = JSON.toJSONString(exeNotInDocker);
			SaturnStatistics exeNotInDockerFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.EXECUTOR_NOT_IN_DOCKER_COUNT, zkAddr);
			if (exeNotInDockerFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.EXECUTOR_NOT_IN_DOCKER_COUNT,
						zkAddr, exeNotInDockerString);
				saturnStatisticsService.create(ss);
			} else {
				exeNotInDockerFromDB.setResult(exeNotInDockerString);
				saturnStatisticsService.updateByPrimaryKey(exeNotInDockerFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateExecutorInDockerCount(int exeInDocker, String zkAddr) {
		try {
			String exeInDockerString = JSON.toJSONString(exeInDocker);
			SaturnStatistics exeInDockerFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.EXECUTOR_IN_DOCKER_COUNT, zkAddr);
			if (exeInDockerFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.EXECUTOR_IN_DOCKER_COUNT, zkAddr,
						exeInDockerString);
				saturnStatisticsService.create(ss);
			} else {
				exeInDockerFromDB.setResult(exeInDockerString);
				saturnStatisticsService.updateByPrimaryKey(exeInDockerFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateJobRankDistribution(List<JobStatistics> jobList, String zkBsKey) {
		try {
			Map<Integer, Integer> jobDegreeCountMap = new HashMap<>();
			for (JobStatistics jobStatistics : jobList) {
				int jobDegree = jobStatistics.getJobDegree();
				Integer count = jobDegreeCountMap.get(jobDegree);
				jobDegreeCountMap.put(jobDegree, count == null ? 1 : count + 1);
			}
			String jobDegreeMapString = JSON.toJSONString(jobDegreeCountMap);
			SaturnStatistics jobDegreeMapFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.JOB_RANK_DISTRIBUTION, zkBsKey);
			if (jobDegreeMapFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.JOB_RANK_DISTRIBUTION, zkBsKey,
						jobDegreeMapString);
				saturnStatisticsService.create(ss);
			} else {
				jobDegreeMapFromDB.setResult(jobDegreeMapString);
				saturnStatisticsService.updateByPrimaryKey(jobDegreeMapFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateTop10FailExecutor(List<ExecutorStatistics> executorList, String zkAddr) {
		try {
			executorList = DashboardServiceHelper.sortExecutorByFailureRate(executorList);
			List<ExecutorStatistics> top10FailExecutor = executorList.subList(0,
					executorList.size() > 9 ? 10 : executorList.size());
			String top10FailExecutorJsonString = JSON.toJSONString(top10FailExecutor);
			SaturnStatistics top10FailExecutorFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_EXECUTOR, zkAddr);
			if (top10FailExecutorFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_FAIL_EXECUTOR, zkAddr,
						top10FailExecutorJsonString);
				saturnStatisticsService.create(ss);
			} else {
				top10FailExecutorFromDB.setResult(top10FailExecutorJsonString);
				saturnStatisticsService.updateByPrimaryKey(top10FailExecutorFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	private void saveOrUpdateTop10FailDomain(List<DomainStatistics> domainList, String zkAddr) {
		try {
			domainList = DashboardServiceHelper.sortDomainByAllTimeFailureRate(domainList);
			List<DomainStatistics> top10FailDomainList = domainList.subList(0,
					domainList.size() > 9 ? 10 : domainList.size());
			String top10FailDomainJsonString = JSON.toJSONString(top10FailDomainList);
			SaturnStatistics top10FailDomainFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_DOMAIN, zkAddr);
			if (top10FailDomainFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_FAIL_DOMAIN, zkAddr,
						top10FailDomainJsonString);
				saturnStatisticsService.create(ss);
			} else {
				top10FailDomainFromDB.setResult(top10FailDomainJsonString);
				saturnStatisticsService.updateByPrimaryKey(top10FailDomainFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateTop10UnstableDomain(List<DomainStatistics> domainList, String zkAddr) {
		try {
			domainList = DashboardServiceHelper.sortDomainByShardingCount(domainList);
			List<DomainStatistics> top10UnstableDomain = domainList.subList(0,
					domainList.size() > 9 ? 10 : domainList.size());
			String top10UnstableDomainJsonString = JSON.toJSONString(top10UnstableDomain);
			SaturnStatistics top10UnstableDomainFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_UNSTABLE_DOMAIN, zkAddr);
			if (top10UnstableDomainFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_UNSTABLE_DOMAIN, zkAddr,
						top10UnstableDomainJsonString);
				saturnStatisticsService.create(ss);
			} else {
				top10UnstableDomainFromDB.setResult(top10UnstableDomainJsonString);
				saturnStatisticsService.updateByPrimaryKey(top10UnstableDomainFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateTop10FailJob(List<JobStatistics> jobList, String zkAddr) {
		try {
			jobList = DashboardServiceHelper.sortJobByAllTimeFailureRate(jobList);
			List<JobStatistics> top10FailJob = jobList.subList(0, jobList.size() > 9 ? 10 : jobList.size());
			String top10FailJobJsonString = JSON.toJSONString(top10FailJob);
			SaturnStatistics top10FailJobFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_JOB, zkAddr);
			if (top10FailJobFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_FAIL_JOB, zkAddr,
						top10FailJobJsonString);
				saturnStatisticsService.create(ss);
			} else {
				top10FailJobFromDB.setResult(top10FailJobJsonString);
				saturnStatisticsService.updateByPrimaryKey(top10FailJobFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateTop10ActiveJob(List<JobStatistics> jobList, String zkAddr) {
		try {
			jobList = DashboardServiceHelper.sortJobByDayProcessCount(jobList);
			List<JobStatistics> top10ActiveJob = jobList.subList(0, jobList.size() > 9 ? 10 : jobList.size());
			String top10ActiveJobJsonString = JSON.toJSONString(top10ActiveJob);
			SaturnStatistics top10ActiveJobFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_ACTIVE_JOB, zkAddr);
			if (top10ActiveJobFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_ACTIVE_JOB, zkAddr,
						top10ActiveJobJsonString);
				saturnStatisticsService.create(ss);
			} else {
				top10ActiveJobFromDB.setResult(top10ActiveJobJsonString);
				saturnStatisticsService.updateByPrimaryKey(top10ActiveJobFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateTop10LoadJob(List<JobStatistics> jobList, String zkAddr) {
		try {
			jobList = DashboardServiceHelper.sortJobByLoadLevel(jobList);
			List<JobStatistics> top10LoadJob = jobList.subList(0, jobList.size() > 9 ? 10 : jobList.size());
			String top10LoadJobJsonString = JSON.toJSONString(top10LoadJob);
			SaturnStatistics top10LoadJobFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_JOB, zkAddr);
			if (top10LoadJobFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_LOAD_JOB, zkAddr,
						top10LoadJobJsonString);
				saturnStatisticsService.create(ss);
			} else {
				top10LoadJobFromDB.setResult(top10LoadJobJsonString);
				saturnStatisticsService.updateByPrimaryKey(top10LoadJobFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateTop10LoadExecutor(List<ExecutorStatistics> executorList, String zkAddr) {
		try {
			executorList = DashboardServiceHelper.sortExecutorByLoadLevel(executorList);
			List<ExecutorStatistics> top10LoadExecutor = executorList.subList(0,
					executorList.size() > 9 ? 10 : executorList.size());
			String top10LoadExecutorJsonString = JSON.toJSONString(top10LoadExecutor);
			SaturnStatistics top10LoadExecutorFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_EXECUTOR, zkAddr);
			if (top10LoadExecutorFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_LOAD_EXECUTOR, zkAddr,
						top10LoadExecutorJsonString);
				saturnStatisticsService.create(ss);
			} else {
				top10LoadExecutorFromDB.setResult(top10LoadExecutorJsonString);
				saturnStatisticsService.updateByPrimaryKey(top10LoadExecutorFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateDomainProcessCount(ZkStatistics zks, String zkAddr) {
		try {
			String domainListJsonString = JSON.toJSONString(zks);
			SaturnStatistics domainProcessCountFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.DOMAIN_PROCESS_COUNT_OF_THE_DAY, zkAddr);
			if (domainProcessCountFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.DOMAIN_PROCESS_COUNT_OF_THE_DAY,
						zkAddr, domainListJsonString);
				saturnStatisticsService.create(ss);
			} else {
				domainProcessCountFromDB.setResult(domainListJsonString);
				saturnStatisticsService.updateByPrimaryKey(domainProcessCountFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateAbnormalJob(List<AbnormalJob> unnormalJobList, String zkAddr) {
		try {
			unnormalJobList = DashboardServiceHelper.sortUnnormaoJobByTimeDesc(unnormalJobList);
			SaturnStatistics unnormalJobFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zkAddr);
			if (unnormalJobFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.UNNORMAL_JOB, zkAddr,
						JSON.toJSONString(unnormalJobList));
				saturnStatisticsService.create(ss);
			} else {
				List<AbnormalJob> oldUnnormalJobList = JSON
						.parseArray(unnormalJobFromDB.getResult(), AbnormalJob.class);
				// 再次同步数据库中最新的read状态
				dealWithReadStatus(unnormalJobList, oldUnnormalJobList);
				unnormalJobFromDB.setResult(JSON.toJSONString(unnormalJobList));
				saturnStatisticsService.updateByPrimaryKey(unnormalJobFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void dealWithReadStatus(List<AbnormalJob> unnormalJobList, List<AbnormalJob> oldUnnormalJobList) {
		if (oldUnnormalJobList == null || oldUnnormalJobList.isEmpty()) {
			return;
		}
		for (AbnormalJob example : unnormalJobList) {
			AbnormalJob equalOld = DashboardServiceHelper.findEqualAbnormalJob(example, oldUnnormalJobList);
			if (equalOld != null) {
				example.setRead(equalOld.isRead());
			}
		}
	}

	private void saveOrUpdateTimeout4AlarmJob(List<Timeout4AlarmJob> timeout4AlarmJobList, String zkAddr) {
		try {
			String timeout4AlarmJobJsonString = JSON.toJSONString(timeout4AlarmJobList);
			SaturnStatistics timeout4AlarmJobFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, zkAddr);
			if (timeout4AlarmJobFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, zkAddr,
						timeout4AlarmJobJsonString);
				saturnStatisticsService.create(ss);
			} else {
				List<Timeout4AlarmJob> oldTimeout4AlarmJobs = JSON
						.parseArray(timeout4AlarmJobFromDB.getResult(), Timeout4AlarmJob.class);
				// 再次同步数据库中最新的read状态
				dealWithReadStatus4Timeout4AlarmJob(timeout4AlarmJobList, oldTimeout4AlarmJobs);
				timeout4AlarmJobFromDB.setResult(JSON.toJSONString(timeout4AlarmJobList));
				saturnStatisticsService.updateByPrimaryKey(timeout4AlarmJobFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void dealWithReadStatus4Timeout4AlarmJob(List<Timeout4AlarmJob> jobList,
			List<Timeout4AlarmJob> oldJobList) {
		if (oldJobList == null || oldJobList.isEmpty()) {
			return;
		}
		for (Timeout4AlarmJob job : jobList) {
			Timeout4AlarmJob oldJob = DashboardServiceHelper.findEqualTimeout4AlarmJob(job, oldJobList);
			if (oldJob != null) {
				job.setRead(oldJob.isRead());
			}
		}
	}

	private void saveOrUpdateUnableFailoverJob(List<AbnormalJob> unableFailoverJobList, String zkAddr) {
		try {
			String unableFailoverJobJsonString = JSON.toJSONString(unableFailoverJobList);
			SaturnStatistics unableFailoverJobFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB, zkAddr);
			if (unableFailoverJobFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB, zkAddr,
						unableFailoverJobJsonString);
				saturnStatisticsService.create(ss);
			} else {
				unableFailoverJobFromDB.setResult(unableFailoverJobJsonString);
				saturnStatisticsService.updateByPrimaryKey(unableFailoverJobFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateVersionDomainNumber(Map<String, Long> versionDomainNumber, String zkAddr) {
		try {
			String versionDomainNumberJsonString = JSON.toJSONString(versionDomainNumber);
			SaturnStatistics versionDomainNumberFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.VERSION_DOMAIN_NUMBER, zkAddr);
			if (versionDomainNumberFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.VERSION_DOMAIN_NUMBER, zkAddr,
						versionDomainNumberJsonString);
				saturnStatisticsService.create(ss);
			} else {
				versionDomainNumberFromDB.setResult(versionDomainNumberJsonString);
				saturnStatisticsService.updateByPrimaryKey(versionDomainNumberFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveOrUpdateVersionExecutorNumber(Map<String, Long> versionExecutorNumber, String zkAddr) {
		try {
			String versionExecutorNumberJsonString = JSON.toJSONString(versionExecutorNumber);
			SaturnStatistics versionExecutorNumberFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.VERSION_EXECUTOR_NUMBER, zkAddr);
			if (versionExecutorNumberFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.VERSION_EXECUTOR_NUMBER, zkAddr,
						versionExecutorNumberJsonString);
				saturnStatisticsService.create(ss);
			} else {
				versionExecutorNumberFromDB.setResult(versionExecutorNumberJsonString);
				saturnStatisticsService.updateByPrimaryKey(versionExecutorNumberFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
