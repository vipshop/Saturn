package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.JobBriefInfo;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobServer;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.NamespaceDomainInfo;
import com.vip.saturn.job.console.domain.RestApiJobConfig;
import com.vip.saturn.job.console.domain.RestApiJobInfo;
import com.vip.saturn.job.console.domain.RestApiJobStatistics;
import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.NamespaceInfo;
import com.vip.saturn.job.console.mybatis.service.NamespaceInfoService;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMapping4SqlService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.RestApiService;
import com.vip.saturn.job.console.service.helper.ReuseCallBack;
import com.vip.saturn.job.console.service.helper.ReuseCallBackWithoutReturn;
import com.vip.saturn.job.console.service.helper.ReuseUtils;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import com.vip.saturn.job.integrate.entity.AlarmInfo;
import com.vip.saturn.job.integrate.exception.ReportAlarmException;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author hebelala
 */
@Service
public class RestApiServiceImpl implements RestApiService {

	private static final Logger log = LoggerFactory.getLogger(RestApiServiceImpl.class);

	private static final long STATUS_UPDATE_FORBIDDEN_INTERVAL_IN_MILL_SECONDS = 3 * 1000L;

	private static final long OPERATION_FORBIDDEN_INTERVAL_AFTER_CREATION_IN_MILL_SECONDS = 10 * 1000L;

	private static final String JOB_STATUS_NOT_CORRECT_TEMPATE = "job' status is not {%s}";

	private static final String ALL_EXECUTORS_ARE_OFFLINE = "all executors are offline";

	private static final String NO_EXECUTOR_FOUND = "no executor found for this job";

	private static final String EXECUTOR_RESTART_TIME_PREFIX = "restart on ";

	private static final String NAMESPACE_CREATOR_NAME = "REST_API";

	private static final String ERR_MSG_TEMPLATE_FAIL_TO_CREATE = "Fail to create new namespace {%s} for reason {%s}";

	private static final String ERR_MSG_NS_NOT_FOUND = "The namespace does not exists.";

	private static final String ERR_MSG_NS_ALREADY_EXIST = "Invalid request. Namespace: {%s} already existed";

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private JobOperationService jobOperationService;

	@Resource
	private ReportAlarmService reportAlarmService;

	@Resource
	private ExecutorService executorService;

	@Resource
	private NamespaceInfoService namespaceInfoService;

	@Resource
	private NamespaceZkClusterMapping4SqlService namespaceZkClusterMapping4SqlService;

	@Override
	public void createJob(String namespace, final JobConfig jobConfig) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, registryCenterService, curatorRepository, new ReuseCallBackWithoutReturn() {
			@Override
			public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
				if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobConfig.getJobName()))) {
					throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
							"Invalid request. Job: {" + jobConfig.getJobName() + "} already existed");
				}
				int maxJobNum = executorService.getMaxJobNum();
				if (executorService.jobIncExceeds(maxJobNum, 1)) {
					throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
							"Invalid request. The current number of job reach the maximum limit[" + maxJobNum + "]");
				}
				jobOperationService.validateJobConfig(jobConfig);
				jobOperationService.persistJob(jobConfig, curatorFrameworkOp);
			}
		});

	}

	@Override
	public RestApiJobInfo getRestAPIJobInfo(String namespace, final String jobName) throws SaturnJobConsoleException {
		return ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository,
				new ReuseCallBack<RestApiJobInfo>() {
					@Override
					public RestApiJobInfo call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						if (!curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))) {
							throw new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(),
									"The jobName does not existed");
						}

						return constructJobInfo(curatorFrameworkOp, jobName);
					}
				});
	}

	@Override
	public List<RestApiJobInfo> getRestApiJobInfos(String namespace) throws SaturnJobConsoleException {
		return ReuseUtils.reuse(namespace, registryCenterService, curatorRepository,
				new ReuseCallBack<List<RestApiJobInfo>>() {
					@Override
					public List<RestApiJobInfo> call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						List<RestApiJobInfo> restApiJobInfos = new ArrayList<>();
						List<String> jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
						if (jobs != null) {
							for (String job : jobs) {
								try {
									RestApiJobInfo restApiJobInfo = constructJobInfo(curatorFrameworkOp, job);
									restApiJobInfos.add(restApiJobInfo);
								} catch (Exception e) {
									log.error("getRestApiJobInfos exception:", e);
									continue;
								}
							}
						}
						return restApiJobInfos;
					}
				});
	}

	private RestApiJobInfo constructJobInfo(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String job) {
		RestApiJobInfo restApiJobInfo = new RestApiJobInfo();
		restApiJobInfo.setJobName(job);
		// 设置作业配置信息
		setJobConfig(curatorFrameworkOp, restApiJobInfo, job);
		// 设置运行状态
		setRunningStatus(curatorFrameworkOp, restApiJobInfo, job);
		// 设置统计信息
		RestApiJobStatistics restApiJobStatistics = new RestApiJobStatistics();
		setStatics(curatorFrameworkOp, restApiJobStatistics, job);
		restApiJobInfo.setStatistics(restApiJobStatistics);
		return restApiJobInfo;
	}

	private void setRunningStatus(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			RestApiJobInfo restApiJobInfo, String jobName) {
		String executionNodePath = JobNodePath.getExecutionNodePath(jobName);
		List<String> items = curatorFrameworkOp.getChildren(executionNodePath);
		boolean isRunning = false;
		if (items != null) {
			for (String item : items) {
				String runningNodePath = JobNodePath.getExecutionNodePath(jobName, item, "running");
				if (curatorFrameworkOp.checkExists(runningNodePath)) {
					isRunning = true;
					break;
				}
			}
		}
		String enabledNodePath = JobNodePath.getConfigNodePath(jobName, "enabled");
		if (Boolean.valueOf(curatorFrameworkOp.getData(enabledNodePath))) {
			if (isRunning) {
				restApiJobInfo.setRunningStatus(JobStatus.RUNNING.name());
			} else {
				restApiJobInfo.setRunningStatus(JobStatus.READY.name());
			}
		} else {
			if (isRunning) {
				restApiJobInfo.setRunningStatus(JobStatus.STOPPING.name());
			} else {
				restApiJobInfo.setRunningStatus(JobStatus.STOPPED.name());
			}
		}
	}

	private void setJobConfig(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, RestApiJobInfo restApiJobInfo,
			String jobName) {
		String configNodePath = JobNodePath.getConfigNodePath(jobName);
		if (curatorFrameworkOp.checkExists(configNodePath)) {
			restApiJobInfo
					.setDescription(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "description")));
			restApiJobInfo.setEnabled(
					Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled"))));

			RestApiJobConfig restApiJobConfig = new RestApiJobConfig();
			restApiJobConfig
					.setJobClass(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobClass")));
			restApiJobConfig.setJobType(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType")));
			restApiJobConfig.setShardingTotalCount(Integer
					.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"))));
			restApiJobConfig.setShardingItemParameters(
					curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters")));
			restApiJobConfig.setJobParameter(
					curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobParameter")));
			String timeZone = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone"));
			if (timeZone == null || timeZone.trim().length() == 0) {
				timeZone = SaturnConstants.TIME_ZONE_ID_DEFAULT;
			}
			restApiJobConfig.setTimeZone(timeZone);
			restApiJobConfig.setCron(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron")));
			restApiJobConfig.setPausePeriodDate(
					curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate")));
			restApiJobConfig.setPausePeriodTime(
					curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime")));
			String timeout4AlarmSecondsStr = curatorFrameworkOp
					.getData(JobNodePath.getConfigNodePath(jobName, "timeout4AlarmSeconds"));
			if (timeout4AlarmSecondsStr != null) {
				restApiJobConfig.setTimeout4AlarmSeconds(Integer.valueOf(timeout4AlarmSecondsStr));
			} else {
				restApiJobConfig.setTimeout4AlarmSeconds(0);
			}
			restApiJobConfig.setTimeoutSeconds(Integer
					.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"))));
			restApiJobConfig
					.setChannelName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "channelName")));
			restApiJobConfig
					.setQueueName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "queueName")));
			restApiJobConfig.setLoadLevel(
					Integer.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"))));
			String jobDegree = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobDegree"));
			if (!Strings.isNullOrEmpty(jobDegree)) {
				restApiJobConfig.setJobDegree(Integer
						.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobDegree"))));
			}

			restApiJobConfig.setEnabledReport(Boolean
					.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabledReport"))));
			restApiJobConfig
					.setPreferList(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList")));
			restApiJobConfig.setUseDispreferList(Boolean
					.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useDispreferList"))));
			restApiJobConfig.setLocalMode(
					Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "localMode"))));
			restApiJobConfig.setUseSerial(
					Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useSerial"))));
			restApiJobConfig.setDependencies(
					curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "dependencies")));
			restApiJobConfig.setGroups(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "groups")));
			restApiJobConfig.setShowNormalLog(Boolean
					.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "showNormalLog"))));
			restApiJobConfig.setProcessCountInterValSeconds(Integer.valueOf(
					curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"))));

			restApiJobInfo.setJobConfig(restApiJobConfig);
		}
	}

	private void setStatics(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			RestApiJobStatistics restApiJobStatistics, String jobName) {
		setProcessCount(curatorFrameworkOp, restApiJobStatistics, jobName);
		setTimes(curatorFrameworkOp, restApiJobStatistics, jobName);
	}

	private void setProcessCount(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			RestApiJobStatistics restApiJobStatistics, String jobName) {
		String processCountPath = JobNodePath.getProcessCountPath(jobName);
		String processCountStr = curatorFrameworkOp.getData(processCountPath);
		if (processCountStr != null) {
			try {
				restApiJobStatistics.setProcessCount(Long.valueOf(processCountStr));
			} catch (NumberFormatException e) {
				log.error(e.getMessage(), e);
			}
		}
		String errorCountPath = JobNodePath.getErrorCountPath(jobName);
		String errorCountPathStr = curatorFrameworkOp.getData(errorCountPath);
		if (errorCountPathStr != null) {
			try {
				restApiJobStatistics.setProcessErrorCount(Long.valueOf(errorCountPathStr));
			} catch (NumberFormatException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private void setTimes(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			RestApiJobStatistics restApiJobStatistics, String jobName) {
		String executionNodePath = JobNodePath.getExecutionNodePath(jobName);
		List<String> items = curatorFrameworkOp.getChildren(executionNodePath);
		if (items != null) {
			List<String> lastBeginTimeList = new ArrayList<>();
			List<String> lastCompleteTimeList = new ArrayList<>();
			List<String> nextFireTimeList = new ArrayList<>();
			int runningItemSize = 0;
			for (String item : items) {
				if (getRunningIP(item, jobName, curatorFrameworkOp) == null) {
					continue;
				}
				++runningItemSize;
				String lastBeginTime = curatorFrameworkOp
						.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastBeginTime"));
				if (null != lastBeginTime) {
					lastBeginTimeList.add(lastBeginTime);
				}
				String lastCompleteTime = curatorFrameworkOp
						.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastCompleteTime"));
				if (null != lastCompleteTime) {
					boolean isItemCompleted = curatorFrameworkOp
							.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "completed"));
					boolean isItemRunning = curatorFrameworkOp
							.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "running"));
					if (isItemCompleted && !isItemRunning) { // 如果作业分片已执行完毕，则添加该完成时间到集合中进行排序
						lastCompleteTimeList.add(lastCompleteTime);
					}
				}
				String nextFireTime = curatorFrameworkOp
						.getData(JobNodePath.getExecutionNodePath(jobName, item, "nextFireTime"));
				if (null != nextFireTime) {
					nextFireTimeList.add(nextFireTime);
				}
			}
			if (!CollectionUtils.isEmpty(lastBeginTimeList)) {
				Collections.sort(lastBeginTimeList);
				try {
					restApiJobStatistics.setLastBeginTime(Long.parseLong(lastBeginTimeList.get(0))); // 所有分片中最近最早的开始时间
				} catch (NumberFormatException e) {
					log.error(e.getMessage(), e);
				}
			}
			if (!CollectionUtils.isEmpty(lastCompleteTimeList) && lastCompleteTimeList.size() == runningItemSize) { // 所有分配都完成才显示最近最晚的完成时间
				Collections.sort(lastCompleteTimeList);
				try {
					restApiJobStatistics.setLastCompleteTime(
							Long.parseLong(lastCompleteTimeList.get(lastCompleteTimeList.size() - 1))); // 所有分片中最近最晚的完成时间
				} catch (NumberFormatException e) {
					log.error(e.getMessage(), e);
				}
			}
			if (!CollectionUtils.isEmpty(nextFireTimeList)) {
				Collections.sort(nextFireTimeList);
				try {
					restApiJobStatistics.setNextFireTime(Long.parseLong(nextFireTimeList.get(0))); // 所有分片中下次最早的开始时间
				} catch (NumberFormatException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	private String getRunningIP(String item, String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String runningIp = null;
		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		if (!curatorFrameworkOp.checkExists(serverNodePath)) {
			return runningIp;
		}
		List<String> servers = curatorFrameworkOp.getChildren(serverNodePath);
		for (String server : servers) {
			String sharding = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, server, "sharding"));
			String toFind = "";
			if (Strings.isNullOrEmpty(sharding)) {
				continue;
			}
			for (String itemKey : Splitter.on(',').split(sharding)) {
				if (item.equals(itemKey)) {
					toFind = itemKey;
					break;
				}
			}
			if (!Strings.isNullOrEmpty(toFind)) {
				runningIp = server;
				break;
			}
		}
		return runningIp;
	}

	@Override
	public void enableJob(String namespace, final String jobName) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository,
				new ReuseCallBackWithoutReturn() {
					@Override
					public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						String enabledNodePath = JobNodePath.getConfigNodePath(jobName, "enabled");
						String enabled = curatorFrameworkOp.getData(enabledNodePath);
						if (Boolean.valueOf(enabled)) {
							throw new SaturnJobConsoleHttpException(HttpStatus.CREATED.value(),
									"The job is already enable");
						} else {
							long ctime = curatorFrameworkOp.getCtime(enabledNodePath);
							long mtime = curatorFrameworkOp.getMtime(enabledNodePath);
							checkUpdateStatusToEnableAllowed(ctime, mtime);

							curatorFrameworkOp.update(enabledNodePath, "true");
						}
					}
				});
	}

	@Override
	public void disableJob(String namespace, final String jobName) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository,
				new ReuseCallBackWithoutReturn() {
					@Override
					public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						String enabledNodePath = JobNodePath.getConfigNodePath(jobName, "enabled");
						String enabled = curatorFrameworkOp.getData(enabledNodePath);
						if (Boolean.valueOf(enabled)) {
							long mtime = curatorFrameworkOp.getMtime(enabledNodePath);
							checkUpdateStatusToDisableAllowed(mtime);

							curatorFrameworkOp.update(enabledNodePath, "false");
						} else {
							throw new SaturnJobConsoleHttpException(HttpStatus.CREATED.value(),
									"The job is already disable");
						}
					}
				});
	}

	@Override
	public void updateJobCron(final String namespace, final String jobName, final String cron,
			final Map<String, String> customContext) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository,
				new ReuseCallBackWithoutReturn() {
					@Override
					public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						String cronNodePath = JobNodePath.getConfigNodePath(jobName, "cron");
						long mtime = curatorFrameworkOp.getMtime(cronNodePath);
						checkUpdateConfigAllowed(mtime);
						String oldcronStr = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron"));
						if (!cron.equals(oldcronStr)) {
							jobOperationService.updateJobCron(jobName, cron, customContext);
						}
					}
				});
	}

	private void checkUpdateConfigAllowed(long lastMtime) throws SaturnJobConsoleHttpException {
		if (Math.abs(System.currentTimeMillis() - lastMtime) < STATUS_UPDATE_FORBIDDEN_INTERVAL_IN_MILL_SECONDS) {
			String errMsg = "The update interval time cannot less than "
					+ STATUS_UPDATE_FORBIDDEN_INTERVAL_IN_MILL_SECONDS / 1000 + " seconds";
			log.warn(errMsg);
			throw new SaturnJobConsoleHttpException(HttpStatus.FORBIDDEN.value(), errMsg);
		}
	}

	@Override
	public void runJobAtOnce(final String namespace, final String jobName) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository,
				new ReuseCallBackWithoutReturn() {
					@Override
					public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						JobStatus js = jobDimensionService.getJobStatus(jobName, curatorFrameworkOp);

						if (!JobStatus.READY.equals(js)) {
							throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
									String.format(JOB_STATUS_NOT_CORRECT_TEMPATE, JobStatus.READY.name()));
						}

						Collection<JobServer> servers = jobDimensionService.getServers(jobName, curatorFrameworkOp);

						if (CollectionUtils.isEmpty(servers)) {
							throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), NO_EXECUTOR_FOUND);
						}

						boolean everExecute = false;
						for (JobServer server : servers) {
							if (ServerStatus.ONLINE.equals(server.getStatus())) {
								log.info("run at once: job:{} executor:{}", jobName, server.getExecutorName());
								jobOperationService.runAtOnceByJobnameAndExecutorName(jobName, server.getExecutorName(),
										curatorFrameworkOp);
								everExecute = true;
							}
						}

						if (!everExecute) {
							throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
									ALL_EXECUTORS_ARE_OFFLINE);
						}
					}

				});
	}

	@Override
	public void stopJobAtOnce(final String namespace, final String jobName) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository,
				new ReuseCallBackWithoutReturn() {
					@Override
					public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						// if job is already STOPPED then return
						JobStatus js = jobDimensionService.getJobStatus(jobName, curatorFrameworkOp);
						if (JobStatus.STOPPED.equals(js)) {
							log.debug("job is already stopped");
							return;
						}

						String jobType = jobDimensionService.getJobType(jobName, curatorFrameworkOp);
						// For Msg Job
						if (JobBriefInfo.JobType.MSG_JOB.name().equals(jobType)) {
							boolean jobEnabled = jobDimensionService.isJobEnabled(jobName, curatorFrameworkOp);
							if (jobEnabled) {
								throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
										"job cannot be stopped while it is enable");
							}

							stopAtOnce(jobName, curatorFrameworkOp);
							return;
						}

						// For other Job types where the status is neither STOPPING nor STOPPED, cannot be stopped at
						// once
						if (JobStatus.READY.equals(js) || JobStatus.RUNNING.equals(js)) {
							throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
									"job cannot be stopped while its status is READY or RUNNING");
						}
						stopAtOnce(jobName, curatorFrameworkOp);
					}
				});
	}

	@Override
	public void deleteJob(String namespace, final String jobName) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository,
				new ReuseCallBackWithoutReturn() {
					@Override
					public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						JobStatus js = jobDimensionService.getJobStatus(jobName, curatorFrameworkOp);

						if (!JobStatus.STOPPED.equals(js)) {
							throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
									String.format(JOB_STATUS_NOT_CORRECT_TEMPATE, JobStatus.STOPPED.name()));
						}

						jobOperationService.deleteJob(jobName, curatorFrameworkOp);
						log.info("job:{} deletion done", jobName);
					}
				});
	}

	@Transactional(rollbackFor = {Exception.class})
	@Override
	public void createNamespace(NamespaceDomainInfo namespaceDomainInfo) throws SaturnJobConsoleException {
		String namespace = namespaceDomainInfo.getNamespace();
		String zkClusterKey = namespaceDomainInfo.getZkCluster();
		ZkCluster currentCluster = registryCenterService.getZkCluster(zkClusterKey);

		if (currentCluster == null) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(ERR_MSG_TEMPLATE_FAIL_TO_CREATE, namespace, "not found zkcluster" + zkClusterKey));
		}

		if (checkNamespaceExists(namespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(ERR_MSG_NS_ALREADY_EXIST, namespace));
		}

		try {
			// 创建 namespaceInfo
			NamespaceInfo namespaceInfo = constructNamespaceInfo(namespaceDomainInfo);
			namespaceInfoService.create(namespaceInfo);
			// 创建 zkcluster 和 namespaceInfo 关系
			namespaceZkClusterMapping4SqlService.insert(namespace, "", zkClusterKey, NAMESPACE_CREATOR_NAME);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					String.format(ERR_MSG_TEMPLATE_FAIL_TO_CREATE, namespace, e.getMessage()));
		}
	}

	@Override
	public void updateNamespace(NamespaceDomainInfo namespaceDomainInfo) throws SaturnJobConsoleException {
		String namespace = namespaceDomainInfo.getNamespace();

		if (!checkNamespaceExists(namespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					ERR_MSG_NS_NOT_FOUND);
		}

		try {
			// 创建 namespaceInfo
			NamespaceInfo namespaceInfo = constructNamespaceInfo(namespaceDomainInfo);
			namespaceInfoService.update(namespaceInfo);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					String.format(ERR_MSG_TEMPLATE_FAIL_TO_CREATE, namespace, e.getMessage()));
		}
	}

	@Override
	public NamespaceDomainInfo getNamespace(String namespace) throws SaturnJobConsoleException {
		if (namespaceInfoService.selectByNamespace(namespace) == null) {
			throw new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(), ERR_MSG_NS_NOT_FOUND);
		}

		String zkClusterKey = namespaceZkClusterMapping4SqlService.getZkClusterKey(namespace);
		if (StringUtils.isBlank(zkClusterKey)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(), ERR_MSG_NS_NOT_FOUND);
		}

		NamespaceDomainInfo namespaceDomainInfo = new NamespaceDomainInfo();
		namespaceDomainInfo.setNamespace(namespace);
		namespaceDomainInfo.setZkCluster(zkClusterKey);

		return namespaceDomainInfo;
	}

	private boolean checkNamespaceExists(String namespace) {
		if (namespaceInfoService.selectByNamespace(namespace) != null) {
			return true;
		}

		// 判断其它集群是否有该域
		String zkClusterKeyOther = namespaceZkClusterMapping4SqlService.getZkClusterKey(namespace);
		if (zkClusterKeyOther != null) {
			return true;
		}

		return false;
	}

	private NamespaceInfo constructNamespaceInfo(NamespaceDomainInfo namespaceDomainInfo) {
		NamespaceInfo namespaceInfo = new NamespaceInfo();
		namespaceInfo.setCreatedBy(NAMESPACE_CREATOR_NAME);
		namespaceInfo.setCreateTime(new Date());
		namespaceInfo.setIsDeleted(0);
		namespaceInfo.setLastUpdatedBy(NAMESPACE_CREATOR_NAME);
		namespaceInfo.setLastUpdateTime(new Date());
		namespaceInfo.setNamespace(namespaceDomainInfo.getNamespace());
		namespaceInfo.setContent(namespaceDomainInfo.getContent());

		return namespaceInfo;
	}

	private void stopAtOnce(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleHttpException {
		Collection<JobServer> servers = jobDimensionService.getServers(jobName, curatorFrameworkOp);
		if (CollectionUtils.isEmpty(servers)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), NO_EXECUTOR_FOUND);
		}

		for (JobServer server : servers) {
			log.info("stop at once: job:{} executor:{}", jobName, server.getExecutorName());
			jobOperationService.stopAtOnceByJobnameAndExecutorName(jobName, server.getExecutorName(),
					curatorFrameworkOp);
		}
	}

	@Override
	public void raiseAlarm(final String namespace, final String jobName, final String executorName,
						   final Integer shardItem, final AlarmInfo alarmInfo) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository,
				new ReuseCallBackWithoutReturn() {
					@Override
					public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						try {
							// verify executor exist or not
							if (!curatorFrameworkOp.checkExists(JobNodePath.getServerNodePath(jobName, executorName))) {
								throw new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(),
										String.format("The executor {%s} does not exists.", executorName));
							}

							reportAlarmService.raise(namespace, jobName, executorName, shardItem, alarmInfo);
						} catch (ReportAlarmException e) {
							throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
									e.getMessage());
						}
					}
				});
	}

	@Override
	public void raiseExecutorRestartAlarm(final String namespace, final String executorName, final AlarmInfo alarmInfo) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, registryCenterService, curatorRepository,
				new ReuseCallBackWithoutReturn() {
					@Override
					public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						try {
							String restartTime = obtainRestartTime(alarmInfo);
							reportAlarmService.executorRestart(namespace, executorName, restartTime);
						} catch (ReportAlarmException e) {
							throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
									e.getMessage());
						}
					}
				});
	}

	private String obtainRestartTime(AlarmInfo alarmInfo) {
		String msg = alarmInfo.getMessage();
		int idx = msg.indexOf(EXECUTOR_RESTART_TIME_PREFIX);
		if (idx > 0) {
			return msg.substring(idx + EXECUTOR_RESTART_TIME_PREFIX.length());
		}

		return "";
	}

	private void checkUpdateStatusToEnableAllowed(long ctime, long mtime) throws SaturnJobConsoleHttpException {
		if (Math.abs(
				System.currentTimeMillis() - ctime) < OPERATION_FORBIDDEN_INTERVAL_AFTER_CREATION_IN_MILL_SECONDS) {
			String errMsg = "Cannot enable the job until "
					+ OPERATION_FORBIDDEN_INTERVAL_AFTER_CREATION_IN_MILL_SECONDS / 1000
					+ " seconds after job creation!";
			log.warn(errMsg);
			throw new SaturnJobConsoleHttpException(HttpStatus.FORBIDDEN.value(), errMsg);
		}

		if (Math.abs(System.currentTimeMillis() - mtime) < STATUS_UPDATE_FORBIDDEN_INTERVAL_IN_MILL_SECONDS) {
			String errMsg = "The update interval time cannot less than "
					+ STATUS_UPDATE_FORBIDDEN_INTERVAL_IN_MILL_SECONDS / 1000 + " seconds";
			log.warn(errMsg);
			throw new SaturnJobConsoleHttpException(HttpStatus.FORBIDDEN.value(), errMsg);
		}
	}

	private void checkUpdateStatusToDisableAllowed(long lastMtime) throws SaturnJobConsoleHttpException {
		if (Math.abs(System.currentTimeMillis() - lastMtime) < STATUS_UPDATE_FORBIDDEN_INTERVAL_IN_MILL_SECONDS) {
			String errMsg = "The update interval time cannot less than "
					+ STATUS_UPDATE_FORBIDDEN_INTERVAL_IN_MILL_SECONDS / 1000 + " seconds";
			log.warn(errMsg);
			throw new SaturnJobConsoleHttpException(HttpStatus.FORBIDDEN.value(), errMsg);
		}
	}

}
