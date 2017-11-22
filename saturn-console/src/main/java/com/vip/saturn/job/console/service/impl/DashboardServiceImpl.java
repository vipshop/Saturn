package com.vip.saturn.job.console.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.domain.JobBriefInfo.JobType;
import com.vip.saturn.job.console.domain.container.ContainerConfig;
import com.vip.saturn.job.console.domain.container.ContainerScaleJob;
import com.vip.saturn.job.console.domain.ExecutorProvided;
import com.vip.saturn.job.console.exception.JobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.mybatis.service.SaturnStatisticsService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.repository.zookeeper.impl.CuratorRepositoryImpl;
import com.vip.saturn.job.console.service.ContainerService;
import com.vip.saturn.job.console.service.DashboardService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.helper.DashboardServiceHelper;
import com.vip.saturn.job.console.utils.*;

import com.vip.saturn.job.integrate.service.ReportAlarmService;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author chembo.huang
 *
 */
@Service
public class DashboardServiceImpl implements DashboardService {

	private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

	private static int REFRESH_INTERVAL_IN_MINUTE = 7;

	private static long ALLOW_DELAY_MILLIONSECONDS = 60L * 1000L * REFRESH_INTERVAL_IN_MINUTE;

	private static final long ALLOW_CONTAINER_DELAY_MILLIONSECONDS = 60L * 1000L * 3;

	private static final long INTERVAL_DELTA_IN_SECOND = 10 * 1000L;

	static {
		String refreshInterval = System.getProperty("VIP_SATURN_DASHBOARD_REFRESH_INTERVAL_MINUTE",
				System.getenv("VIP_SATURN_DASHBOARD_REFRESH_INTERVAL_MINUTE"));
		if (refreshInterval != null) {
			try {
				REFRESH_INTERVAL_IN_MINUTE = Integer.valueOf(refreshInterval);
				ALLOW_DELAY_MILLIONSECONDS = 60 * 1000 * REFRESH_INTERVAL_IN_MINUTE;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private Map<String/** domainName_jobName_shardingItemStr **/
			, AbnormalShardingState/** abnormal sharding state */
	> abnormalShardingStateCache = new ConcurrentHashMap<>();

	private Timer refreshStatisticsTimmer;
	private Timer cleanAbnormalShardingCacheTimer;
	private ExecutorService updateStatisticsThreadPool;

	@Autowired
	private SaturnStatisticsService saturnStatisticsService;

	@Autowired
	private RegistryCenterService registryCenterService;

	@Autowired
	private JobDimensionService jobDimensionService;

	@Autowired
	private CuratorRepository curatorRepository;

	@Autowired
	private ReportAlarmService reportAlarmService;

	@Autowired
	private ContainerService containerService;

	@PostConstruct
	public void init() throws Exception {
		initUpdateStatisticsThreadPool();
		startRefreshStatisticsTimmer();
		startCleanAbnormalShardingCacheTimer();
	}

	@PreDestroy
	public void destroy() {
		if (updateStatisticsThreadPool != null) {
			updateStatisticsThreadPool.shutdownNow();
		}
		if (refreshStatisticsTimmer != null) {
			refreshStatisticsTimmer.cancel();
		}
		if (cleanAbnormalShardingCacheTimer != null) {
			cleanAbnormalShardingCacheTimer.cancel();
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

	private void startRefreshStatisticsTimmer() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				try {
					refreshStatistics2DB(false);
				} catch (Throwable t) {
					log.error("refresh statistics error", t);
				}
			}
		};
		refreshStatisticsTimmer = new Timer("refresh-statistics-to-db-timer", true);
		refreshStatisticsTimmer.scheduleAtFixedRate(timerTask, 1000 * 15, 1000 * 60 * REFRESH_INTERVAL_IN_MINUTE);
	}

	private void startCleanAbnormalShardingCacheTimer() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				try {
					for (Entry<String, AbnormalShardingState> entrySet : abnormalShardingStateCache.entrySet()) {
						AbnormalShardingState shardingState = entrySet.getValue();
						if (shardingState.getAlertTime() + ALLOW_DELAY_MILLIONSECONDS * 2 < System.currentTimeMillis()) {
							abnormalShardingStateCache.remove(entrySet.getKey());
							log.info(
									"Clean abnormalShardingCache with key: {}, alertTime: {}, zkNodeCVersion: {}: "
											+ entrySet.getKey(),
									shardingState.getAlertTime(), shardingState.getZkNodeCVersion());
						}
					}
				} catch (Throwable t) {
					log.error("Clean abnormalShardingCache error", t);
				}
			}
		};
		cleanAbnormalShardingCacheTimer = new Timer("clean-abnormalShardingCache-timer", true);
		cleanAbnormalShardingCacheTimer.scheduleAtFixedRate(timerTask, 0, ALLOW_DELAY_MILLIONSECONDS);
	}

	@Override
	public synchronized void refreshStatistics2DB(boolean force) {
		log.info("start refresh statistics");
		Date start = new Date();
		try {
			Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
			if (zkClusterList != null) {
				for (ZkCluster zkCluster : zkClusterList) {
					if (force || registryCenterService.isDashboardLeader(zkCluster.getZkClusterKey())) {
						refreshStatistics2DB(zkCluster);
					}
				}
			}
		} catch (Throwable t) {
			log.error("refresh statistics error", t);
		}
		log.info("end refresh statistics, takes " + (new Date().getTime() - start.getTime()));
	}
	
	@Override
	public synchronized void refreshStatistics2DB(String zkClusterKey) throws SaturnJobConsoleException {
		log.info("start refresh statistics by zkClusterKey:" + zkClusterKey);
		Date start = new Date();
		ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
		if (zkCluster != null) {
			refreshStatistics2DB(zkCluster);
		} else {
			throw new SaturnJobConsoleException("zk cluster not found by zkClusterKey:" + zkClusterKey);
		}
		log.info("end refresh statistics by zkClusterKey, takes " + (new Date().getTime() - start.getTime()));
	}

	private void refreshStatistics2DB(ZkCluster zkCluster) {
		HashMap<String/** {jobname}-{domain} */
				, JobStatistics> jobMap = new HashMap<>();
		HashMap<String/** {executorName}-{domain} */
				, ExecutorStatistics> executorMap = new HashMap<>();
		List<JobStatistics> jobList = new ArrayList<>();
		List<ExecutorStatistics> executorList = new ArrayList<>();
		List<AbnormalJob> unnormalJobList = new ArrayList<>();
		List<AbnormalJob> unableFailoverJobList = new ArrayList<>();
		List<Timeout4AlarmJob> timeout4AlarmJobList = new ArrayList<>();
		List<DomainStatistics> domainList = new ArrayList<>();
		List<AbnormalContainer> abnormalContainerList = new ArrayList<>();
		Map<String, Long> versionDomainNumber = new HashMap<>(); // 不同版本的域数量
		Map<String, Long> versionExecutorNumber = new HashMap<>(); // 不同版本的executor数量
		int exeInDocker = 0;
		int exeNotInDocker = 0;
		int totalCount = 0;
		int errorCount = 0;
		for (RegistryCenterConfiguration config : zkCluster.getRegCenterConfList()) {
			// 过滤非当前zk连接
			if (zkCluster.getZkAddr().equals(config.getZkAddressList())) {
				int processCountOfThisDomainAllTime = 0;
				int errorCountOfThisDomainAllTime = 0;
				int processCountOfThisDomainThisDay = 0;
				int errorCountOfThisDomainThisDay = 0;

				DomainStatistics domain = new DomainStatistics(config.getNamespace(), zkCluster.getZkAddr(),
						config.getNameAndNamespace());

				RegistryCenterClient registryCenterClient = registryCenterService.connect(config.getNameAndNamespace());
				try {
					if (registryCenterClient != null && registryCenterClient.isConnected()) {
						CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
						CuratorFrameworkOp curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorClient);
						// 统计稳定性
						if (checkExists(curatorClient, ExecutorNodePath.SHARDING_COUNT_PATH)) {
							String countStr = getData(curatorClient, ExecutorNodePath.SHARDING_COUNT_PATH);
							domain.setShardingCount(Integer.valueOf(countStr));
						}
						String version = null; // 该域的版本号
						long executorNumber = 0L; // 该域的在线executor数量
						// 统计物理容器资源，统计版本数据
						if (null != curatorClient.checkExists().forPath(ExecutorNodePath.getExecutorNodePath())) {
							List<String> executors = curatorClient.getChildren()
									.forPath(ExecutorNodePath.getExecutorNodePath());
							if (executors != null) {
								for (String exe : executors) {
									// 在线的才统计
									if (null != curatorClient.checkExists()
											.forPath(ExecutorNodePath.getExecutorIpNodePath(exe))) {
										// 统计是物理机还是容器
										String executorMapKey = exe + "-" + config.getNamespace();
										ExecutorStatistics executorStatistics = executorMap.get(executorMapKey);
										if (executorStatistics == null) {
											executorStatistics = new ExecutorStatistics(exe, config.getNamespace());
											executorStatistics.setNns(domain.getNns());
											executorStatistics.setIp(getData(curatorClient,
													ExecutorNodePath.getExecutorIpNodePath(exe)));
											executorMap.put(executorMapKey, executorStatistics);
										}
										// set runInDocker field
										if (checkExists(curatorClient,
												ExecutorNodePath.get$ExecutorTaskNodePath(exe))) {
											executorStatistics.setRunInDocker(true);
											exeInDocker++;
										} else {
											exeNotInDocker++;
										}
									}
									// 获取版本号
									if (version == null) {
										version = getData(curatorClient,
												ExecutorNodePath.getExecutorVersionNodePath(exe));
									}
								}
								executorNumber = executors.size();
							}
						}
						// 统计版本数据
						if (version == null) { // 未知版本
							version = "-1";
						}
						if (versionDomainNumber.containsKey(version)) {
							Long domainNumber = versionDomainNumber.get(version);
							versionDomainNumber.put(version, domainNumber + 1);
						} else {
							versionDomainNumber.put(version, 1L);
						}
						if (versionExecutorNumber.containsKey(version)) {
							Long executorNumber0 = versionExecutorNumber.get(version);
							versionExecutorNumber.put(version, executorNumber0 + executorNumber);
						} else {
							if (executorNumber != 0) {
								versionExecutorNumber.put(version, executorNumber);
							}
						}

						// 遍历所有$Jobs子节点，非系统作业
						List<String> jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
						SaturnStatistics saturnStatistics = saturnStatisticsService.findStatisticsByNameAndZkList(
								StatisticsTableKeyConstant.UNNORMAL_JOB, zkCluster.getZkAddr());
						List<AbnormalJob> oldAbnormalJobs = new ArrayList<>();
						if (saturnStatistics != null) {
							String result = saturnStatistics.getResult();
							if (StringUtils.isNotBlank(result)) {
								oldAbnormalJobs = JSON.parseArray(result, AbnormalJob.class);
							}
						}
						saturnStatistics = saturnStatisticsService.findStatisticsByNameAndZkList(
								StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, zkCluster.getZkAddr());
						List<Timeout4AlarmJob> oldTimeout4AlarmJobs = new ArrayList<>();
						if (saturnStatistics != null) {
							String result = saturnStatistics.getResult();
							if (StringUtils.isNotBlank(result)) {
								oldTimeout4AlarmJobs = JSON.parseArray(result, Timeout4AlarmJob.class);
							}
						}
						for (String job : jobs) {
							try {
								Boolean localMode = Boolean.valueOf(
										getData(curatorClient, JobNodePath.getConfigNodePath(job, "localMode")));
								String jobDomainKey = job + "-" + config.getNamespace();
								JobStatistics jobStatistics = jobMap.get(jobDomainKey);
								if (jobStatistics == null) {
									jobStatistics = new JobStatistics(job, config.getNamespace(),
											config.getNameAndNamespace());
									jobMap.put(jobDomainKey, jobStatistics);
								}

								String jobDegree = getData(curatorClient,
										JobNodePath.getConfigNodePath(job, "jobDegree"));
								if (Strings.isNullOrEmpty(jobDegree)) {
									jobDegree = "0";
								}
								jobStatistics.setJobDegree(Integer.parseInt(jobDegree));

								// 非本地作业才参与判断
								if (!localMode) {
									AbnormalJob unnormalJob = new AbnormalJob(job, config.getNamespace(),
											config.getNameAndNamespace(), config.getDegree());
									checkJavaOrShellJobHasProblem(oldAbnormalJobs, curatorClient, unnormalJob,
											jobDegree, unnormalJobList);
								}

								// 查找超时告警作业
								Timeout4AlarmJob timeout4AlarmJob = new Timeout4AlarmJob(job, config.getNamespace(),
										config.getNameAndNamespace(), config.getDegree());
								if (isTimeout4AlarmJob(oldTimeout4AlarmJobs, timeout4AlarmJob, curatorFrameworkOp) != null) {
									timeout4AlarmJob.setJobDegree(jobDegree);
									timeout4AlarmJobList.add(timeout4AlarmJob);
								}

								// 查找无法高可用的作业
								AbnormalJob unableFailoverJob = new AbnormalJob(job, config.getNamespace(),
										config.getNameAndNamespace(), config.getDegree());
								if (isUnableFailoverJob(curatorClient, unableFailoverJob, curatorFrameworkOp) != null) {
									unableFailoverJob.setJobDegree(jobDegree);
									unableFailoverJobList.add(unableFailoverJob);
								}

								String processCountOfThisJobAllTimeStr = getData(curatorClient,
										JobNodePath.getProcessCountPath(job));
								String errorCountOfThisJobAllTimeStr = getData(curatorClient,
										JobNodePath.getErrorCountPath(job));
								int processCountOfThisJobAllTime = processCountOfThisJobAllTimeStr == null ? 0
										: Integer.valueOf(processCountOfThisJobAllTimeStr);
								int errorCountOfThisJobAllTime = processCountOfThisJobAllTimeStr == null ? 0
										: Integer.valueOf(errorCountOfThisJobAllTimeStr);
								processCountOfThisDomainAllTime += processCountOfThisJobAllTime;
								errorCountOfThisDomainAllTime += errorCountOfThisJobAllTime;
								int processCountOfThisJobThisDay = 0;
								int errorCountOfThisJobThisDay = 0;

								// loadLevel of this job
								int loadLevel = Integer.parseInt(
										getData(curatorClient, JobNodePath.getConfigNodePath(job, "loadLevel")));
								int shardingTotalCount = Integer.parseInt(getData(curatorClient,
										JobNodePath.getConfigNodePath(job, "shardingTotalCount")));
								List<String> servers = null;
								if (null != curatorClient.checkExists().forPath(JobNodePath.getServerNodePath(job))) {
									servers = curatorClient.getChildren().forPath(JobNodePath.getServerNodePath(job));
									for (String server : servers) {
										// 如果结点存活，算两样东西：1.遍历所有servers节点里面的processSuccessCount &
										// processFailureCount，用以统计作业每天的执行次数；2.统计executor的loadLevel;，
										if (checkExists(curatorClient, JobNodePath.getServerStatus(job, server))) {
											// 1.遍历所有servers节点里面的processSuccessCount &
											// processFailureCount，用以统计作业每天的执行次数；
											try {
												String processSuccessCountOfThisExeStr = getData(curatorClient,
														JobNodePath.getProcessSucessCount(job, server));
												String processFailureCountOfThisExeStr = getData(curatorClient,
														JobNodePath.getProcessFailureCount(job, server));
												int processSuccessCountOfThisExe = processSuccessCountOfThisExeStr == null
														? 0 : Integer.valueOf(processSuccessCountOfThisExeStr);
												int processFailureCountOfThisExe = processFailureCountOfThisExeStr == null
														? 0 : Integer.valueOf(processFailureCountOfThisExeStr);
												// 该作业当天运行统计
												processCountOfThisJobThisDay += processSuccessCountOfThisExe
														+ processFailureCountOfThisExe;
												errorCountOfThisJobThisDay += processFailureCountOfThisExe;

												// 全部域当天的成功数与失败数
												totalCount += processSuccessCountOfThisExe
														+ processFailureCountOfThisExe;
												errorCount += processFailureCountOfThisExe;

												// 全域当天运行统计
												processCountOfThisDomainThisDay += processCountOfThisJobThisDay;
												errorCountOfThisDomainThisDay += errorCountOfThisJobThisDay;

												// executor当天运行成功失败数
												String executorMapKey = server + "-" + config.getNamespace();
												ExecutorStatistics executorStatistics = executorMap.get(executorMapKey);
												if (executorStatistics == null) {
													executorStatistics = new ExecutorStatistics(server,
															config.getNamespace());
													executorStatistics.setNns(domain.getNns());
													executorStatistics.setIp(getData(curatorClient,
															ExecutorNodePath.getExecutorIpNodePath(server)));
													executorMap.put(executorMapKey, executorStatistics);
												}
												executorStatistics.setFailureCountOfTheDay(
														executorStatistics.getFailureCountOfTheDay()
																+ processFailureCountOfThisExe);
												executorStatistics.setProcessCountOfTheDay(
														executorStatistics.getProcessCountOfTheDay()
																+ processSuccessCountOfThisExe
																+ processFailureCountOfThisExe);

											} catch (Exception e) {
												log.info(e.getMessage());
											}

											// 2.统计executor的loadLevel;
											try {
												// enabled 的作业才需要计算权重
												if (Boolean.valueOf(getData(curatorClient,
														JobNodePath.getConfigNodePath(job, "enabled")))) {
													String sharding = getData(curatorClient,
															JobNodePath.getServerSharding(job, server));
													if (StringUtils.isNotEmpty(sharding)) {
														// 更新job的executorsAndshards
														String exesAndShards = (jobStatistics
																.getExecutorsAndShards() == null ? ""
																		: jobStatistics.getExecutorsAndShards())
																+ server + ":" + sharding + "; ";
														jobStatistics.setExecutorsAndShards(exesAndShards);
														// 2.统计是物理机还是容器
														String executorMapKey = server + "-" + config.getNamespace();
														ExecutorStatistics executorStatistics = executorMap
																.get(executorMapKey);
														if (executorStatistics == null) {
															executorStatistics = new ExecutorStatistics(server,
																	config.getNamespace());
															executorStatistics.setNns(domain.getNns());
															executorStatistics.setIp(getData(curatorClient,
																	ExecutorNodePath.getExecutorIpNodePath(server)));
															executorMap.put(executorMapKey, executorStatistics);
															// set runInDocker field
															if (checkExists(curatorClient, ExecutorNodePath
																	.get$ExecutorTaskNodePath(server))) {
																executorStatistics.setRunInDocker(true);
																exeInDocker++;
															} else {
																exeNotInDocker++;
															}
														}
														if (executorStatistics.getJobAndShardings() != null) {
															executorStatistics.setJobAndShardings(
																	executorStatistics.getJobAndShardings() + job + ":"
																			+ sharding + ";");
														} else {
															executorStatistics
																	.setJobAndShardings(job + ":" + sharding + ";");
														}
														int newLoad = executorStatistics.getLoadLevel()
																+ (loadLevel * sharding.split(",").length);
														executorStatistics.setLoadLevel(newLoad);
													}
												}
											} catch (Exception e) {
												log.info(e.getMessage());
											}
										}
									}
								}
								// local-mode job = server count(regardless server status)
								if (localMode) {
									jobStatistics.setTotalLoadLevel(servers == null ? 0 : (servers.size() * loadLevel));
								} else {
									jobStatistics.setTotalLoadLevel(loadLevel * shardingTotalCount);
								}
								jobStatistics.setErrorCountOfAllTime(errorCountOfThisJobAllTime);
								jobStatistics.setProcessCountOfAllTime(processCountOfThisJobAllTime);
								jobStatistics.setFailureCountOfTheDay(errorCountOfThisJobThisDay);
								jobStatistics.setProcessCountOfTheDay(processCountOfThisJobThisDay);
								jobMap.put(jobDomainKey, jobStatistics);
							} catch (Exception e) {
								log.info("statistics namespace:{} ,jobName:{} ,exception:{}", domain.getNns(), job,
										e.getMessage());
							}
						}

						// 遍历容器资源，获取异常资源
						String dcosTasksNodePath = ContainerNodePath.getDcosTasksNodePath();
						List<String> tasks = curatorFrameworkOp.getChildren(dcosTasksNodePath);
						if (tasks != null && !tasks.isEmpty()) {
							for (String taskId : tasks) {
								AbnormalContainer abnormalContainer = new AbnormalContainer(taskId,
										config.getNamespace(), config.getNameAndNamespace(), config.getDegree());
								if (isContainerInstanceMismatch(abnormalContainer, curatorFrameworkOp) != null) {
									abnormalContainerList.add(abnormalContainer);
								}
							}
						}
					}
				} catch (Exception e) {
					log.info("refreshStatistics2DB namespace:{} ,exception:{}", domain.getNns(), e.getMessage());
				}
				domain.setErrorCountOfAllTime(errorCountOfThisDomainAllTime);
				domain.setProcessCountOfAllTime(processCountOfThisDomainAllTime);
				domain.setErrorCountOfTheDay(errorCountOfThisDomainThisDay);
				domain.setProcessCountOfTheDay(processCountOfThisDomainThisDay);
				domainList.add(domain);
			}
		}

		jobList.addAll(jobMap.values());

		executorList.addAll(executorMap.values());

		// 全域当天处理总数，失败总数
		saveOrUpdateDomainProcessCount(new ZkStatistics(totalCount, errorCount), zkCluster.getZkAddr());

		// 失败率Top10的域列表
		saveOrUpdateTop10FailDomain(domainList, zkCluster.getZkAddr());

		// 稳定性最差的Top10的域列表
		saveOrUpdateTop10UnstableDomain(domainList, zkCluster.getZkAddr());

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
		saveOrUpdateAbnormalJob(unnormalJobList, zkCluster.getZkAddr());

		// 超时告警的作业列表
		saveOrUpdateTimeout4AlarmJob(timeout4AlarmJobList, zkCluster.getZkAddr());

		// 无法高可用的作业列表
		saveOrUpdateUnableFailoverJob(unableFailoverJobList, zkCluster.getZkAddr());

		// 异常容器资源列表，包含实例数不匹配的资源列表
		saveOrUpdateAbnormalContainer(abnormalContainerList, zkCluster.getZkAddr());

		// 不同版本的域数量
		saveOrUpdateVersionDomainNumber(versionDomainNumber, zkCluster.getZkAddr());

		// 不同版本的executor数量
		saveOrUpdateVersionExecutorNumber(versionExecutorNumber, zkCluster.getZkAddr());

		// 不同作业等级的作业数量
		saveOrUpdateJobRankDistribution(jobList, zkCluster.getZkAddr());

		// 容器executor数量
		saveOrUpdateExecutorInDockerCount(exeInDocker, zkCluster.getZkAddr());

		// 物理机executor数量
		saveOrUpdateExecutorNotInDockerCount(exeNotInDocker, zkCluster.getZkAddr());

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

	private void saveOrUpdateJobRankDistribution(List<JobStatistics> jobList, String zkAddr) {
		try {
			Map<Integer, Integer> jobDegreeCountMap = new HashMap<>();
			for (JobStatistics jobStatistics : jobList) {
				int jobDegree = jobStatistics.getJobDegree();
				Integer count = jobDegreeCountMap.get(jobDegree);
				jobDegreeCountMap.put(jobDegree, count == null ? 1 : count + 1);
			}
			String jobDegreeMapString = JSON.toJSONString(jobDegreeCountMap);
			SaturnStatistics jobDegreeMapFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.JOB_RANK_DISTRIBUTION, zkAddr);
			if (jobDegreeMapFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.JOB_RANK_DISTRIBUTION, zkAddr,
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
				List<AbnormalJob> oldUnnormalJobList = JSON.parseArray(unnormalJobFromDB.getResult(), AbnormalJob.class);
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
			AbnormalJob equalOld = findEqualAbnormalJob(example, oldUnnormalJobList);
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
				List<Timeout4AlarmJob> oldTimeout4AlarmJobs = JSON.parseArray(timeout4AlarmJobFromDB.getResult(), Timeout4AlarmJob.class);
				dealWithReadStatus4Timeout4AlarmJob(timeout4AlarmJobList, oldTimeout4AlarmJobs);
				timeout4AlarmJobFromDB.setResult(JSON.toJSONString(timeout4AlarmJobList));
				saturnStatisticsService.updateByPrimaryKey(timeout4AlarmJobFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void dealWithReadStatus4Timeout4AlarmJob(List<Timeout4AlarmJob> jobList, List<Timeout4AlarmJob> oldJobList) {
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

	private void saveOrUpdateAbnormalContainer(List<AbnormalContainer> abnormalContainerList, String zkAddr) {
		try {
			String abnormalContainerJsonString = JSON.toJSONString(abnormalContainerList);
			SaturnStatistics abnormalContainerFromDB = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.ABNORMAL_CONTAINER, zkAddr);
			if (abnormalContainerFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.ABNORMAL_CONTAINER, zkAddr,
						abnormalContainerJsonString);
				saturnStatisticsService.create(ss);
			} else {
				abnormalContainerFromDB.setResult(abnormalContainerJsonString);
				saturnStatisticsService.updateByPrimaryKey(abnormalContainerFromDB);
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

	private void fillAbnormalJob(CuratorFramework curatorClient, AbnormalJob abnormalJob, String cause, String timeZone,
			long nextFireTimeExcludePausePeriod) throws Exception {
		boolean areNotReady = true;
		String serverNodePath = JobNodePath.getServerNodePath(abnormalJob.getJobName());
		if (checkExists(curatorClient, serverNodePath)) {
			List<String> servers = curatorClient.getChildren().forPath(serverNodePath);
			if (servers != null && !servers.isEmpty()) {
				for (String server : servers) {
					if (checkExists(curatorClient, JobNodePath.getServerStatus(abnormalJob.getJobName(), server))) {
						areNotReady = false;
						break;
					}
				}
			}
		}
		if (areNotReady) {
			cause = AbnormalJob.Cause.EXECUTORS_NOT_READY.name();
		}
		abnormalJob.setCause(cause);
		abnormalJob.setTimeZone(timeZone);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
		abnormalJob.setNextFireTimeWithTimeZoneFormat(sdf.format(nextFireTimeExcludePausePeriod));
		abnormalJob.setNextFireTime(nextFireTimeExcludePausePeriod);
	}

	private AbnormalJob findEqualAbnormalJob(AbnormalJob example, List<AbnormalJob> oldUnnormalJobList) {
		if (oldUnnormalJobList == null || oldUnnormalJobList.isEmpty()) {
			return null;
		}
		for (AbnormalJob oldUnnormalJob : oldUnnormalJobList) {
			if (oldUnnormalJob.equals(example)) {
				return oldUnnormalJob;
			}
		}
		return null;
	}

	private void registerAbnormalJobIfNecessary(List<AbnormalJob> oldAbnormalJobs, AbnormalJob abnormalJob,
			String timeZone, Long nextFireTime) {
		AbnormalJob oldAbnormalJob = findEqualAbnormalJob(abnormalJob, oldAbnormalJobs);
		if (oldAbnormalJob != null) {
			abnormalJob.setRead(oldAbnormalJob.isRead());
			if (oldAbnormalJob.getUuid() != null) {
				abnormalJob.setUuid(oldAbnormalJob.getUuid());
			} else {
				abnormalJob.setUuid(UUID.randomUUID().toString());
			}
		} else {
			abnormalJob.setUuid(UUID.randomUUID().toString());
		}
		if (!abnormalJob.isRead()) {
			try {
				reportAlarmService.dashboardAbnormalJob(abnormalJob.getDomainName(), abnormalJob.getJobName(), timeZone,
						nextFireTime);
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}
	}

	/**
	 * 检查和处理问题作业
	 */
	private void checkAndHandleJobProblem(List<AbnormalJob> oldAbnormalJobs, CuratorFramework curatorClient,
			AbnormalJob abnormalJob, String enabledPath, String shardingItemStr, int zkNodeCVersion, String jobDegree,
			List<AbnormalJob> unnormalJobList) throws Exception {
		if (unnormalJobList.contains(abnormalJob)) {
			return;
		}

		long nextFireTime = checkShardingState(curatorClient, abnormalJob, enabledPath, shardingItemStr);
		if (nextFireTime != -1 && doubleCheckShardingState(abnormalJob, shardingItemStr, zkNodeCVersion)) {
			if (abnormalJob.getCause() == null) {
				abnormalJob.setCause(AbnormalJob.Cause.NOT_RUN.name());
			}
			String timeZone = getTimeZone(abnormalJob.getJobName(), curatorClient);
			// 补充异常信息
			fillAbnormalJob(curatorClient, abnormalJob, abnormalJob.getCause(), timeZone, nextFireTime);
			// 如果有必要，上报hermes
			registerAbnormalJobIfNecessary(oldAbnormalJobs, abnormalJob, timeZone, nextFireTime);

			// 增加到非正常作业列表
			abnormalJob.setJobDegree(jobDegree);
			unnormalJobList.add(abnormalJob);

			log.info("Job sharding alert with DomainName: {}, JobName: {}, ShardingItem: {}, Cause: {}",
					abnormalJob.getDomainName(), abnormalJob.getJobName(), shardingItemStr, abnormalJob.getCause());
		}
	}

	/**
	 * 符合连续两次告警返回true，否则返回false ALLOW_DELAY_MILLIONSECONDS * 1.5 钝化触发检查的时间窗口精度 告警触发条件： 1、上次告警+本次检查窗口告警（连续2次）
	 * 2、上次告警CVersion与本次一致（说明当前本次检查窗口时间内没有子节点变更）
	 */
	private boolean doubleCheckShardingState(AbnormalJob abnormalJob, String shardingItemStr, int zkNodeCVersion) {
		String key = abnormalJob.getDomainName() + "_" + abnormalJob.getJobName() + "_" + shardingItemStr;
		long nowTime = System.currentTimeMillis();

		if (abnormalShardingStateCache.containsKey(key)) {
			AbnormalShardingState abnormalShardingState = abnormalShardingStateCache.get(key);
			if (abnormalShardingState != null
					&& abnormalShardingState.getAlertTime() + ALLOW_DELAY_MILLIONSECONDS * 1.5 > nowTime
					&& abnormalShardingState.getZkNodeCVersion() == zkNodeCVersion) {
				abnormalShardingStateCache.put(key, new AbnormalShardingState(nowTime, zkNodeCVersion));// 更新告警
				return true;
			} else {
				abnormalShardingStateCache.put(key, new AbnormalShardingState(nowTime, zkNodeCVersion));// 更新无效（过时）告警
				return false;
			}
		} else {
			abnormalShardingStateCache.put(key, new AbnormalShardingState(nowTime, zkNodeCVersion));// 新增告警信息
			return false;
		}
	}

	/**
	 * 判断分片状态
	 *
	 * 逻辑： 1、有running节点，返回正常 2.1、有completed节点，但马上就取不到Mtime，节点有变动说明正常 2.2、根据Mtime计算下次触发时间，比较下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
	 * 3、既没有running又没completed视为异常
	 * @param curatorClient
	 * @param abnormalJob
	 * @param shardingItemStr
	 * @return -1：状态正常，非-1：状态异常
	 * @throws Exception
	 */
	private long checkShardingState(CuratorFramework curatorClient, AbnormalJob abnormalJob, String enabledPath,
			String shardingItemStr) throws Exception {
		List<String> itemChildren = curatorClient.getChildren()
				.forPath(JobNodePath.getExecutionItemNodePath(abnormalJob.getJobName(), shardingItemStr));

		// 注意：针对stock-update域的不上报节点信息但又有分片残留的情况，分片节点下只有两个子节点，返回正常
		if (itemChildren.size() != 2) {
			// 有running节点，返回正常
			if (itemChildren.contains("running")) {
				return -1;
			}

			// 有completed节点：尝试取分片节点的Mtime时间
			// 1、能取到则根据Mtime计算下次触发时间，比较下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
			// 2、取不到（为0）说明completed节点刚好被删除了，节点有变动说明正常（上一秒还在，下一秒不在了）
			long currentTime = System.currentTimeMillis();
			if (itemChildren.contains("completed")) {
				String completedPath = JobNodePath.getExecutionNodePath(abnormalJob.getJobName(), shardingItemStr,
						"completed");
				long completedMtime = getMtime(curatorClient, completedPath);

				if (completedMtime > 0) {
					// 对比minCompletedMtime与enabled mtime, 取最大值
					long nextFireTimeAfterThis = getMtime(curatorClient, enabledPath);
					if (nextFireTimeAfterThis < completedMtime) {
						nextFireTimeAfterThis = completedMtime;
					}

					Long nextFireTimeExcludePausePeriod = jobDimensionService
							.getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(nextFireTimeAfterThis,
									abnormalJob.getJobName(),
									new CuratorRepositoryImpl().newCuratorFrameworkOp(curatorClient));
					// 下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
					if (nextFireTimeExcludePausePeriod != null
							&& nextFireTimeExcludePausePeriod + ALLOW_DELAY_MILLIONSECONDS < currentTime) {
						// 为了避免误报情况(executor时钟快过console时钟产生的误报)，加上一个delta，然后再计算
						if (!doubleCheckShardingStateAfterAddingDeltaInterval(curatorClient, abnormalJob,
								nextFireTimeAfterThis, nextFireTimeExcludePausePeriod, currentTime)) {
							log.debug("still has problem after adding delta interval");
							return nextFireTimeExcludePausePeriod;
						} else {
							return -1;
						}
					}
				} else {
					return -1;
				}
			}
			// 既没有running又没completed，同时nextFireTime + ALLOW_DELAY_MILLIONSECONDS < 当期时间，视为异常
			else {
				if (abnormalJob.getNextFireTimeAfterEnabledMtimeOrLastCompleteTime() == 0) {
					abnormalJob.setNextFireTimeAfterEnabledMtimeOrLastCompleteTime(
							jobDimensionService.getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(
									getMtime(curatorClient, enabledPath), abnormalJob.getJobName(),
									new CuratorRepositoryImpl().newCuratorFrameworkOp(curatorClient)));
				}

				Long nextFireTime = abnormalJob.getNextFireTimeAfterEnabledMtimeOrLastCompleteTime();
				// 下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
				if (nextFireTime != null && nextFireTime + ALLOW_DELAY_MILLIONSECONDS < currentTime) {
					return nextFireTime;
				}
			}
		}

		return -1;
	}

	/**
	 * 为了避免executor时钟比Console快的现象，加上一个修正值，然后计算新的nextFireTime + ALLOW_DELAY_MILLIONSECONDS 依然早于当前时间。
	 *
	 * @return false: 依然有异常；true: 修正后没有异常。
	 */
	private boolean doubleCheckShardingStateAfterAddingDeltaInterval(CuratorFramework curatorClient,
			AbnormalJob abnormalJob, long nextFireTimeAfterThis, Long nextFireTimeExcludePausePeriod,
			long currentTime) {
		Long nextFireTimeExcludePausePeriodWithDelta = jobDimensionService
				.getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(nextFireTimeAfterThis + INTERVAL_DELTA_IN_SECOND,
						abnormalJob.getJobName(), new CuratorRepositoryImpl().newCuratorFrameworkOp(curatorClient));

		if (nextFireTimeExcludePausePeriod.equals(nextFireTimeExcludePausePeriodWithDelta)
				|| nextFireTimeExcludePausePeriodWithDelta + ALLOW_DELAY_MILLIONSECONDS < currentTime) {
			log.debug("still not work after adding delta interval");
			return false;
		}

		return true;
	}

	private void checkJavaOrShellJobHasProblem(List<AbnormalJob> oldAbnormalJobs, CuratorFramework curatorClient,
			AbnormalJob abnormalJob, String jobDegree, List<AbnormalJob> unnormalJobList) {
		try {
			// 计算异常作业,根据$Jobs/jobName/execution/item/nextFireTime，如果小于当前时间且作业不在running，则为异常
			// 只有java/shell作业有cron
			String jobType = getData(curatorClient, JobNodePath.getConfigNodePath(abnormalJob.getJobName(), "jobType"));
			if (JobType.JAVA_JOB.name().equals(jobType) || JobType.SHELL_JOB.name().equals(jobType)) {
				// enabled 的作业才需要判断
				String enabledPath = JobNodePath.getConfigNodePath(abnormalJob.getJobName(), "enabled");
				if (Boolean.valueOf(getData(curatorClient, enabledPath))) {
					String enabledReportPath = JobNodePath.getConfigNodePath(abnormalJob.getJobName(), "enabledReport");
					String enabledReportVal = getData(curatorClient, enabledReportPath);
					// 开启上报运行信息
					if (enabledReportVal == null || "true".equals(enabledReportVal)) {
						String executionRootpath = JobNodePath.getExecutionNodePath(abnormalJob.getJobName());
						// 有execution节点
						List<String> items = null;
						try {
							items = curatorClient.getChildren().forPath(executionRootpath);
						} catch (Exception e) {
						}
						// 有分片
						if (items != null && !items.isEmpty()) {
							int shardingTotalCount = Integer.parseInt(getData(curatorClient,
									JobNodePath.getConfigNodePath(abnormalJob.getJobName(), "shardingTotalCount")));
							for (String itemStr : items) {
								int each = Integer.parseInt(itemStr);
								// 过滤历史遗留分片
								if (each >= shardingTotalCount) {
									continue;
								}
								checkAndHandleJobProblem(oldAbnormalJobs, curatorClient, abnormalJob, enabledPath,
										itemStr,
										getCVersion(curatorClient, JobNodePath
												.getExecutionItemNodePath(abnormalJob.getJobName(), itemStr)),
										jobDegree, unnormalJobList);
							}
						} else { // 无分片
							abnormalJob.setCause(AbnormalJob.Cause.NO_SHARDS.name());

							Long nextFireTime = jobDimensionService.getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(
									getMtime(curatorClient, enabledPath), abnormalJob.getJobName(),
									new CuratorRepositoryImpl().newCuratorFrameworkOp(curatorClient));
							// 下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
							if (nextFireTime != null
									&& nextFireTime + ALLOW_DELAY_MILLIONSECONDS < new Date().getTime()) {
								String timeZone = getTimeZone(abnormalJob.getJobName(), curatorClient);
								// 补充异常信息
								fillAbnormalJob(curatorClient, abnormalJob, abnormalJob.getCause(), timeZone,
										nextFireTime);
								// 如果有必要，上报hermes
								registerAbnormalJobIfNecessary(oldAbnormalJobs, abnormalJob, timeZone, nextFireTime);

								// 增加到非正常作业列表
								abnormalJob.setJobDegree(jobDegree);
								unnormalJobList.add(abnormalJob);

								log.info(
										"Job sharding alert with DomainName: {}, JobName: {}, ShardingItem: {}, Cause: {}",
										abnormalJob.getDomainName(), abnormalJob.getJobName(), 0,
										abnormalJob.getCause());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private String getTimeZone(String jobName, CuratorFramework curatorClient) {
		String timeZoneStr = getData(curatorClient, JobNodePath.getConfigNodePath(jobName, "timeZone"));
		if (timeZoneStr == null || timeZoneStr.trim().length() == 0) {
			timeZoneStr = SaturnConstants.TIME_ZONE_ID_DEFAULT;
		}
		return timeZoneStr;
	}

	/**
	 * 如果配置了超时告警时间，而且running节点存在时间大于它，则告警
	 */
	private Timeout4AlarmJob isTimeout4AlarmJob(List<Timeout4AlarmJob> oldTimeout4AlarmJobs, Timeout4AlarmJob timeout4AlarmJob,
			CuratorFrameworkOp curatorFrameworkOp) {
		String jobName = timeout4AlarmJob.getJobName();
		String timeout4AlarmSecondsStr = curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, "timeout4AlarmSeconds"));
		int timeout4AlarmSeconds = 0;
		if (timeout4AlarmSecondsStr != null) {
			try {
				timeout4AlarmSeconds = Integer.parseInt(timeout4AlarmSecondsStr);
			} catch (NumberFormatException e) {
				log.error(e.getMessage(), e);
			}
		}
		if (timeout4AlarmSeconds > 0) {
			List<String> items = new ArrayList<>();
			List<String> tmp = curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName));
			if (tmp != null) {
				items.addAll(tmp);
			}
			if (items != null && !items.isEmpty()) {
				long timeout4AlarmMills = timeout4AlarmSeconds * 1L * 1000;
				timeout4AlarmJob.setTimeout4AlarmSeconds(timeout4AlarmSeconds);
				for (String itemStr : items) {
					long ctime = curatorFrameworkOp
							.getCtime(JobNodePath.getExecutionNodePath(jobName, itemStr, "running"));
					if (ctime > 0 && System.currentTimeMillis() - ctime > timeout4AlarmMills) {
						timeout4AlarmJob.getTimeoutItems().add(Integer.parseInt(itemStr));
					}
				}
				if (!timeout4AlarmJob.getTimeoutItems().isEmpty()) {
					Timeout4AlarmJob oldJob = DashboardServiceHelper.findEqualTimeout4AlarmJob(timeout4AlarmJob, oldTimeout4AlarmJobs);
					if (oldJob != null) {
						timeout4AlarmJob.setRead(oldJob.isRead());
						if (oldJob.getUuid() != null) {
							timeout4AlarmJob.setUuid(oldJob.getUuid());
						} else {
							timeout4AlarmJob.setUuid(UUID.randomUUID().toString());
						}
					} else {
						timeout4AlarmJob.setUuid(UUID.randomUUID().toString());
					}
					if (!timeout4AlarmJob.isRead()) {
						try {
							reportAlarmService.dashboardTimeout4AlarmJob(timeout4AlarmJob.getDomainName(), jobName,
									timeout4AlarmJob.getTimeoutItems(), timeout4AlarmSeconds);
						} catch (Throwable t) {
							log.error(t.getMessage(), t);
						}
					}
					return timeout4AlarmJob;
				}
			}
		}
		return null;
	}

	// 无法高可用的情况：
	// 1、勾选只使用优先executor，preferList只有一个物理机器（剔除offline、deleted的物理机）
	// 2、没有勾选只使用优先executor，没有选择容器资源，可供选择的preferList只有一个物理机器（剔除offline、deleted的物理机，剔除容器资源）
	private AbnormalJob isUnableFailoverJob(CuratorFramework curatorClient, AbnormalJob unableFailoverJob,
			CuratorFrameworkOp curatorFrameworkOp) {
		try {
			String jobName = unableFailoverJob.getJobName();
			String preferList = getData(curatorClient, JobNodePath.getConfigNodePath(jobName, "preferList"));
			Boolean onlyUsePreferList = !Boolean
					.valueOf(getData(curatorClient, JobNodePath.getConfigNodePath(jobName, "useDispreferList")));
			List<ExecutorProvided> preferListProvided = jobDimensionService.getAllExecutors(jobName, curatorFrameworkOp);
			List<String> preferListArr = new ArrayList<>();
			if (preferList != null && preferList.trim().length() > 0) {
				String[] split = preferList.split(",");
				for (String prefer : split) {
					String tmp = prefer.trim();
					if (tmp.length() > 0) {
						if (!preferListArr.contains(tmp)) {
							preferListArr.add(tmp);
						}
					}
				}
			}
			boolean containerSelected = false;
			int count = 0;
			if (onlyUsePreferList) {
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
			if (!containerSelected && count == 1) {
				return unableFailoverJob;
			}
			return null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	private AbnormalContainer isContainerInstanceMismatch(AbnormalContainer abnormalContainer,
			CuratorFrameworkOp curatorFrameworkOp) {
		try {
			String taskId = abnormalContainer.getTaskId();
			String dcosTaskConfigNodePath = ContainerNodePath.getDcosTaskConfigNodePath(taskId);
			long configMtime = curatorFrameworkOp.getMtime(dcosTaskConfigNodePath);
			String dcosTaskScaleJobsNodePath = ContainerNodePath.getDcosTaskScaleJobsNodePath(taskId);
			List<String> scaleJobs = curatorFrameworkOp.getChildren(dcosTaskScaleJobsNodePath);
			long maxItemMtime = 0L;
			String lastScalaJob = null;
			if (scaleJobs != null && !taskId.isEmpty()) {
				for (String scaleJob : scaleJobs) {
					// 如果存在running节点，则不做检查
					if (curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(scaleJob, "0", "running"))) {
						return null;
					}

					String completedNodePath = JobNodePath.getExecutionNodePath(scaleJob, "0", "completed");
					long completedMtime = curatorFrameworkOp.getMtime(completedNodePath);
					if (completedMtime > maxItemMtime) {
						lastScalaJob = scaleJob;
						maxItemMtime = completedMtime;
					}
				}
			}
			Integer myInstance = -1;
			long lastMtime = 0L;
			if (configMtime > maxItemMtime) {
				String taskConfigData = curatorFrameworkOp.getData(dcosTaskConfigNodePath);
				if (taskConfigData != null && taskConfigData.trim().length() > 0) {
					ContainerConfig containerConfig = JSON.parseObject(taskConfigData, ContainerConfig.class);
					myInstance = containerConfig.getInstances();
					lastMtime = configMtime;
				}
			} else if (configMtime < maxItemMtime) {
				String dcosTaskScaleJobNodePath = ContainerNodePath.getDcosTaskScaleJobNodePath(taskId, lastScalaJob);
				String scaleJobData = curatorFrameworkOp.getData(dcosTaskScaleJobNodePath);
				if (scaleJobData != null && scaleJobData.trim().length() > 0) {
					ContainerScaleJob containerScaleJob = JSON.parseObject(scaleJobData, ContainerScaleJob.class);
					myInstance = containerScaleJob.getContainerScaleJobConfig().getInstances();
					lastMtime = maxItemMtime;
				}
			}
			if (myInstance != -1) {
				boolean timeAllowed = Math
						.abs(System.currentTimeMillis() - lastMtime) <= ALLOW_CONTAINER_DELAY_MILLIONSECONDS;
				if (timeAllowed) {
					return null;
				}

				int count = containerService.getContainerRunningInstances(taskId, curatorFrameworkOp);
				if (myInstance != count) {
					abnormalContainer.setCause(AbnormalContainer.Cause.CONTAINER_INSTANCE_MISMATCH.name());
					abnormalContainer.setConfigInstances(myInstance);
					abnormalContainer.setRunningInstances(count);
					try {
						reportAlarmService.dashboardContainerInstancesMismatch(abnormalContainer.getDomainName(),
								abnormalContainer.getTaskId(), abnormalContainer.getConfigInstances(),
								abnormalContainer.getRunningInstances());
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
					return abnormalContainer;
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public boolean checkExists(final CuratorFramework curatorClient, final String znode) {
		try {
			return null != curatorClient.checkExists().forPath(znode);
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			throw new JobConsoleException(ex);
		}
	}

	public long getMtime(final CuratorFramework curatorClient, final String znode) {
		try {
			Stat stat = curatorClient.checkExists().forPath(znode);
			if (stat != null) {
				return stat.getMtime();
			} else {
				return 0l;
			}
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			throw new JobConsoleException(ex);
		}
	}

	public int getCVersion(final CuratorFramework curatorClient, final String znode) {
		try {
			Stat stat = curatorClient.checkExists().forPath(znode);
			if (stat != null) {
				return stat.getCversion();
			} else {
				return 0;
			}
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			throw new JobConsoleException(ex);
		}
	}

	public String getData(final CuratorFramework curatorClient, final String znode) {
		try {
			if (checkExists(curatorClient, znode)) {
				byte[] getZnodeData = curatorClient.getData().forPath(znode);
				if (getZnodeData == null) {// executor的分片可能存在全部飘走的情况，sharding节点有可能获取到的是null，需要对null做判断，否则new
											// String时会报空指针异常
					return null;
				}
				return new String(getZnodeData, Charset.forName("UTF-8"));
			} else {
				return null;
			}
		} catch (final NoNodeException ex) {
			return null;
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			throw new JobConsoleException(ex);
		}
	}

	@Override
	public int executorInDockerCount(String zkList) {
		return getCountFromDB(StatisticsTableKeyConstant.EXECUTOR_IN_DOCKER_COUNT, zkList);
	}

	@Override
	public int executorNotInDockerCount(String zkList) {
		return getCountFromDB(StatisticsTableKeyConstant.EXECUTOR_NOT_IN_DOCKER_COUNT, zkList);
	}

	@Override
	public int jobCount(String zkList) {
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
	public SaturnStatistics top10FailureJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_JOB,
				zklist);
	}

	@Override
	public String top10FailureJobByAllZkCluster() {
		List<JobStatistics> jobStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
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
	public SaturnStatistics top10FailureExecutor(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_EXECUTOR,
				zklist);
	}

	@Override
	public String top10FailureExecutorByAllZkCluster() {
		List<ExecutorStatistics> executorStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
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
	public SaturnStatistics top10AactiveJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_ACTIVE_JOB,
				zklist);
	}

	@Override
	public String top10AactiveJobByAllZkCluster() {
		List<JobStatistics> jobStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
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
	public SaturnStatistics top10LoadExecutor(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_EXECUTOR,
				zklist);
	}

	@Override
	public String top10LoadExecutorByAllZkCluster() {
		List<ExecutorStatistics> executorStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
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
	public SaturnStatistics top10LoadJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_JOB,
				zklist);
	}

	@Override
	public String top10LoadJobByAllZkCluster() {
		List<JobStatistics> jobStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
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
	public SaturnStatistics top10UnstableDomain(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_UNSTABLE_DOMAIN,
				zklist);
	}

	@Override
	public String top10UnstableDomainByAllZkCluster() {
		List<DomainStatistics> domainStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
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
	public SaturnStatistics allProcessAndErrorCountOfTheDay(String zklist) {
		return saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.DOMAIN_PROCESS_COUNT_OF_THE_DAY, zklist);
	}

	@Override
	public String allProcessAndErrorCountOfTheDayByAllZkCluster() {
		int count = 0, error = 0;
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
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
	public SaturnStatistics allUnnormalJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zklist);
	}

	@Override
	public String allUnnormalJobByAllZkCluster() {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = allUnnormalJob(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<AbnormalJob> tempList = JSON.parseArray(result, AbnormalJob.class);
				if (tempList != null) {
					abnormalJobList.addAll(tempList);
				}
			}
		}
		abnormalJobList = DashboardServiceHelper.sortUnnormaoJobByTimeDesc(abnormalJobList);
		return JSON.toJSONString(abnormalJobList);
	}

	@Override
	public SaturnStatistics allTimeout4AlarmJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB,
				zklist);
	}

	@Override
	public String allTimeout4AlarmJobByAllZkCluster() {
		List<Timeout4AlarmJob> abnormalJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = allTimeout4AlarmJob(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<Timeout4AlarmJob> tempList = JSON.parseArray(result, Timeout4AlarmJob.class);
				if (tempList != null) {
					abnormalJobList.addAll(tempList);
				}
			}
		}
		return JSON.toJSONString(abnormalJobList);
	}

	@Override
	public SaturnStatistics allUnableFailoverJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB,
				zklist);
	}

	@Override
	public String allUnableFailoverJobByAllZkCluster() {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = allUnableFailoverJob(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<AbnormalJob> tempList = JSON.parseArray(result, AbnormalJob.class);
				if (tempList != null) {
					abnormalJobList.addAll(tempList);
				}
			}
		}
		return JSON.toJSONString(abnormalJobList);
	}

	@Override
	public SaturnStatistics top10FailureDomain(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_DOMAIN,
				zklist);
	}

	@Override
	public String top10FailureDomainByAllZkCluster() {
		List<DomainStatistics> domainStatisticsList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
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
	public void cleanShardingCount(String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		if (registryCenterClient.isConnected()) {
			CuratorFramework curatorClient = registryCenterClient.getCuratorClient();

			if (checkExists(curatorClient, ExecutorNodePath.SHARDING_COUNT_PATH)) {
				curatorClient.setData().forPath(ExecutorNodePath.SHARDING_COUNT_PATH, "0".getBytes());

			} else {
				curatorClient.create().forPath(ExecutorNodePath.SHARDING_COUNT_PATH, "0".getBytes());
			}
			asyncForceRefreshStatistics();
		}
	}

	@Override
	public void cleanOneJobAnalyse(String jobName, String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		if (registryCenterClient.isConnected()) {
			CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
			// reset analyse data.
			updateResetValue(curatorClient, jobName, ResetCountType.RESET_ANALYSE);

			resetOneJobAnalyse(jobName, curatorClient);
			asyncForceRefreshStatistics();
		}
	}

	@Override
	public void cleanAllJobAnalyse(String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		if (registryCenterClient.isConnected()) {
			CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
			CuratorFrameworkOp curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorClient);
			// 遍历所有$Jobs子节点，非系统作业
			List<String> jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
			for (String job : jobs) {
				resetOneJobAnalyse(job, curatorClient);
				// reset analyse data.
				updateResetValue(curatorClient, job, ResetCountType.RESET_ANALYSE);
			}
			asyncForceRefreshStatistics();
		}
	}

	@Override
	public void cleanAllJobExecutorCount(String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		if (registryCenterClient.isConnected()) {
			CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
			CuratorFrameworkOp curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorClient);
			// 遍历所有$Jobs子节点，非系统作业
			List<String> jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
			for (String job : jobs) {
				resetOneJobExecutorCount(job, curatorClient);
				// reset all jobs' executor's success/failure count.
				updateResetValue(curatorClient, job, ResetCountType.RESET_SERVERS);
			}
			asyncForceRefreshStatistics();
		}
	}

	@Override
	public void cleanOneJobExecutorCount(String jobName, String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		if (registryCenterClient.isConnected()) {
			CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
			// reset executor's success/failure count.
			updateResetValue(curatorClient, jobName, ResetCountType.RESET_SERVERS);
			resetOneJobExecutorCount(jobName, curatorClient);
			asyncForceRefreshStatistics();
		}
	}

	private void resetOneJobExecutorCount(String jobName, CuratorFramework curatorClient) throws Exception {
		if (null != curatorClient.checkExists().forPath(JobNodePath.getServerNodePath(jobName))) {
			List<String> servers = curatorClient.getChildren().forPath(JobNodePath.getServerNodePath(jobName));
			for (String server : servers) {
				if (checkExists(curatorClient, JobNodePath.getProcessSucessCount(jobName, server))) {
					curatorClient.setData().forPath(JobNodePath.getProcessSucessCount(jobName, server), "0".getBytes());
				} else {
					curatorClient.create().forPath(JobNodePath.getProcessSucessCount(jobName, server), "0".getBytes());
				}
				if (checkExists(curatorClient, JobNodePath.getProcessFailureCount(jobName, server))) {
					curatorClient.setData().forPath(JobNodePath.getProcessFailureCount(jobName, server),
							"0".getBytes());
				} else {
					curatorClient.create().forPath(JobNodePath.getProcessFailureCount(jobName, server), "0".getBytes());
				}
			}
		}
	}

	private void resetOneJobAnalyse(String jobName, CuratorFramework curatorClient) throws Exception {
		if (checkExists(curatorClient, JobNodePath.getProcessCountPath(jobName))) {
			curatorClient.setData().forPath(JobNodePath.getProcessCountPath(jobName), "0".getBytes());
		} else {
			curatorClient.create().forPath(JobNodePath.getProcessCountPath(jobName), "0".getBytes());
		}
		if (checkExists(curatorClient, JobNodePath.getErrorCountPath(jobName))) {
			curatorClient.setData().forPath(JobNodePath.getErrorCountPath(jobName), "0".getBytes());
		} else {
			curatorClient.create().forPath(JobNodePath.getErrorCountPath(jobName), "0".getBytes());
		}
	}

	private void updateResetValue(CuratorFramework curatorFramework, String job, String value) throws Exception {
		String path = JobNodePath.getAnalyseResetPath(job);
		if (checkExists(curatorFramework, JobNodePath.getAnalyseResetPath(job))) {
			curatorFramework.setData().forPath(path, value.getBytes());
		} else {
			curatorFramework.create().creatingParentsIfNeeded().forPath(path, value.getBytes());
		}
	}

	private void asyncForceRefreshStatistics() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					refreshStatistics2DB(true);
				} catch (Throwable t) {
					log.error("async refresh statistics error", t);
				}
			}
		};
		updateStatisticsThreadPool.submit(runnable);
	}

	@Override
	public Map<String, Integer> loadDomainRankDistribution(String zkClusterKey) {
		Map<String, Integer> domainMap = new HashMap<>();
		if (zkClusterKey != null) {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
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
	public Map<String, Integer> loadDomainRankDistributionByAllZkCluster() {
		Map<String, Integer> domainMap = new HashMap<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
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
	public Map<Integer, Integer> loadJobRankDistribution(String zklist) {
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
	public Map<Integer, Integer> loadJobRankDistributionByAllZkCluster() {
		Map<Integer, Integer> jobDegreeCountMap = new HashMap<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String zkAddr = zkCluster.getZkAddr();
			Map<Integer, Integer> temp = loadJobRankDistribution(zkAddr);
			if (temp != null) {
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
		}
		return jobDegreeCountMap;
	}

	@Override
	public SaturnStatistics abnormalContainer(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.ABNORMAL_CONTAINER,
				zklist);
	}

	@Override
	public String abnormalContainerByAllZkCluster() {
		List<AbnormalContainer> abnormalContainerList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = abnormalContainer(zkCluster.getZkAddr());
			if (saturnStatistics != null) {
				String result = saturnStatistics.getResult();
				List<AbnormalContainer> tempList = JSON.parseArray(result, AbnormalContainer.class);
				if (tempList != null) {
					abnormalContainerList.addAll(tempList);
				}
			}
		}
		return JSON.toJSONString(abnormalContainerList);
	}

	@Override
	public Map<String, Long> versionDomainNumber(String currentZkAddr) {
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
	public Map<String, Long> versionDomainNumberByAllZkCluster() {
		Map<String, Long> versionDomainNumberMap = new HashMap<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String zkAddr = zkCluster.getZkAddr();
			Map<String, Long> temp = versionDomainNumber(zkAddr);
			if (temp != null) {
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
		}
		return versionDomainNumberMap;
	}

	@Override
	public Map<String, Long> versionExecutorNumber(String currentZkAddr) {
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
	public Map<String, Long> versionExecutorNumberByAllZkCluster() {
		Map<String, Long> versionExecutorNumberMap = new HashMap<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String zkAddr = zkCluster.getZkAddr();
			Map<String, Long> temp = versionExecutorNumber(zkAddr);
			if (temp != null) {
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
		}
		return versionExecutorNumberMap;
	}

	@Override
	public void setUnnormalJobMonitorStatusToRead(String currentZkAddr, String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return;
		}
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, currentZkAddr);
		if (saturnStatistics != null) {
			String result = saturnStatistics.getResult();
			List<AbnormalJob> jobs = JSON.parseArray(result, AbnormalJob.class);
			if (jobs != null) {
				boolean find = false;
				for (AbnormalJob job : jobs) {
					if (uuid.equals(job.getUuid())) {
						job.setRead(true);
						find = true;
						break;
					}
				}
				if (find) {
					saturnStatistics.setResult(JSON.toJSONString(jobs));
					saturnStatisticsService.updateByPrimaryKeySelective(saturnStatistics);
				}
			}
		}
	}

	@Override
	public void setUnnormalJobMonitorStatusToReadByAllZkCluster(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return;
		}
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String zkAddr = zkCluster.getZkAddr();
			SaturnStatistics saturnStatistics = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zkAddr);
			if (saturnStatistics != null) {
				boolean find = false;
				String result = saturnStatistics.getResult();
				List<AbnormalJob> abnormalJobList = JSON.parseArray(result, AbnormalJob.class);
				if (abnormalJobList != null) {
					for (AbnormalJob abnormalJob : abnormalJobList) {
						if (uuid.equals(abnormalJob.getUuid())) {
							abnormalJob.setRead(true);
							find = true;
							break;
						}
					}
				}
				if (find) {
					saturnStatistics.setResult(JSON.toJSONString(abnormalJobList));
					saturnStatisticsService.updateByPrimaryKeySelective(saturnStatistics);
					return;
				}
			}
		}
	}

	@Override
	public void setTimeout4AlarmJobMonitorStatusToRead(String currentZkAddr, String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return;
		}
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, currentZkAddr);
		if (saturnStatistics != null) {
			String result = saturnStatistics.getResult();
			List<Timeout4AlarmJob> jobs = JSON.parseArray(result, Timeout4AlarmJob.class);
			if (jobs != null) {
				boolean find = false;
				for (Timeout4AlarmJob job : jobs) {
					if (uuid.equals(job.getUuid())) {
						job.setRead(true);
						find = true;
						break;
					}
				}
				if (find) {
					saturnStatistics.setResult(JSON.toJSONString(jobs));
					saturnStatisticsService.updateByPrimaryKeySelective(saturnStatistics);
				}
			}
		}
	}

	@Override
	public void setTimeout4AlarmJobMonitorStatusToReadByAllZkCluster(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return;
		}
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String zkAddr = zkCluster.getZkAddr();
			SaturnStatistics saturnStatistics = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, zkAddr);
			if (saturnStatistics != null) {
				boolean find = false;
				String result = saturnStatistics.getResult();
				List<Timeout4AlarmJob> jobs = JSON.parseArray(result, Timeout4AlarmJob.class);
				if (jobs != null) {
					for (Timeout4AlarmJob job : jobs) {
						if (uuid.equals(job.getUuid())) {
							job.setRead(true);
							find = true;
							break;
						}
					}
				}
				if (find) {
					saturnStatistics.setResult(JSON.toJSONString(jobs));
					saturnStatisticsService.updateByPrimaryKeySelective(saturnStatistics);
					return;
				}
			}
		}
	}

}
