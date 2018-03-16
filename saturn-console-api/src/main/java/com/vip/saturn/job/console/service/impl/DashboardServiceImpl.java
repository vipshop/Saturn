package com.vip.saturn.job.console.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.vip.saturn.job.console.domain.DomainStatistics;
import com.vip.saturn.job.console.domain.ExecutorStatistics;
import com.vip.saturn.job.console.domain.JobStatistics;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.domain.ZkStatistics;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.mybatis.service.SaturnStatisticsService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.DashboardService;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.StatisticsRefreshService;
import com.vip.saturn.job.console.service.helper.DashboardServiceHelper;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.ResetCountType;
import com.vip.saturn.job.console.utils.StatisticsTableKeyConstant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author chembo.huang
 */
public class DashboardServiceImpl implements DashboardService {

	private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

	@Autowired
	private SaturnStatisticsService saturnStatisticsService;

	@Autowired
	private RegistryCenterService registryCenterService;

	@Autowired
	private JobService jobService;

	@Autowired
	private StatisticsRefreshService statisticsRefreshService;

	private ExecutorService updateStatisticsThreadPool;

	@PostConstruct
	public void init() {
		initUpdateStatisticsThreadPool();
	}

	@PreDestroy
	public void destroy() {
		if (updateStatisticsThreadPool != null) {
			updateStatisticsThreadPool.shutdownNow();
		}
	}

	private void initUpdateStatisticsThreadPool() {
		updateStatisticsThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				String name = "single-update-statistics";
				Thread t = new Thread(r, name);
				if (t.isDaemon()) {
					t.setDaemon(false);
				}
				if (t.getPriority() != Thread.NORM_PRIORITY) {
					t.setPriority(Thread.NORM_PRIORITY);
				}
				return t;
			}
		});
	}

	@Override
	public int executorInDockerCount(String zkList) throws SaturnJobConsoleException {
		return getCountFromDB(StatisticsTableKeyConstant.EXECUTOR_IN_DOCKER_COUNT, zkList);
	}

	@Override
	public int executorNotInDockerCount(String zkList) throws SaturnJobConsoleException {
		return getCountFromDB(StatisticsTableKeyConstant.EXECUTOR_NOT_IN_DOCKER_COUNT, zkList);
	}

	@Override
	public int jobCount(String zkList) throws SaturnJobConsoleException {
		return getCountFromDB(StatisticsTableKeyConstant.JOB_COUNT, zkList);
	}

	private int getCountFromDB(String name, String zkList) {
		SaturnStatistics ss = saturnStatisticsService.findStatisticsByNameAndZkList(name, zkList);
		if (ss == null || StringUtils.isBlank(ss.getResult())) {
			return 0;
		}

		String result = ss.getResult();
		try {
			Integer count = JSON.parseObject(result, new TypeReference<Integer>() {
			});
			return count == null ? 0 : count;
		} catch (Exception e) {
			log.error("exception throws during get count from DB. name:" + name, e);
			return 0;
		}
	}

	@Override
	public SaturnStatistics top10FailureJob(String zklist) throws SaturnJobConsoleException {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_JOB,
				zklist);
	}

	@Override
	public String top10FailureJobByAllZkCluster() throws SaturnJobConsoleException {
		List<JobStatistics> jobStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = top10FailureJob(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<JobStatistics> tempList = JSON.parseArray(result, JobStatistics.class);
				if (tempList != null) {
					jobStatisticsList.addAll(tempList);
				}
			}
		}
		jobStatisticsList = DashboardServiceHelper.sortJobByAllTimeFailureRate(jobStatisticsList);
		List<JobStatistics> top10FailJob = jobStatisticsList.subList(0,
				jobStatisticsList.size() > 9 ? 10 : jobStatisticsList.size());
		return JSON.toJSONString(top10FailJob);
	}

	@Override
	public SaturnStatistics top10FailureExecutor(String zklist) throws SaturnJobConsoleException {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_EXECUTOR,
				zklist);
	}

	@Override
	public String top10FailureExecutorByAllZkCluster() throws SaturnJobConsoleException {
		List<ExecutorStatistics> executorStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = top10FailureExecutor(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<ExecutorStatistics> tempList = JSON.parseArray(result, ExecutorStatistics.class);
				if (tempList != null) {
					executorStatisticsList.addAll(tempList);
				}
			}
		}
		executorStatisticsList = DashboardServiceHelper.sortExecutorByFailureRate(executorStatisticsList);
		List<ExecutorStatistics> top10FailureExecutor = executorStatisticsList.subList(0,
				executorStatisticsList.size() > 9 ? 10 : executorStatisticsList.size());
		return JSON.toJSONString(top10FailureExecutor);
	}

	@Override
	public SaturnStatistics top10AactiveJob(String zklist) throws SaturnJobConsoleException {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_ACTIVE_JOB,
				zklist);
	}

	@Override
	public String top10AactiveJobByAllZkCluster() throws SaturnJobConsoleException {
		List<JobStatistics> jobStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = top10AactiveJob(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<JobStatistics> tempList = JSON.parseArray(result, JobStatistics.class);
				if (tempList != null) {
					jobStatisticsList.addAll(tempList);
				}
			}
		}
		jobStatisticsList = DashboardServiceHelper.sortJobByDayProcessCount(jobStatisticsList);
		List<JobStatistics> top10AactiveJob = jobStatisticsList.subList(0,
				jobStatisticsList.size() > 9 ? 10 : jobStatisticsList.size());
		return JSON.toJSONString(top10AactiveJob);
	}

	@Override
	public SaturnStatistics top10LoadExecutor(String zklist) throws SaturnJobConsoleException {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_EXECUTOR,
				zklist);
	}

	@Override
	public String top10LoadExecutorByAllZkCluster() throws SaturnJobConsoleException {
		List<ExecutorStatistics> executorStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = top10LoadExecutor(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<ExecutorStatistics> tempList = JSON.parseArray(result, ExecutorStatistics.class);
				if (tempList != null) {
					executorStatisticsList.addAll(tempList);
				}
			}
		}
		executorStatisticsList = DashboardServiceHelper.sortExecutorByLoadLevel(executorStatisticsList);
		List<ExecutorStatistics> top10LoadExecutor = executorStatisticsList.subList(0,
				executorStatisticsList.size() > 9 ? 10 : executorStatisticsList.size());
		return JSON.toJSONString(top10LoadExecutor);
	}

	@Override
	public SaturnStatistics top10LoadJob(String zklist) throws SaturnJobConsoleException {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_JOB,
				zklist);
	}

	@Override
	public String top10LoadJobByAllZkCluster() throws SaturnJobConsoleException {
		List<JobStatistics> jobStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = top10LoadJob(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<JobStatistics> tempList = JSON.parseArray(result, JobStatistics.class);
				if (tempList != null) {
					jobStatisticsList.addAll(tempList);
				}
			}
		}
		jobStatisticsList = DashboardServiceHelper.sortJobByLoadLevel(jobStatisticsList);
		List<JobStatistics> top10LoadJob = jobStatisticsList.subList(0,
				jobStatisticsList.size() > 9 ? 10 : jobStatisticsList.size());
		return JSON.toJSONString(top10LoadJob);
	}

	@Override
	public SaturnStatistics top10UnstableDomain(String zklist) throws SaturnJobConsoleException {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_UNSTABLE_DOMAIN,
				zklist);
	}

	@Override
	public String top10UnstableDomainByAllZkCluster() throws SaturnJobConsoleException {
		List<DomainStatistics> domainStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = top10UnstableDomain(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<DomainStatistics> tempList = JSON.parseArray(result, DomainStatistics.class);
				if (tempList != null) {
					domainStatisticsList.addAll(tempList);
				}
			}
		}
		domainStatisticsList = DashboardServiceHelper.sortDomainByShardingCount(domainStatisticsList);
		List<DomainStatistics> top10UnstableDomain = domainStatisticsList.subList(0,
				domainStatisticsList.size() > 9 ? 10 : domainStatisticsList.size());
		return JSON.toJSONString(top10UnstableDomain);
	}

	@Override
	public SaturnStatistics allProcessAndErrorCountOfTheDay(String zklist) throws SaturnJobConsoleException {
		return saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.DOMAIN_PROCESS_COUNT_OF_THE_DAY, zklist);
	}

	@Override
	public String allProcessAndErrorCountOfTheDayByAllZkCluster() throws SaturnJobConsoleException {
		int count = 0;
		int error = 0;
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = allProcessAndErrorCountOfTheDay(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				ZkStatistics temp = JSON.parseObject(result, ZkStatistics.class);
				if (temp != null) {
					count += temp.getCount();
					error += temp.getError();
				}
			}
		}
		return JSON.toJSONString(new ZkStatistics(count, error));
	}

	@Override
	public SaturnStatistics top10FailureDomain(String zklist) throws SaturnJobConsoleException {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_DOMAIN,
				zklist);
	}

	@Override
	public String top10FailureDomainByAllZkCluster() throws SaturnJobConsoleException {
		List<DomainStatistics> domainStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = top10FailureDomain(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<DomainStatistics> tempList = JSON.parseArray(result, DomainStatistics.class);
				if (tempList != null) {
					domainStatisticsList.addAll(tempList);
				}
			}
		}
		domainStatisticsList = DashboardServiceHelper.sortDomainByAllTimeFailureRate(domainStatisticsList);
		List<DomainStatistics> top10FailureDomain = domainStatisticsList.subList(0,
				domainStatisticsList.size() > 9 ? 10 : domainStatisticsList.size());
		return JSON.toJSONString(top10FailureDomain);
	}

	@Override
	public void cleanShardingCount(String namespace) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		curatorFrameworkOp.update(ExecutorNodePath.SHARDING_COUNT_PATH, "0");
		asyncForceRefreshStatistics(namespace);
	}

	@Override
	public void cleanOneJobAnalyse(String namespace, String jobName) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		updateResetValue(curatorFrameworkOp, jobName, ResetCountType.RESET_ANALYSE);
		resetOneJobAnalyse(jobName, curatorFrameworkOp);
		asyncForceRefreshStatistics(namespace);
	}

	@Override
	public void cleanAllJobAnalyse(String namespace) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		List<String> jobs = jobService.getUnSystemJobNames(namespace);
		for (String job : jobs) {
			resetOneJobAnalyse(job, curatorFrameworkOp);
			updateResetValue(curatorFrameworkOp, job, ResetCountType.RESET_ANALYSE);
		}
		asyncForceRefreshStatistics(namespace);
	}

	@Override
	public void cleanAllJobExecutorCount(String namespace) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		List<String> jobs = jobService.getUnSystemJobNames(namespace);
		for (String job : jobs) {
			resetOneJobExecutorCount(job, curatorFrameworkOp);
			updateResetValue(curatorFrameworkOp, job, ResetCountType.RESET_SERVERS);
		}
		asyncForceRefreshStatistics(namespace);
	}

	@Override
	public void cleanOneJobExecutorCount(String namespace, String jobName) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		updateResetValue(curatorFrameworkOp, jobName, ResetCountType.RESET_SERVERS);
		resetOneJobExecutorCount(jobName, curatorFrameworkOp);
		asyncForceRefreshStatistics(namespace);
	}

	private void resetOneJobExecutorCount(String jobName, CuratorFrameworkOp curatorFrameworkOp) {
		if (curatorFrameworkOp.checkExists(JobNodePath.getServerNodePath(jobName))) {
			List<String> servers = curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName));
			for (String server : servers) {
				curatorFrameworkOp.update(JobNodePath.getProcessSucessCount(jobName, server), "0");
				curatorFrameworkOp.update(JobNodePath.getProcessFailureCount(jobName, server), "0");
			}
		}
	}

	private void resetOneJobAnalyse(String jobName, CuratorFrameworkOp curatorFrameworkOp) {
		curatorFrameworkOp.update(JobNodePath.getProcessCountPath(jobName), "0");
		curatorFrameworkOp.update(JobNodePath.getErrorCountPath(jobName), "0");
	}

	private void updateResetValue(CuratorFrameworkOp curatorFrameworkOp, String job, String value) {
		String path = JobNodePath.getAnalyseResetPath(job);
		curatorFrameworkOp.update(path, value.getBytes());
	}

	private void asyncForceRefreshStatistics(final String namespace) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					RegistryCenterConfiguration conf = registryCenterService.findConfigByNamespace(namespace);
					if (conf != null) {
						statisticsRefreshService.refresh(conf.getZkClusterKey(), false);
					}
				} catch (Throwable t) {
					log.error("async refresh statistics error", t);
				}
			}
		};
		updateStatisticsThreadPool.submit(runnable);
	}

	@Override
	public Map<String, Integer> loadDomainRankDistribution(String zkClusterKey) throws SaturnJobConsoleException {
		Map<String, Integer> domainMap = new HashMap<>();
		if (zkClusterKey != null) {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null && !zkCluster.isOffline()) {
				for (RegistryCenterConfiguration config : zkCluster.getRegCenterConfList()) {
					Integer count = domainMap.get(config.getDegree());
					if (null != config.getDegree()) {
						domainMap.put(config.getDegree(), count == null ? 1 : count + 1);
					}
				}
			}
		}
		return domainMap;
	}

	@Override
	public Map<String, Integer> loadDomainRankDistributionByAllZkCluster() throws SaturnJobConsoleException {
		Map<String, Integer> domainMap = new HashMap<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			ArrayList<RegistryCenterConfiguration> regCenterConfList = zkCluster.getRegCenterConfList();
			if (regCenterConfList != null) {
				for (RegistryCenterConfiguration config : regCenterConfList) {
					String degree = config.getDegree();
					if (degree != null) {
						Integer count = domainMap.get(degree);
						domainMap.put(degree, count == null ? 1 : count + 1);
					}
				}
			}
		}
		return domainMap;
	}

	@Override
	public Map<Integer, Integer> loadJobRankDistribution(String zklist) throws SaturnJobConsoleException {
		SaturnStatistics ss = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.JOB_RANK_DISTRIBUTION, zklist);
		if (ss != null) {
			String result = ss.getResult();
			return JSON.parseObject(result, new TypeReference<Map<Integer, Integer>>() {
			});
		} else {
			return new HashMap<>();
		}
	}

	@Override
	public Map<Integer, Integer> loadJobRankDistributionByAllZkCluster() throws SaturnJobConsoleException {
		Map<Integer, Integer> jobDegreeCountMap = new HashMap<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String zkAddr = zkCluster.getZkAddr();
			Map<Integer, Integer> temp = loadJobRankDistribution(zkAddr);
			if (temp == null) {
				continue;
			}
			Iterator<Entry<Integer, Integer>> iterator = temp.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<Integer, Integer> next = iterator.next();
				Integer jobDegree = next.getKey();
				Integer count = next.getValue();
				if (jobDegree != null && count != null) {
					if (jobDegreeCountMap.containsKey(jobDegree)) {
						jobDegreeCountMap.put(jobDegree, jobDegreeCountMap.get(jobDegree) + count);
					} else {
						jobDegreeCountMap.put(jobDegree, count);
					}
				}
			}
		}
		return jobDegreeCountMap;
	}

	@Override
	public Map<String, Long> versionDomainNumber(String currentZkAddr) throws SaturnJobConsoleException {
		SaturnStatistics ss = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.VERSION_DOMAIN_NUMBER, currentZkAddr);
		if (ss != null) {
			String result = ss.getResult();
			return JSON.parseObject(result, new TypeReference<Map<String, Long>>() {
			});
		} else {
			return new HashMap<>();
		}
	}

	@Override
	public Map<String, Long> versionDomainNumberByAllZkCluster() throws SaturnJobConsoleException {
		Map<String, Long> versionDomainNumberMap = new HashMap<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String zkAddr = zkCluster.getZkAddr();
			Map<String, Long> temp = versionDomainNumber(zkAddr);
			if (temp == null) {
				continue;
			}
			Iterator<Entry<String, Long>> iterator = temp.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, Long> next = iterator.next();
				String version = next.getKey();
				Long domainNumber = next.getValue();
				if (version != null && domainNumber != null) {
					if (versionDomainNumberMap.containsKey(version)) {
						versionDomainNumberMap.put(version, versionDomainNumberMap.get(version) + domainNumber);
					} else {
						versionDomainNumberMap.put(version, domainNumber);
					}
				}
			}
		}
		return versionDomainNumberMap;
	}

	@Override
	public Map<String, Long> versionExecutorNumber(String currentZkAddr) throws SaturnJobConsoleException {
		SaturnStatistics ss = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.VERSION_EXECUTOR_NUMBER, currentZkAddr);
		if (ss != null) {
			String result = ss.getResult();
			return JSON.parseObject(result, new TypeReference<Map<String, Long>>() {
			});
		} else {
			return new HashMap<>();
		}
	}

	@Override
	public Map<String, Long> versionExecutorNumberByAllZkCluster() throws SaturnJobConsoleException {
		Map<String, Long> versionExecutorNumberMap = new HashMap<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String zkAddr = zkCluster.getZkAddr();
			Map<String, Long> temp = versionExecutorNumber(zkAddr);
			if (temp == null) {
				continue;
			}
			Iterator<Entry<String, Long>> iterator = temp.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, Long> next = iterator.next();
				String version = next.getKey();
				Long executorNumber = next.getValue();
				if (version != null && executorNumber != null) {
					if (versionExecutorNumberMap.containsKey(version)) {
						versionExecutorNumberMap.put(version,
								versionExecutorNumberMap.get(version) + executorNumber);
					} else {
						versionExecutorNumberMap.put(version, executorNumber);
					}
				}
			}
		}
		return versionExecutorNumberMap;
	}

}
