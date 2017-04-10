/**
 * 
 */
package com.vip.saturn.job.console.service.impl;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.base.Strings;
import com.vip.saturn.job.console.constants.StatisticsTableKeyConstant;
import com.vip.saturn.job.console.domain.AbnormalContainer;
import com.vip.saturn.job.console.domain.AbnormalJob;
import com.vip.saturn.job.console.domain.DomainStatistics;
import com.vip.saturn.job.console.domain.ExecutorStatistics;
import com.vip.saturn.job.console.domain.JobBriefInfo.JobType;
import com.vip.saturn.job.console.domain.JobStatistics;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.domain.ZkStatistics;
import com.vip.saturn.job.console.domain.container.ContainerConfig;
import com.vip.saturn.job.console.domain.container.ContainerScaleJob;
import com.vip.saturn.job.console.exception.JobConsoleException;
import com.vip.saturn.job.console.marathon.MarathonRestClient;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.mybatis.service.SaturnStatisticsService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.repository.zookeeper.impl.CuratorRepositoryImpl;
import com.vip.saturn.job.console.service.DashboardService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.helper.DashboardServiceHelper;
import com.vip.saturn.job.console.utils.ContainerNodePath;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.ResetCountType;

/**
 * @author chembo.huang
 *
 */
@Service
public class DashboardServiceImpl implements DashboardService {
	
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(DashboardServiceImpl.class);

	public static int REFRESH_INTERVAL_IN_MINUTE = 7;
	
	private static int ALLOW_DELAY_MILLIONSECONDS = 60 * 1000 * REFRESH_INTERVAL_IN_MINUTE;


	public static HashMap<String/** zkBsKey **/, List<AbnormalJob>> UNNORMAL_JOB_LIST_CACHE = new HashMap<>();
	public static HashMap<String/** zkBsKey **/, HashMap<String/** {jobname}-{domain} */, JobStatistics>> JOB_MAP_CACHE = new HashMap<>();
	public static HashMap<String/** zkBsKey **/, HashMap<String/** {executorName}-{domain} */, ExecutorStatistics>> EXECUTOR_MAP_CACHE = new HashMap<>();
	public static HashMap<String/** zkBsKey **/, Integer/** docker executor count */> DOCKER_EXECUTOR_COUNT_MAP = new HashMap<>();
	public static HashMap<String/** zkBsKey **/, Integer/** physical executor count */> PHYSICAL_EXECUTOR_COUNT_MAP = new HashMap<>();
	public static Map<String/** jobName **/, Long/** last alert time */> JOB_MAP_LAST_ALERT_TIME = new WeakHashMap<>();

	
	@Autowired
	private SaturnStatisticsService saturnStatisticsService;
	
	@Autowired
	private RegistryCenterService registryCenterService;
	
	@Autowired
	private JobDimensionService jobDimensionService;
	
	@Autowired
	private CuratorRepository curatorRepository;

	@Autowired
	private ReportAlarmServiceImpl reportAlarmService;

	@PostConstruct
	public void init() throws Exception {
		startRefreshStatisticsTimmer();
	}
	
	static {
		String refreshInterval = System.getProperty("VIP_SATURN_DASHBOARD_REFRESH_INTERVAL_MINUTE", System.getenv("VIP_SATURN_DASHBOARD_REFRESH_INTERVAL_MINUTE"));
		if (refreshInterval != null) {
			try {
				REFRESH_INTERVAL_IN_MINUTE = Integer.valueOf(refreshInterval);
				ALLOW_DELAY_MILLIONSECONDS = 60 * 1000 * REFRESH_INTERVAL_IN_MINUTE;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			String name = "single-update-satatistics";
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

	private TimerTask refreshStatisticsTask() {
		return new TimerTask() {
			@Override
			public void run() {
				try {
					Date start = new Date();
					log.info("start refresh statistics.");
					refreshStatistics2DB();
					log.info("end refresh statistics, takes " + (new Date().getTime()  - start.getTime()));
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		};
	}
	
	private void startRefreshStatisticsTimmer() {
		Timer timer = new Timer("refresh-statistics-to-db-timmer", true);
		timer.scheduleAtFixedRate(refreshStatisticsTask(), 1000 * 15 , 1000 * 60 * REFRESH_INTERVAL_IN_MINUTE);
	}

	@Override
	public synchronized void  refreshStatistics2DB() {
		Collection<ZkCluster> zkClusters = RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.values();
		for (ZkCluster zkCluster : zkClusters) {
			HashMap<String/** {jobname}-{domain} */, JobStatistics> jobMap = new HashMap<>();
			HashMap<String/** {executorName}-{domain} */, ExecutorStatistics> executorMap = new HashMap<>();
			List<JobStatistics> jobList = new ArrayList<>();
			List<ExecutorStatistics> executorList = new ArrayList<>();
			List<AbnormalJob> unnormalJobList = new ArrayList<>();
			List<AbnormalJob> cannotGetShardJobList = new ArrayList<>();
			List<AbnormalJob> unableFailoverJobList = new ArrayList<>();
			List<DomainStatistics> domainList = new ArrayList<>();
			List<AbnormalContainer> abnormalContainerList = new ArrayList<>();
			Map<String, Long> versionDomainNumber = new HashMap<>(); // 不同版本的域数量
			Map<String, Long> versionExecutorNumber = new HashMap<>(); // 不同版本的executor数量
			int exeInDocker = 0;
			int exeNotInDocker = 0;
			int totalCount = 0; 
			int errorCount = 0;
			for (RegistryCenterConfiguration config : RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.get(zkCluster.getZkAddr()).getRegCenterConfList()) {
				// 过滤非当前zk连接
				if (zkCluster.getZkAddr().equals(config.getZkAddressList())) {
					int processCountOfThisDomainAllTime = 0;
					int errorCountOfThisDomainAllTime = 0;
					int processCountOfThisDomainThisDay = 0;
					int errorCountOfThisDomainThisDay = 0;
					
					DomainStatistics domain = new DomainStatistics(config.getNamespace(), zkCluster.getZkAddr(), config.getNameAndNamespace());
							
					RegistryCenterClient registryCenterClient = registryCenterService.connect(config.getNameAndNamespace());
					try {
						if (registryCenterClient != null) {
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
								List<String> executors = curatorClient.getChildren().forPath(ExecutorNodePath.getExecutorNodePath());
								if(executors != null) {
									for (String exe : executors) {
										// 在线的才统计
										if (null != curatorClient.checkExists().forPath(ExecutorNodePath.getExecutorIpNodePath(exe))) {
											// 统计是物理机还是容器
											String executorMapKey = exe + "-" + config.getNamespace();
											ExecutorStatistics executorStatistics = executorMap.get(executorMapKey);
											if (executorStatistics == null) {
												executorStatistics = new ExecutorStatistics(exe, config.getNamespace());
												executorStatistics.setNns(domain.getNns());
												executorStatistics.setIp(getData(curatorClient, ExecutorNodePath.getExecutorIpNodePath(exe)));
												executorMap.put(executorMapKey, executorStatistics);
											}
											// set runInDocker field
											if (checkExists(curatorClient, ExecutorNodePath.get$ExecutorTaskNodePath(exe))) {
												executorStatistics.setRunInDocker(true);
												exeInDocker++;
											} else {
												exeNotInDocker++;
											}
										}
										// 获取版本号
										if (version == null) {
											version = getData(curatorClient, ExecutorNodePath.getExecutorVersionNodePath(exe));
										}
									}
									executorNumber = executors.size();
								}
							}
							// 统计版本数据
							if(version == null) { // 未知版本
								version = "-1";
							}
							if(versionDomainNumber.containsKey(version)) {
								Long domainNumber = versionDomainNumber.get(version);
								versionDomainNumber.put(version, domainNumber + 1);
							} else {
								versionDomainNumber.put(version, 1L);
							}
							if(versionExecutorNumber.containsKey(version)) {
								Long executorNumber0 = versionExecutorNumber.get(version);
								versionExecutorNumber.put(version, executorNumber0 + executorNumber);
							} else {
								if(executorNumber != 0) {
									versionExecutorNumber.put(version, executorNumber);
								}
							}
							
							// 遍历所有$Jobs子节点，非系统作业
							List<String> jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
							for (String job : jobs) {
								try{
									Boolean localMode = Boolean.valueOf(getData(curatorClient,JobNodePath.getConfigNodePath(job, "localMode")));
									String jobDomainKey = job + "-" + config.getNamespace();
									JobStatistics jobStatistics = jobMap.get(jobDomainKey);
									if (jobStatistics == null) {
										jobStatistics = new JobStatistics(job, config.getNamespace(),config.getNameAndNamespace());
										jobMap.put(jobDomainKey, jobStatistics);
									}
									
									String jobDegree = getData(curatorClient,JobNodePath.getConfigNodePath(job, "jobDegree"));
									if(Strings.isNullOrEmpty(jobDegree)){
										jobDegree = "0";
									}
									jobStatistics.setJobDegree(Integer.parseInt(jobDegree));
									
									// 非本地作业才参与判断
									if (!localMode) {
										AbnormalJob unnormalJob = new AbnormalJob(job, config.getNamespace(), config.getNameAndNamespace(), config.getDegree());
										if (isJavaOrShellJobHasProblem(curatorClient, unnormalJob) != null) {
											unnormalJob.setJobDegree(jobDegree);
											unnormalJobList.add(unnormalJob);
										}
									}
									
									// 查找无法高可用的作业
									AbnormalJob unableFailoverJob = new AbnormalJob(job, config.getNamespace(), config.getNameAndNamespace(), config.getDegree());
									if (isUnableFailoverJob(curatorClient, unableFailoverJob,curatorFrameworkOp) != null) {
										unableFailoverJob.setJobDegree(jobDegree);
										unableFailoverJobList.add(unableFailoverJob);
									}
									
									String processCountOfThisJobAllTimeStr = getData(curatorClient, JobNodePath.getProcessCountPath(job));
									String errorCountOfThisJobAllTimeStr = getData(curatorClient, JobNodePath.getErrorCountPath(job));
									int processCountOfThisJobAllTime = processCountOfThisJobAllTimeStr == null?0:Integer.valueOf(processCountOfThisJobAllTimeStr);
									int errorCountOfThisJobAllTime = processCountOfThisJobAllTimeStr == null?0:Integer.valueOf(errorCountOfThisJobAllTimeStr);
									processCountOfThisDomainAllTime += processCountOfThisJobAllTime;
									errorCountOfThisDomainAllTime += errorCountOfThisJobAllTime;
									int processCountOfThisJobThisDay = 0;
									int errorCountOfThisJobThisDay = 0;
									
									// loadLevel of this job
									int loadLevel = Integer.parseInt(getData(curatorClient,JobNodePath.getConfigNodePath(job, "loadLevel")));
									int shardingTotalCount = Integer.parseInt(getData(curatorClient,JobNodePath.getConfigNodePath(job, "shardingTotalCount")));
									List<String> servers = null;
									if (null != curatorClient.checkExists().forPath(JobNodePath.getServerNodePath(job))) {
										servers = curatorClient.getChildren().forPath(JobNodePath.getServerNodePath(job));
										for (String server:servers) {
											// 如果结点存活，算两样东西：1.遍历所有servers节点里面的processSuccessCount &  processFailureCount，用以统计作业每天的执行次数；2.统计executor的loadLevel;，
											if (checkExists(curatorClient, JobNodePath.getServerStatus(job, server))) {
												// 1.遍历所有servers节点里面的processSuccessCount &  processFailureCount，用以统计作业每天的执行次数；
												try {
													String processSuccessCountOfThisExeStr = getData(curatorClient, JobNodePath.getProcessSucessCount(job, server));
													String processFailureCountOfThisExeStr = getData(curatorClient, JobNodePath.getProcessFailureCount(job, server));
													int processSuccessCountOfThisExe = processSuccessCountOfThisExeStr == null?0:Integer.valueOf(processSuccessCountOfThisExeStr);
													int processFailureCountOfThisExe = processFailureCountOfThisExeStr == null?0:Integer.valueOf(processFailureCountOfThisExeStr);
													// 该作业当天运行统计
													processCountOfThisJobThisDay += processSuccessCountOfThisExe + processFailureCountOfThisExe;
													errorCountOfThisJobThisDay += processFailureCountOfThisExe;
													
													// 全部域当天的成功数与失败数
													totalCount += processSuccessCountOfThisExe + processFailureCountOfThisExe;
													errorCount += processFailureCountOfThisExe;
													
													// 全域当天运行统计
													processCountOfThisDomainThisDay += processCountOfThisJobThisDay;
													errorCountOfThisDomainThisDay += errorCountOfThisJobThisDay;
													
													// executor当天运行成功失败数
													String executorMapKey = server + "-" + config.getNamespace();
													ExecutorStatistics executorStatistics = executorMap.get(executorMapKey);
													if (executorStatistics == null) {
														executorStatistics = new ExecutorStatistics(server, config.getNamespace());
														executorStatistics.setNns(domain.getNns());
														executorStatistics.setIp(getData(curatorClient, ExecutorNodePath.getExecutorIpNodePath(server)));
														executorMap.put(executorMapKey, executorStatistics);
													}
													executorStatistics.setFailureCountOfTheDay(executorStatistics.getFailureCountOfTheDay() + processFailureCountOfThisExe);
													executorStatistics.setProcessCountOfTheDay(executorStatistics.getProcessCountOfTheDay() + processSuccessCountOfThisExe + processFailureCountOfThisExe);
													
												} catch (Exception e) {
													log.info(e.getMessage());
												}
												
												// 2.统计executor的loadLevel;
												try {
													// enabled 的作业才需要计算权重
													if (Boolean.valueOf(getData(curatorClient, JobNodePath.getConfigNodePath(job, "enabled")))) {
														String sharding = getData(curatorClient,JobNodePath.getServerSharding(job, server));
														if (StringUtils.isNotEmpty(sharding)) {
															// 更新job的executorsAndshards
															String exesAndShards = (jobStatistics.getExecutorsAndShards() == null?"":jobStatistics.getExecutorsAndShards())  + server + ":" + sharding + "; "; 
															jobStatistics.setExecutorsAndShards(exesAndShards);
															// 2.统计是物理机还是容器
															String executorMapKey = server + "-" + config.getNamespace();
															ExecutorStatistics executorStatistics = executorMap.get(executorMapKey);
															if (executorStatistics == null) {
																executorStatistics = new ExecutorStatistics(server, config.getNamespace());
																executorStatistics.setNns(domain.getNns());
																executorStatistics.setIp(getData(curatorClient, ExecutorNodePath.getExecutorIpNodePath(server)));
																executorMap.put(executorMapKey, executorStatistics);
																// set runInDocker field
																if (checkExists(curatorClient, ExecutorNodePath.get$ExecutorTaskNodePath(server))) {
																	executorStatistics.setRunInDocker(true);
																	exeInDocker ++;
																} else {
																	exeNotInDocker ++;
																}
															}
															if (executorStatistics.getJobAndShardings() != null) {
																executorStatistics.setJobAndShardings(executorStatistics.getJobAndShardings() + job + ":" + sharding + ";");
															} else {
																executorStatistics.setJobAndShardings(job + ":" + sharding + ";");
															}
															int newLoad = executorStatistics.getLoadLevel() + (loadLevel * sharding.split(",").length);
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
										jobStatistics.setTotalLoadLevel(servers == null?0:(servers.size() * loadLevel));
									} else {
										jobStatistics.setTotalLoadLevel(loadLevel * shardingTotalCount);
									}
									jobStatistics.setErrorCountOfAllTime(errorCountOfThisJobAllTime);
									jobStatistics.setProcessCountOfAllTime(processCountOfThisJobAllTime);
									jobStatistics.setFailureCountOfTheDay(errorCountOfThisJobThisDay);
									jobStatistics.setProcessCountOfTheDay(processCountOfThisJobThisDay);
									jobMap.put(jobDomainKey, jobStatistics);
								}catch(Exception e){
									log.info("statistics namespace:{} ,jobName:{} ,exception:{}",domain.getNns(),job,e.getMessage());
								}
							}

							// 遍历容器资源，获取异常资源
							String dcosTasksNodePath = ContainerNodePath.getDcosTasksNodePath();
							List<String> tasks = curatorFrameworkOp.getChildren(dcosTasksNodePath);
							if(tasks != null && !tasks.isEmpty()) {
								for(String taskId : tasks) {
									AbnormalContainer abnormalContainer = new AbnormalContainer(taskId, config.getNamespace(), config.getNameAndNamespace(), config.getDegree());
									if(isContainerInstanceMismatch(abnormalContainer, curatorFrameworkOp) != null) {
										abnormalContainerList.add(abnormalContainer);
									}
								}
							}
						}
					} catch (Exception e) {
						log.info("refreshStatistics2DB namespace:{} ,exception:{}",domain.getNns(), e.getMessage());
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
			
			// 不能获取到分片的作业列表（持久化分片有异常的作业）
			saveOrUpdateCannotGetShardJob(cannotGetShardJobList, zkCluster.getZkAddr());
			
			// 无法高可用的作业列表
			saveOrUpdateUnableFailoverJob(unableFailoverJobList, zkCluster.getZkAddr());

			// 异常容器资源列表，包含实例数不匹配的资源列表
			saveOrUpdateAbnormalContainer(abnormalContainerList, zkCluster.getZkAddr());

			// 不同版本的域数量
			saveOrUpdateVersionDomainNumber(versionDomainNumber, zkCluster.getZkAddr());

			// 不同版本的executor数量
			saveOrUpdateVersionExecutorNumber(versionExecutorNumber, zkCluster.getZkAddr());
			
			UNNORMAL_JOB_LIST_CACHE.put(zkCluster.getZkAddr(), unnormalJobList);

			JOB_MAP_CACHE.put(zkCluster.getZkAddr(), jobMap);
			EXECUTOR_MAP_CACHE.put(zkCluster.getZkAddr(), executorMap);
			DOCKER_EXECUTOR_COUNT_MAP.put(zkCluster.getZkAddr(), exeInDocker);
			PHYSICAL_EXECUTOR_COUNT_MAP.put(zkCluster.getZkAddr(), exeNotInDocker);
		}
	}

	private void saveOrUpdateTop10FailExecutor(List<ExecutorStatistics> executorList, String zkAddr) {
		try {
			executorList = DashboardServiceHelper.sortExecutorByFailureRate(executorList);
			List<ExecutorStatistics> top10FailExecutor = executorList.subList(0, executorList.size() > 9?10:executorList.size());
			String top10FailExecutorJsonString = JSON.toJSONString(top10FailExecutor);
			SaturnStatistics top10FailExecutorFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_EXECUTOR, zkAddr);
			if (top10FailExecutorFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_FAIL_EXECUTOR, zkAddr, top10FailExecutorJsonString);
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
			List<DomainStatistics> top10FailDomainList = domainList.subList(0, domainList.size() > 9? 10:domainList.size());
			String top10FailDomainJsonString = JSON.toJSONString(top10FailDomainList);
			SaturnStatistics top10FailDomainFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_DOMAIN, zkAddr);
			if (top10FailDomainFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_FAIL_DOMAIN, zkAddr, top10FailDomainJsonString);
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
			List<DomainStatistics> top10UnstableDomain = domainList.subList(0, domainList.size() > 9? 10:domainList.size());
			String top10UnstableDomainJsonString = JSON.toJSONString(top10UnstableDomain);
			SaturnStatistics top10UnstableDomainFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_UNSTABLE_DOMAIN, zkAddr);
			if (top10UnstableDomainFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_UNSTABLE_DOMAIN, zkAddr, top10UnstableDomainJsonString);
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
			List<JobStatistics> top10FailJob = jobList.subList(0, jobList.size() > 9?10:jobList.size());
			String top10FailJobJsonString = JSON.toJSONString(top10FailJob);
			SaturnStatistics top10FailJobFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_JOB, zkAddr);
			if (top10FailJobFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_FAIL_JOB, zkAddr, top10FailJobJsonString);
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
			List<JobStatistics> top10ActiveJob = jobList.subList(0, jobList.size() > 9?10:jobList.size());
			String top10ActiveJobJsonString = JSON.toJSONString(top10ActiveJob);
			SaturnStatistics top10ActiveJobFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_ACTIVE_JOB, zkAddr);
			if (top10ActiveJobFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_ACTIVE_JOB, zkAddr, top10ActiveJobJsonString);
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
			List<JobStatistics> top10LoadJob = jobList.subList(0, jobList.size() > 9?10:jobList.size());
			String top10LoadJobJsonString = JSON.toJSONString(top10LoadJob);
			SaturnStatistics top10LoadJobFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_JOB, zkAddr);
			if (top10LoadJobFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_LOAD_JOB, zkAddr, top10LoadJobJsonString);
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
			List<ExecutorStatistics> top10LoadExecutor = executorList.subList(0, executorList.size() > 9?10:executorList.size());
			String top10LoadExecutorJsonString = JSON.toJSONString(top10LoadExecutor);
			SaturnStatistics top10LoadExecutorFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_EXECUTOR, zkAddr);
			if (top10LoadExecutorFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.TOP_10_LOAD_EXECUTOR, zkAddr, top10LoadExecutorJsonString);
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
		String domainListJsonString = JSON.toJSONString(zks);
		SaturnStatistics domainProcessCountFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.DOMAIN_PROCESS_COUNT_OF_THE_DAY, zkAddr);
		if (domainProcessCountFromDB == null) {
			SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.DOMAIN_PROCESS_COUNT_OF_THE_DAY, zkAddr, domainListJsonString);
			saturnStatisticsService.create(ss);
		} else {
			domainProcessCountFromDB.setResult(domainListJsonString);
			saturnStatisticsService.updateByPrimaryKey(domainProcessCountFromDB);
		}
	}

	private void saveOrUpdateAbnormalJob(List<AbnormalJob> unnormalJobList, String zkAddr) {
		unnormalJobList = DashboardServiceHelper.sortUnnormaoJobByTimeDesc(unnormalJobList);
		String unnormalJobJsonString = JSON.toJSONString(unnormalJobList);
		SaturnStatistics unnormalJobFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zkAddr);
		if (unnormalJobFromDB == null) {
			SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.UNNORMAL_JOB, zkAddr, unnormalJobJsonString);
			saturnStatisticsService.create(ss);
		} else {
			unnormalJobFromDB.setResult(unnormalJobJsonString);
			saturnStatisticsService.updateByPrimaryKey(unnormalJobFromDB);
		}
	}
	
	private void saveOrUpdateCannotGetShardJob(List<AbnormalJob> cannotGetShardJobList, String zkAddr) {
		String cannotGetShardJobJsonString = JSON.toJSONString(cannotGetShardJobList);
		SaturnStatistics cannotGetShardJobFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.CANNOT_GET_SHARD_JOB, zkAddr);
		if (cannotGetShardJobFromDB == null) {
			SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.CANNOT_GET_SHARD_JOB, zkAddr, cannotGetShardJobJsonString);
			saturnStatisticsService.create(ss);
		} else {
			cannotGetShardJobFromDB.setResult(cannotGetShardJobJsonString);
			saturnStatisticsService.updateByPrimaryKey(cannotGetShardJobFromDB);
		}
	}
	
	private void saveOrUpdateUnableFailoverJob(List<AbnormalJob> unableFailoverJobList, String zkAddr) {
		String unableFailoverJobJsonString = JSON.toJSONString(unableFailoverJobList);
		SaturnStatistics unableFailoverJobFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB, zkAddr);
		if (unableFailoverJobFromDB == null) {
			SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB, zkAddr, unableFailoverJobJsonString);
			saturnStatisticsService.create(ss);
		} else {
			unableFailoverJobFromDB.setResult(unableFailoverJobJsonString);
			saturnStatisticsService.updateByPrimaryKey(unableFailoverJobFromDB);
		}
	}

	private void saveOrUpdateAbnormalContainer(List<AbnormalContainer> abnormalContainerList, String zkAddr) {
		String abnormalContainerJsonString = JSON.toJSONString(abnormalContainerList);
		SaturnStatistics abnormalContainerFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.ABNORMAL_CONTAINER, zkAddr);
		if (abnormalContainerFromDB == null) {
			SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.ABNORMAL_CONTAINER, zkAddr, abnormalContainerJsonString);
			saturnStatisticsService.create(ss);
		} else {
			abnormalContainerFromDB.setResult(abnormalContainerJsonString);
			saturnStatisticsService.updateByPrimaryKey(abnormalContainerFromDB);
		}
	}

	private void saveOrUpdateVersionDomainNumber(Map<String, Long> versionDomainNumber, String zkAddr) {
		try {
			String versionDomainNumberJsonString = JSON.toJSONString(versionDomainNumber);
			SaturnStatistics versionDomainNumberFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.VERSION_DOMAIN_NUMBER, zkAddr);
			if (versionDomainNumberFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.VERSION_DOMAIN_NUMBER, zkAddr, versionDomainNumberJsonString);
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
			SaturnStatistics versionExecutorNumberFromDB = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.VERSION_EXECUTOR_NUMBER, zkAddr);
			if (versionExecutorNumberFromDB == null) {
				SaturnStatistics ss = new SaturnStatistics(StatisticsTableKeyConstant.VERSION_EXECUTOR_NUMBER, zkAddr, versionExecutorNumberJsonString);
				saturnStatisticsService.create(ss);
			} else {
				versionExecutorNumberFromDB.setResult(versionExecutorNumberJsonString);
				saturnStatisticsService.updateByPrimaryKey(versionExecutorNumberFromDB);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private AbnormalJob isJavaOrShellJobHasProblem(CuratorFramework curatorClient, AbnormalJob abnormalJob) {
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
						String cause = null;
						String executionRootpath = JobNodePath.getExecutionNodePath(abnormalJob.getJobName());
	            		long minCompletedMtime = 0;
						// 有execution节点
		            	List<String> items = curatorClient.getChildren().forPath(executionRootpath);
		            	// 有分片
		            	if (items != null && !items.isEmpty()) {
		            		cause = AbnormalJob.Cause.NOT_RUN.name();
		            		int shardingTotalCount = Integer.parseInt(getData(curatorClient,JobNodePath.getConfigNodePath(abnormalJob.getJobName(), "shardingTotalCount")));
		            		boolean allRunning = true;
        					for (String itemStr : items) {
        						int each = Integer.parseInt(itemStr);
        						// 过滤历史遗留分片
        						if (each >= shardingTotalCount) {
        							continue;
        						}
					    		// 针对stock-update域的不上报节点信息但又有分片残留的情况
					    		List<String> itemChildren = curatorClient.getChildren().forPath(JobNodePath.getExecutionItemNodePath(abnormalJob.getJobName(), itemStr));
    				    		if (itemChildren.size() == 2) {
    					    		return null;
    				    		} else {
    				    			String completedPath = JobNodePath.getExecutionNodePath(abnormalJob.getJobName(), itemStr, "completed");
    				    			if (itemChildren.contains("completed")) {
    				    				// 上一秒还在，下一秒不在了，说明在跑，正常；
    				    				long completedMtime = getMtime(curatorClient, completedPath);
    				    				if (completedMtime == 0l) {
    				    					continue;
    				    				} else {
    				    					allRunning = false;
    				    					if (completedMtime < minCompletedMtime || minCompletedMtime == 0) {
    				    						minCompletedMtime = completedMtime;
    				    					}
    				    				}
    				    			} else if (itemChildren.contains("running")) { 
    				    				continue;
    				    			} else {
    				    				// 既无running又无completed视为异常，立即终止循环
    				    				allRunning = false;
    				    				minCompletedMtime = 0;
    				    				break;
    				    			}
    				    		}
				    		}
        					if (allRunning) {
        						return null;
        					}
		            	} else { // 无分片
		            		cause = AbnormalJob.Cause.NO_SHARDS.name();
		            	}
			            // 对比minCompletedMtime与enabled mtime, 取最大值
			    		long nextFireTimeAfterThis = getMtime(curatorClient, enabledPath);
			    		if (nextFireTimeAfterThis < minCompletedMtime) {
			    			nextFireTimeAfterThis = minCompletedMtime;
			    		}
			    		Long nextFireTimeExcludePausePeriod = jobDimensionService.getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(nextFireTimeAfterThis, abnormalJob.getJobName(), new CuratorRepositoryImpl().newCuratorFrameworkOp(curatorClient));
			    		// 下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
			    		if (nextFireTimeExcludePausePeriod != null && nextFireTimeExcludePausePeriod + ALLOW_DELAY_MILLIONSECONDS < new Date().getTime() ) {
			    			// all servers are not ready for the job
				    		boolean areNotReady = true;
				    		String serverNodePath = JobNodePath.getServerNodePath(abnormalJob.getJobName());
				    		if(checkExists(curatorClient, serverNodePath)) {
					    		List<String> servers = curatorClient.getChildren().forPath(serverNodePath);
					    		if(servers != null && !servers.isEmpty()) {
					    			for(String server : servers) {
					    				if(checkExists(curatorClient, JobNodePath.getServerStatus(abnormalJob.getJobName(), server))) {
					    					areNotReady = false;
					    					break;
					    				}
					    			}
					    		}
				    		}
				    		if(areNotReady) {
				    			cause = AbnormalJob.Cause.EXECUTORS_NOT_READY.name();
				    		}
					    	abnormalJob.setCause(cause);
					    	abnormalJob.setNextFireTime(nextFireTimeExcludePausePeriod);
							return abnormalJob;
			    		}
					} else {
						// 关闭上报视为正常
						return null;
					}
				}
				return null;
			}
			// 非java/shell job视为正常
			return null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	// 无法高可用的情况：
	// 1、勾选只使用优先executor，preferList只有一个物理机器（剔除offline、deleted的物理机）
	// 2、没有勾选只使用优先executor，没有选择容器资源，可供选择的preferList只有一个物理机器（剔除offline、deleted的物理机，剔除容器资源）
	private AbnormalJob isUnableFailoverJob(CuratorFramework curatorClient, AbnormalJob unableFailoverJob, CuratorFrameworkOp curatorFrameworkOp) {
		try {
			String jobName = unableFailoverJob.getJobName();
			String preferList = getData(curatorClient, JobNodePath.getConfigNodePath(jobName, "preferList"));
			Boolean onlyUsePreferList = !Boolean.valueOf(getData(curatorClient, JobNodePath.getConfigNodePath(jobName, "useDispreferList")));
			String preferListCandidateStr = jobDimensionService.getAllExecutors(jobName,curatorFrameworkOp);
			List<String> preferListArr = new ArrayList<>();
			if(preferList != null && preferList.trim().length() > 0) {
				String[] split = preferList.split(",");
				for(String prefer : split) {
					String tmp = prefer.trim();
					if(tmp.length() > 0) {
						if(!preferListArr.contains(tmp)) {
							preferListArr.add(tmp);
						}
					}
				}
			}
			if(preferListCandidateStr != null && preferListCandidateStr.trim().length() > 0) {
				String[] preferListCandidateArr = preferListCandidateStr.split(",");
				if (onlyUsePreferList) {
					boolean containerSelected = false;
					int count = 0;
					for(String preferListCandidate : preferListCandidateArr) {
						String tmp = preferListCandidate.split("\\(")[0];
						if(preferListCandidate.indexOf("容器资源") != -1) {
							tmp = "@" + tmp;
						}
						if(preferListArr.contains(tmp)) {
							if (preferListCandidate.indexOf("容器资源") != -1) {
								containerSelected = true;
								break;
							} else {
								if (preferListCandidate.indexOf("已离线") == -1 && preferListCandidate.indexOf("已删除") == -1) {
									count++;
								}
							}
						}
					}
					if(!containerSelected && count == 1) {
						return unableFailoverJob;
					}
				} else {
					boolean containerSelected = false;
					int count = 0;
					for(String preferListCandidate : preferListCandidateArr) {
						if(preferListCandidate.indexOf("容器资源") != -1 && preferListArr.contains("@" + preferListCandidate.split("\\(")[0])) {
							containerSelected = true;
							break;
						}
						if(preferListCandidate.indexOf("已离线") == -1 && preferListCandidate.indexOf("已删除") == -1 && preferListCandidate.indexOf("容器资源") == -1) {
							count++;
						}
					}
					if(!containerSelected && count == 1) {
						return unableFailoverJob;
					}
				}
			}
			return null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	private AbnormalContainer isContainerInstanceMismatch(AbnormalContainer abnormalContainer, CuratorFrameworkOp curatorFrameworkOp) {
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
					String completedNodePath = JobNodePath.getExecutionNodePath(scaleJob, "0", "completed");
					long completedMtime = curatorFrameworkOp.getMtime(completedNodePath);
					if (completedMtime > maxItemMtime) {
						lastScalaJob = scaleJob;
						maxItemMtime = completedMtime;
					}
				}
			}
			Integer myInstance = -1;
			if (configMtime > maxItemMtime) {
				String taskConfigData = curatorFrameworkOp.getData(dcosTaskConfigNodePath);
				if (taskConfigData != null && taskConfigData.trim().length() > 0) {
					ContainerConfig containerConfig = JSON.parseObject(taskConfigData, ContainerConfig.class);
					myInstance = containerConfig.getInstances();
				}
			} else if (configMtime < maxItemMtime) {
				String dcosTaskScaleJobNodePath = ContainerNodePath.getDcosTaskScaleJobNodePath(taskId, lastScalaJob);
				String scaleJobData = curatorFrameworkOp.getData(dcosTaskScaleJobNodePath);
				if (scaleJobData != null && scaleJobData.trim().length() > 0) {
					ContainerScaleJob containerScaleJob = JSON.parseObject(scaleJobData, ContainerScaleJob.class);
					myInstance = containerScaleJob.getContainerScaleJobConfig().getInstances();
				}
			}
			if (myInstance != -1) {
				String dcosConfigTokenNodePath = ContainerNodePath.getDcosConfigTokenNodePath();
				String tokenData = curatorFrameworkOp.getData(dcosConfigTokenNodePath);
				int count = MarathonRestClient.count("", "", taskId);
				if(myInstance != count) {
					abnormalContainer.setCause(AbnormalContainer.Cause.CONTAINER_INSTANCE_MISMATCH.name());
					abnormalContainer.setConfigInstances(myInstance);
					abnormalContainer.setRunningInstances(count);
					try {
						Map<String, String> alarmData = new HashMap<>();
						alarmData.put("eventType", ReportAlarmServiceImpl.EventType.CONTAINER_INSTANCE_MISMATCH.name());
						alarmData.put("domain", abnormalContainer.getDomainName());
						alarmData.put("taskId", abnormalContainer.getTaskId());
						alarmData.put("configInstances", String.valueOf(abnormalContainer.getConfigInstances()));
						alarmData.put("runningInstances", String.valueOf(abnormalContainer.getRunningInstances()));
						reportAlarmService.reportWarningAlarm(alarmData);
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
	public SaturnStatistics top10FailureJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_JOB, zklist);
	}

	@Override
	public SaturnStatistics top10FailureExecutor(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_EXECUTOR, zklist);
	}

	@Override
	public SaturnStatistics top10AactiveJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_ACTIVE_JOB, zklist);
	}

	@Override
	public SaturnStatistics top10LoadExecutor(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_EXECUTOR, zklist);
	}

	@Override
	public SaturnStatistics top10LoadJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_LOAD_JOB, zklist);
	}

	@Override
	public SaturnStatistics top10UnstableDomain(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_UNSTABLE_DOMAIN, zklist);
	}

	@Override
	public SaturnStatistics allProcessAndErrorCountOfTheDay(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.DOMAIN_PROCESS_COUNT_OF_THE_DAY, zklist);
	}

	@Override
	public SaturnStatistics allUnnormalJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zklist);
	}
	
	@Override
	public SaturnStatistics allCannotGetShardJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.CANNOT_GET_SHARD_JOB, zklist);
	}
	
	@Override
	public SaturnStatistics allUnableFailoverJob(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB, zklist);
	}

	@Override
	public SaturnStatistics top10FailureDomain(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TOP_10_FAIL_DOMAIN, zklist);
	}

	@Override
	public void cleanShardingCount(String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
		
		if (checkExists(curatorClient, ExecutorNodePath.SHARDING_COUNT_PATH)) {
			curatorClient.setData().forPath(ExecutorNodePath.SHARDING_COUNT_PATH, "0".getBytes());

		} else {
			curatorClient.create().forPath(ExecutorNodePath.SHARDING_COUNT_PATH, "0".getBytes());
		}
		asyncRefreshStatistics();
	}

	@Override
	public void cleanOneJobAnalyse(String jobName, String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
		// reset analyse data.
		updateResetValue(curatorClient, jobName, ResetCountType.RESET_ANALYSE);
		
		resetOneJobAnalyse(jobName, curatorClient);
		asyncRefreshStatistics();
	}
	
	@Override
	public void cleanAllJobAnalyse(String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
		CuratorFrameworkOp curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorClient);
		// 遍历所有$Jobs子节点，非系统作业
		List<String> jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
		for (String job : jobs) {
			resetOneJobAnalyse(job, curatorClient);
			// reset analyse data.
			updateResetValue(curatorClient, job, ResetCountType.RESET_ANALYSE);
		}
		asyncRefreshStatistics();
	}
	
	@Override
	public void cleanAllJobExecutorCount(String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
		CuratorFrameworkOp curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorClient);
		// 遍历所有$Jobs子节点，非系统作业
		List<String> jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
		for (String job : jobs) {
			resetOneJobExecutorCount(job, curatorClient);
			// reset all jobs' executor's success/failure count.
			updateResetValue(curatorClient, job, ResetCountType.RESET_SERVERS);
		}
		asyncRefreshStatistics();
	}

	@Override
	public void cleanOneJobExecutorCount(String jobName, String nns) throws Exception {
		// 获取当前连接
		RegistryCenterClient registryCenterClient = registryCenterService.connect(nns);
		CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
		// reset executor's success/failure count.
		updateResetValue(curatorClient, jobName, ResetCountType.RESET_SERVERS);
		resetOneJobExecutorCount(jobName, curatorClient);
		asyncRefreshStatistics();
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
					curatorClient.setData().forPath(JobNodePath.getProcessFailureCount(jobName, server), "0".getBytes());
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

	private void asyncRefreshStatistics() {
		singleThreadExecutor.submit(refreshStatisticsTask());
	}

	@Override
	public Map<String, Integer> loadDomainRankDistribution(String zkBsKey) {
		Map<String, Integer> domainMap = new HashMap<>();
		for (RegistryCenterConfiguration config : RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.get(zkBsKey).getRegCenterConfList()) {
			Integer count = domainMap.get(config.getDegree());
			if (null != config.getDegree()) {
				domainMap.put(config.getDegree(), count == null?1:count + 1);
			}
		}
		return domainMap;
	}
	
	@Override
	public Map<Integer, Integer> loadJobRankDistribution(String zkBsKey) {
		Map<Integer, Integer> jobDegreeMap = new HashMap<>();
		HashMap<String, JobStatistics> jobStatisticsMap = JOB_MAP_CACHE.get(zkBsKey);
		if(jobStatisticsMap == null || jobStatisticsMap.values().isEmpty()){
			return jobDegreeMap;
		}
		for (JobStatistics jobStatistics : jobStatisticsMap.values()) {
			Integer count = jobDegreeMap.get(jobStatistics.getJobDegree());
			jobDegreeMap.put(jobStatistics.getJobDegree(), count == null?1:count + 1);
		}
		return jobDegreeMap;
	}

	@Override
	public SaturnStatistics abnormalContainer(String zklist) {
		return saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.ABNORMAL_CONTAINER, zklist);
	}

	@Override
	public Map<String, Long> versionDomainNumber(String currentZkAddr) {
		SaturnStatistics ss = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.VERSION_DOMAIN_NUMBER, currentZkAddr);
		if(ss != null) {
			String result = ss.getResult();
			return JSON.parseObject(result, new TypeReference<Map<String, Long>>(){});
		} else {
			return new HashMap<>();
		}
	}

	@Override
	public Map<String, Long> versionExecutorNumber(String currentZkAddr) {
		SaturnStatistics ss = saturnStatisticsService.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.VERSION_EXECUTOR_NUMBER, currentZkAddr);
		if(ss != null) {
			String result = ss.getResult();
			return JSON.parseObject(result, new TypeReference<Map<String, Long>>(){});
		} else {
			return new HashMap<>();
		}
	}


}
