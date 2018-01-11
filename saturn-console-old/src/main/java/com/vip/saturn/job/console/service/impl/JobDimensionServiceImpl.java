/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.domain.ExecutionInfo.ExecutionStatus;
import com.vip.saturn.job.console.domain.JobBriefInfo.JobType;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.mybatis.service.HistoryJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp.CuratorTransactionOp;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.utils.BooleanWrapper;
import com.vip.saturn.job.console.utils.ContainerNodePath;
import com.vip.saturn.job.console.utils.CronExpression;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service
public class JobDimensionServiceImpl implements JobDimensionService {

	private static final Logger log = LoggerFactory.getLogger(JobDimensionServiceImpl.class);

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private HistoryJobConfigService historyJobConfigService;

	@Resource
	private CurrentJobConfigService currentJobConfigService;

	private MapperFacade mapper;

	@Autowired
	public void setMapperFactory(MapperFactory mapperFactory) {
		this.mapper = mapperFactory.getMapperFacade();
	}

	private JobBriefInfo genJobBriefInfo4tree(String jobName, Map<String, CurrentJobConfig> jobConfigs) {
		if (jobConfigs.containsKey(jobName)) {
			CurrentJobConfig jobConfig = jobConfigs.get(jobName);
			JobBriefInfo jobBriefInfo = new JobBriefInfo();
			jobBriefInfo.setJobName(jobName);
			jobBriefInfo.setDescription(jobConfig.getDescription());
			jobBriefInfo.setJobClass(jobConfig.getJobClass());
			jobBriefInfo.setJobType(JobType.getJobType(jobConfig.getJobType()));
			if (JobType.UNKOWN_JOB.equals(jobBriefInfo.getJobType())) {
				if (jobBriefInfo.getJobClass() != null && jobBriefInfo.getJobClass().indexOf("SaturnScriptJob") != -1) {
					jobBriefInfo.setJobType(JobType.SHELL_JOB);
				} else {
					jobBriefInfo.setJobType(JobType.JAVA_JOB);
				}
			}
			return jobBriefInfo;
		}
		return null;
	}

	@Override
	public Collection<JobBriefInfo> getAllJobsBriefInfo4Tree() {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		Map<String, CurrentJobConfig> jobConfigs = getJobConfigsFromDB(
				curatorFrameworkOp.getCuratorFramework().getNamespace());
		List<String> jobNames = new ArrayList<>();
		try {
			jobNames = getAllUnSystemJobs(jobConfigs);
		} catch (SaturnJobConsoleException e) {
			log.error(e.getMessage(), e);
		}
		List<JobBriefInfo> result = new ArrayList<>(jobNames.size());
		for (String jobName : jobNames) {
			try {
				JobBriefInfo jobBriefInfo = genJobBriefInfo4tree(jobName, jobConfigs);
				result.add(jobBriefInfo);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return result;
	}

	@Override
	public List<String> getAllJobs(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		List<String> allJobs = new ArrayList<>();
		if (curatorFrameworkOp == null) {
			curatorFrameworkOp = curatorRepository.inSessionClient();
		}
		String jobsNodePath = JobNodePath.get$JobsNodePath();
		if (curatorFrameworkOp.checkExists(jobsNodePath)) {
			List<String> jobs = curatorFrameworkOp.getChildren(jobsNodePath);
			if (jobs != null && jobs.size() > 0) {
				for (String job : jobs) {
					if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(job))) {// 如果config节点存在才视为正常作业，其他异常作业在其他功能操作时也忽略
						allJobs.add(job);
					}
				}
			}
		}
		Collections.sort(allJobs);
		return allJobs;
	}

	@Override
	public List<String> getAllUnSystemJobs(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		if (curatorFrameworkOp == null) {
			curatorFrameworkOp = curatorRepository.inSessionClient();
		}
		List<String> allJobs = getAllJobs(curatorFrameworkOp);
		Iterator<String> iterator = allJobs.iterator();
		while (iterator.hasNext()) {
			String job = iterator.next();
			String jobMode = JobNodePath.getConfigNodePath(job, "jobMode");
			if (curatorFrameworkOp.checkExists(jobMode)) {
				String data = curatorFrameworkOp.getData(jobMode);
				if (data != null && data.startsWith(JobMode.SYSTEM_PREFIX)) {
					iterator.remove();
				}
			}
		}
		return allJobs;
	}

	private List<String> getAllUnSystemJobs(Map<String, CurrentJobConfig> jobConfigMap)
			throws SaturnJobConsoleException {
		List<String> allJobs = new ArrayList<String>();
		for (CurrentJobConfig jobConfig : jobConfigMap.values()) {
			allJobs.add(jobConfig.getJobName());
		}
		Collections.sort(allJobs);
		Iterator<String> iterator = allJobs.iterator();
		while (iterator.hasNext()) {
			String job = iterator.next();
			CurrentJobConfig jobConfig = jobConfigMap.get(job);
			if (StringUtils.isNotBlank(jobConfig.getJobMode())) {
				if (jobConfig.getJobMode().startsWith(JobMode.SYSTEM_PREFIX)) {
					iterator.remove();
				}
			}
		}
		return allJobs;
	}

	@Override
	public List<JobConfig> getDependentJobsStatus(String jobName) throws SaturnJobConsoleException {
		List<JobConfig> jobConfigs = new ArrayList<>();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String dependencies = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "dependencies"));
		if (dependencies != null) {
			List<String> allUnSystemJobs = getAllUnSystemJobs(curatorFrameworkOp);
			String[] split = dependencies.split(",");
			if (split != null) {
				for (String tmp : split) {
					if (tmp != null) {
						String dependency = tmp.trim();
						if (dependency.length() > 0) {
							if (!dependency.equals(jobName) && allUnSystemJobs.contains(dependency)) {
								JobConfig jobConfig = new JobConfig();
								jobConfig.setJobName(dependency);
								jobConfig.setEnabled(Boolean.valueOf(curatorFrameworkOp
										.getData(JobNodePath.getConfigNodePath(dependency, "enabled"))));
								jobConfigs.add(jobConfig);
							}
						}
					}
				}
			}
		}
		return jobConfigs;
	}

	@Override
	public List<JobConfig> getDependedJobsStatus(String jobName) throws SaturnJobConsoleException {
		List<JobConfig> jobConfigs = new ArrayList<>();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		List<String> allUnSystemJobs = getAllUnSystemJobs(curatorFrameworkOp);
		if (allUnSystemJobs != null) {
			for (String job : allUnSystemJobs) {
				if (!job.equals(jobName)) {
					String dependencies = curatorFrameworkOp
							.getData(JobNodePath.getConfigNodePath(job, "dependencies"));
					if (dependencies != null) {
						String[] split = dependencies.split(",");
						if (split != null) {
							for (String tmp : split) {
								if (tmp != null) {
									String dependency = tmp.trim();
									if (dependency.equals(jobName)) {
										JobConfig jobConfig = new JobConfig();
										jobConfig.setJobName(job);
										jobConfig.setEnabled(Boolean.valueOf(curatorFrameworkOp
												.getData(JobNodePath.getConfigNodePath(job, "enabled"))));
										jobConfigs.add(jobConfig);
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		return jobConfigs;
	}

	@Override
	public Collection<JobBriefInfo> getAllJobsBriefInfo(String sessionBsKey, String namespace) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		namespace = curatorFrameworkOp.getCuratorFramework().getNamespace();
		Map<String, CurrentJobConfig> jobConfigs = getJobConfigsFromDB(namespace);
		List<String> jobNames = new ArrayList<>();
		try {
			jobNames = getAllUnSystemJobs(jobConfigs);
		} catch (SaturnJobConsoleException e) {
			log.error(e.getMessage(), e);
		}
		List<JobBriefInfo> result = new ArrayList<>(jobNames.size());
		for (String jobName : jobNames) {
			try {
				if (jobConfigs.containsKey(jobName)) {
					CurrentJobConfig currentJobConfig = jobConfigs.get(jobName);
					JobBriefInfo jobBriefInfo = genJobBriefInfo4tree(jobName, jobConfigs);
					jobBriefInfo.setIsJobEnabled(currentJobConfig.getEnabled());
					jobBriefInfo.setStatus(getJobStatus(jobName, curatorFrameworkOp, currentJobConfig.getEnabled()));
					jobBriefInfo.setJobParameter(currentJobConfig.getJobParameter());
					jobBriefInfo.setShardingItemParameters(currentJobConfig.getShardingItemParameters());
					jobBriefInfo.setQueueName(currentJobConfig.getQueueName());
					jobBriefInfo.setChannelName(currentJobConfig.getChannelName());
					jobBriefInfo.setLoadLevel(String.valueOf(currentJobConfig.getLoadLevel()));
					String jobDegree = currentJobConfig.getJobDegree() == null ? "0"
							: String.valueOf(currentJobConfig.getJobDegree());
					jobBriefInfo.setJobDegree(jobDegree);
					jobBriefInfo.setShardingTotalCount(String.valueOf(currentJobConfig.getShardingTotalCount()));

					if (currentJobConfig.getTimeout4AlarmSeconds() == null) {
						jobBriefInfo.setTimeout4AlarmSeconds(0);
					} else {
						jobBriefInfo.setTimeout4AlarmSeconds(currentJobConfig.getTimeout4AlarmSeconds());
					}
					jobBriefInfo.setTimeoutSeconds(currentJobConfig.getTimeoutSeconds());
					jobBriefInfo.setPausePeriodDate(currentJobConfig.getPausePeriodDate());
					jobBriefInfo.setPausePeriodTime(currentJobConfig.getPausePeriodTime());
					jobBriefInfo.setShowNormalLog(currentJobConfig.getShowNormalLog());
					jobBriefInfo.setLocalMode(currentJobConfig.getLocalMode());
					jobBriefInfo.setUseSerial(currentJobConfig.getUseSerial());
					jobBriefInfo.setUseDispreferList(currentJobConfig.getUseDispreferList());
					jobBriefInfo.setProcessCountIntervalSeconds(currentJobConfig.getProcessCountIntervalSeconds());
					jobBriefInfo.setGroups(currentJobConfig.getGroups());
					String preferList = currentJobConfig.getPreferList();
					jobBriefInfo.setPreferList(preferList);
					if (!Strings.isNullOrEmpty(preferList)) {
						String containerTaskIdsNodePath = ContainerNodePath.getDcosTasksNodePath();
						List<String> containerTaskIds = curatorFrameworkOp.getChildren(containerTaskIdsNodePath);
						jobBriefInfo.setMigrateEnabled(isMigrateEnabled(preferList, containerTaskIds));
					} else {
						jobBriefInfo.setMigrateEnabled(false);
					}
					String timeZone = currentJobConfig.getTimeZone();
					if (Strings.isNullOrEmpty(timeZone)) {
						jobBriefInfo.setTimeZone(SaturnConstants.TIME_ZONE_ID_DEFAULT);
					} else {
						jobBriefInfo.setTimeZone(timeZone);
					}
					jobBriefInfo.setCron(currentJobConfig.getCron());

					updateJobBriefInfoStatus(curatorFrameworkOp, jobName, jobBriefInfo);

					result.add(jobBriefInfo);
				}
			} catch (Exception e) {
				log.error("加载作业{}时出现异常：", jobName, e);
				continue;
			}
		}
		Collections.sort(result);
		return result;
	}

	private void updateJobBriefInfoStatus(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName,
			JobBriefInfo jobBriefInfo) {
		if (JobStatus.STOPPED.equals(jobBriefInfo.getStatus())) {// 作业如果是STOPPED状态，不需要显示已分配的executor
			return;
		}
		String executorsPath = JobNodePath.getServerNodePath(jobName);
		List<String> executors = curatorFrameworkOp.getChildren(executorsPath);
		if (CollectionUtils.isEmpty(executors)) {
			return;
		}
		StringBuilder shardingListSb = new StringBuilder();
		for (String executor : executors) {
			String sharding = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executor, "sharding"));
			if (!Strings.isNullOrEmpty(sharding)) {
				shardingListSb.append(executor).append(",");
			}
		}
		if (shardingListSb != null && shardingListSb.length() > 0) {
			jobBriefInfo.setShardingList(shardingListSb.substring(0, shardingListSb.length() - 1));
		}
	}

	private Map<String, CurrentJobConfig> getJobConfigsFromDB(String namespace) {
		Map<String, CurrentJobConfig> result = new HashMap<String, CurrentJobConfig>();
		List<CurrentJobConfig> jobConfigs = currentJobConfigService.findConfigsByNamespace(namespace);
		if (CollectionUtils.isEmpty(jobConfigs)) {
			return result;
		}
		for (CurrentJobConfig jobConfig : jobConfigs) {
			result.put(jobConfig.getJobName(), jobConfig);
		}
		return result;
	}

	private boolean isMigrateEnabled(String preferList, List<String> tasks) {
		if (tasks == null || tasks.isEmpty()) {
			return false;
		}
		List<String> preferTasks = new ArrayList<>();
		String[] split = preferList.split(",");
		for (int i = 0; i < split.length; i++) {
			String prefer = split[i].trim();
			if (prefer.startsWith("@")) {
				preferTasks.add(prefer.substring(1));
			}
		}
		if (!preferTasks.isEmpty()) {
			for (String task : tasks) {
				if (!preferTasks.contains(task)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String geJobRunningInfo(final String jobName) {
		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		if (!curatorFrameworkOp.checkExists(serverNodePath)) {
			return "";
		}
		List<String> servers = curatorFrameworkOp.getChildren(serverNodePath);
		// server为空表示没有Server工作，这个时候作业状态应该是Crashed
		if (servers == null || servers.size() == 0) {
			return "";
		}

		int processSuccessCount = 0;
		int processFailureCount = 0;

		for (String each : servers) {
			String processSuccessCountStr = curatorFrameworkOp
					.getData(JobNodePath.getServerNodePath(jobName, each, "processSuccessCount"));
			if (!Strings.isNullOrEmpty(processSuccessCountStr)) {
				processSuccessCount += Integer.parseInt(processSuccessCountStr);
			}

			String processFailureCountStr = curatorFrameworkOp
					.getData(JobNodePath.getServerNodePath(jobName, each, "processFailureCount"));
			if (!Strings.isNullOrEmpty(processFailureCountStr)) {
				processFailureCount += Integer.parseInt(processFailureCountStr);
			}
		}

		int count = processSuccessCount;
		int total = processSuccessCount + processFailureCount;
		if (total == 0) {
			return "";
		}
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits(2);
		String result = numberFormat.format((double) count / (double) total * 100);
		return result + "%";
	}

	@Override
	public JobStatus getJobStatus(final String jobName) {
		return getJobStatus(jobName, curatorRepository.inSessionClient());
	}

	public JobStatus getJobStatus(final String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			boolean enabled) {
		// see if all the shards is finished.
		boolean isAllShardsFinished = isAllShardsFinished(jobName, curatorFrameworkOp);
		return doGetJobStatus(isAllShardsFinished, enabled);
	}

	@Override
	public JobStatus getJobStatus(final String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		// see if all the shards is finished.
		boolean isAllShardsFinished = isAllShardsFinished(jobName, curatorFrameworkOp);
		// see if the job is enabled or not.
		boolean enabled = Boolean
				.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled")));
		return doGetJobStatus(isAllShardsFinished, enabled);
	}

	private JobStatus doGetJobStatus(boolean isAllShardsFinished, boolean enabled) {
		if (enabled) {
			if (isAllShardsFinished) {
				return JobStatus.READY;
			}
			return JobStatus.RUNNING;
		} else {
			if (isAllShardsFinished) {
				return JobStatus.STOPPED;
			}
			return JobStatus.STOPPING;
		}
	}

	private boolean isAllShardsFinished(final String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		List<String> executionItems = curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName));
		boolean isAllShardsFinished = true;
		if (executionItems != null && !executionItems.isEmpty()) {
			for (String itemStr : executionItems) {
				boolean isItemCompleted = curatorFrameworkOp
						.checkExists(JobNodePath.getExecutionNodePath(jobName, itemStr, "completed"));
				boolean isItemRunning = curatorFrameworkOp
						.checkExists(JobNodePath.getExecutionNodePath(jobName, itemStr, "running"));
				// if executor is kill by -9 while it is running, completed node won't exists as well as running node.
				// under this circumstance, we consider it is completed.
				if (!isItemCompleted && isItemRunning) {
					isAllShardsFinished = false;
					break;
				}
			}
		}
		return isAllShardsFinished;
	}

	@Override
	public JobSettings getJobSettings(final String jobName, RegistryCenterConfiguration configInSession) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		return getJobSettings(jobName, curatorFrameworkOp, configInSession);
	}

	@Override
	public JobSettings getJobSettings(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			RegistryCenterConfiguration configInSession) {
		return getJobSettings(jobName, curatorFrameworkOp, configInSession, false);
	}

	@Override
	public JobSettings getJobSettingsFromZK(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		return getJobSettings(jobName, curatorFrameworkOp, null, true);
	}

	private JobSettings getJobSettings(final String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			RegistryCenterConfiguration configInSession, boolean isJobConfigOnly) {
		JobSettings result = new JobSettings();
		result.setJobName(jobName);
		result.setJobType(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType")));
		result.setJobClass(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobClass")));
		// 兼容旧版没有msg_job。
		if (StringUtils.isBlank(result.getJobType())) {
			if (result.getJobClass().indexOf("script") > 0) {
				result.setJobType(JobType.SHELL_JOB.name());
			} else {
				result.setJobType(JobType.JAVA_JOB.name());
			}
		}
		result.setShardingTotalCount(Integer
				.parseInt(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"))));
		String timeZone = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone"));
		if (Strings.isNullOrEmpty(timeZone)) {
			result.setTimeZone(SaturnConstants.TIME_ZONE_ID_DEFAULT);
		} else {
			result.setTimeZone(timeZone);
		}
		result.setTimeZonesProvided(Arrays.asList(TimeZone.getAvailableIDs()));
		result.setCron(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron")));
		result.setPausePeriodDate(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate")));
		result.setPausePeriodTime(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime")));
		result.setShardingItemParameters(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters")));
		result.setJobParameter(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobParameter")));
		result.setProcessCountIntervalSeconds(Integer.parseInt(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"))));
		String timeout4AlarmSecondsStr = curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, "timeout4AlarmSeconds"));
		if (Strings.isNullOrEmpty(timeout4AlarmSecondsStr)) {
			result.setTimeout4AlarmSeconds(0);
		} else {
			result.setTimeout4AlarmSeconds(Integer.parseInt(timeout4AlarmSecondsStr));
		}
		result.setTimeoutSeconds(
				Integer.parseInt(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"))));
		String lv = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"));
		if (Strings.isNullOrEmpty(lv)) {
			result.setLoadLevel(1);
		} else {
			result.setLoadLevel(Integer.parseInt(lv));
		}
		String jobDegree = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobDegree"));
		if (Strings.isNullOrEmpty(jobDegree)) {
			result.setJobDegree(0);
		} else {
			result.setJobDegree(Integer.parseInt(jobDegree));
		}
		result.setEnabled(
				Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled"))));// 默认是禁用的
		result.setPreferList(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList")));
		if (!isJobConfigOnly) {
			result.setPreferListProvided(getAllExecutors(jobName));
		}
		String useDispreferList = curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, "useDispreferList"));
		if (Strings.isNullOrEmpty(useDispreferList)) {
			result.setUseDispreferList(null);
		} else {
			result.setUseDispreferList(Boolean.valueOf(useDispreferList));
		}
		result.setLocalMode(
				Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "localMode"))));
		result.setDependencies(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "dependencies")));
		result.setGroups(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "groups")));
		try {
			List<String> allUnSystemJobs = getAllUnSystemJobs(curatorFrameworkOp);
			if (allUnSystemJobs != null) {
				allUnSystemJobs.remove(jobName);
				result.setDependenciesProvided(allUnSystemJobs);
			}
		} catch (SaturnJobConsoleException e) {
			log.error(e.getMessage(), e);
		}
		result.setDescription(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "description")));
		result.setJobMode(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobMode")));
		result.setUseSerial(
				Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useSerial"))));
		result.setQueueName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "queueName")));
		result.setChannelName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "channelName")));
		if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName, "showNormalLog")) == false) {
			curatorFrameworkOp.create(JobNodePath.getConfigNodePath(jobName, "showNormalLog"));
		}
		String enabledReport = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabledReport"));
		Boolean enabledReportValue = Boolean.valueOf(enabledReport);
		if (Strings.isNullOrEmpty(enabledReport)) {
			enabledReportValue = true;
		}
		result.setEnabledReport(enabledReportValue);
		result.setShowNormalLog(
				Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "showNormalLog"))));
		if (!isJobConfigOnly) {
			try {
				CurrentJobConfig jobconfig = currentJobConfigService
						.findConfigByNamespaceAndJobName(configInSession.getNamespace(), result.getJobName());
				// config not exists, save it to current config.
				if (jobconfig == null) {
					CurrentJobConfig current = mapper.map(result, CurrentJobConfig.class);
					current.setCreateTime(new Date());
					current.setLastUpdateTime(new Date());
					current.setNamespace(configInSession.getNamespace());
					currentJobConfigService.create(current);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return result;
	}

	@Override
	public JobConfig getHistoryJobConfigByHistoryId(Long historyId) throws SaturnJobConsoleException {
		JobConfig jobConfig = null;
		try {
			jobConfig = historyJobConfigService.findByPrimaryKey(historyId);
			try {
				jobConfig.setTimeZonesProvided(SaturnConstants.TIME_ZONE_IDS);
				CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
				List<String> allUnSystemJobs = getAllUnSystemJobs(curatorFrameworkOp);
				if (allUnSystemJobs != null) {
					allUnSystemJobs.remove(jobConfig.getJobName());
					jobConfig.setDependenciesProvided(allUnSystemJobs);
				}
			} catch (SaturnJobConsoleException e) {
				log.error(e.getMessage(), e);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e.getMessage(), e);
		}
		return jobConfig;
	}

	@Transactional
	@Override
	public String updateJobSettings(final JobSettings jobSettings, RegistryCenterConfiguration configInSession) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		// Modify JobSettings.updateFields() sync, if the update fields changed.
		jobSettings.setDefaultValues();
		BooleanWrapper bw = new BooleanWrapper(false);
		CuratorTransactionOp curatorTransactionOp = null;
		try {
			curatorTransactionOp = curatorFrameworkOp.inTransaction()
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "jobMode"),
							jobSettings.getJobMode(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "shardingTotalCount"),
							jobSettings.getShardingTotalCount(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "loadLevel"),
							jobSettings.getLoadLevel(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "jobDegree"),
							jobSettings.getJobDegree(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "enabledReport"),
							jobSettings.getEnabledReport(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "timeZone"),
							StringUtils.trim(jobSettings.getTimeZone()), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "cron"),
							StringUtils.trim(jobSettings.getCron()), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "pausePeriodDate"),
							jobSettings.getPausePeriodDate(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "pausePeriodTime"),
							jobSettings.getPausePeriodTime(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "shardingItemParameters"),
							jobSettings.getShardingItemParameters(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "jobParameter"),
							jobSettings.getJobParameter(), bw)
					.replaceIfchanged(
							JobNodePath.getConfigNodePath(jobSettings.getJobName(), "processCountIntervalSeconds"),
							jobSettings.getProcessCountIntervalSeconds(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "timeout4AlarmSeconds"),
							jobSettings.getTimeout4AlarmSeconds(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "timeoutSeconds"),
							jobSettings.getTimeoutSeconds(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "dependencies"),
							jobSettings.getDependencies(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "groups"),
							jobSettings.getGroups(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "description"),
							jobSettings.getDescription(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "channelName"),
							StringUtils.trim(jobSettings.getChannelName()), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "queueName"),
							StringUtils.trim(jobSettings.getQueueName()), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "showNormalLog"),
							jobSettings.getShowNormalLog(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "preferList"),
							jobSettings.getPreferList(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "useDispreferList"),
							jobSettings.getUseDispreferList(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "failover"),
							jobSettings.getFailover(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "localMode"),
							jobSettings.getLocalMode(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "useSerial"),
							jobSettings.getUseSerial(), bw);
			if (jobSettings.getEnabledReport() != null && !jobSettings.getEnabledReport()) {// 当enabledReport关闭上报时，要清理execution节点
				log.info("the switch of enabledReport set to false, now deleteJob the execution zk node");
				String executionNodePath = JobNodePath.getExecutionNodePath(jobSettings.getJobName());
				if (curatorFrameworkOp.checkExists(executionNodePath)) {
					curatorFrameworkOp.deleteRecursive(executionNodePath);
				}
			}
		} catch (Exception e) {
			log.error("update settings to zk failed: {}", e.getMessage());
			log.error(e.getMessage(), e);
			return e.getMessage();
		}
		try {
			CurrentJobConfig jobconfig = currentJobConfigService
					.findConfigByNamespaceAndJobName(configInSession.getNamespace(), jobSettings.getJobName());
			// config not exists, save it to current config.
			if (jobconfig == null) {
				CurrentJobConfig current = mapper.map(jobSettings, CurrentJobConfig.class);
				current.setNamespace(configInSession.getNamespace());
				current.setCreateTime(new Date());
				current.setLastUpdateTime(new Date());
				currentJobConfigService.create(current);
			} else if (bw.isValue()) { // config changed, update current config and save a copy to history config.
				currentJobConfigService.updateConfigAndSave2History(jobconfig, jobSettings, null);
			}

			if (curatorTransactionOp != null) {
				curatorTransactionOp.commit();
			}
		} catch (Exception e) {
			log.error("update settings to db failed: {}", e.getMessage());
			log.error(e.getMessage(), e);
			return e.getMessage();
		}
		return null;
	}

	@Override
	public Collection<JobServer> getServers(final String jobName) {
		return getServers(jobName, curatorRepository.inSessionClient());
	}

	@Override
	public Collection<JobServer> getServers(final String jobName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		List<String> serverIps = new ArrayList<>();
		if (curatorFrameworkOp.checkExists(serverNodePath)) {
			serverIps = curatorFrameworkOp.getChildren(serverNodePath);
		}
		String leaderIp = curatorFrameworkOp.getData(JobNodePath.getLeaderNodePath(jobName, "election/host"));
		Collection<JobServer> result = new ArrayList<>(serverIps.size());
		for (String each : serverIps) {
			result.add(getJobServer(jobName, leaderIp, each, curatorFrameworkOp));
		}
		return result;
	}

	@Override
	public void getServersVersion(final String jobName, List<HealthCheckJobServer> allJobServers,
			RegistryCenterConfiguration registryCenterConfig) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		List<String> executorNames = new ArrayList<>();
		if (curatorFrameworkOp.checkExists(serverNodePath)) {
			executorNames = curatorFrameworkOp.getChildren(serverNodePath);
		}
		for (String executorName : executorNames) {
			if (allJobServers.size() >= SaturnConstants.HEALTH_CHECK_VERSION_MAX_SIZE) {// 容量控制，最多查询10000条
				break;
			}
			allJobServers.add(getJobServerVersion(jobName, executorName, registryCenterConfig));
		}
	}

	private JobServer getJobServer(final String jobName, final String leaderIp, final String serverIp,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		JobServer result = new JobServer();
		result.setExecutorName(serverIp);
		result.setIp(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, serverIp, "ip")));
		result.setVersion(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, serverIp, "version")));
		String processSuccessCount = curatorFrameworkOp
				.getData(JobNodePath.getServerNodePath(jobName, serverIp, "processSuccessCount"));
		result.setProcessSuccessCount(null == processSuccessCount ? 0 : Integer.parseInt(processSuccessCount));
		String processFailureCount = curatorFrameworkOp
				.getData(JobNodePath.getServerNodePath(jobName, serverIp, "processFailureCount"));
		result.setProcessFailureCount(null == processFailureCount ? 0 : Integer.parseInt(processFailureCount));
		result.setSharding(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, serverIp, "sharding")));
		result.setStatus(getServerStatus(jobName, serverIp, curatorFrameworkOp));
		result.setLeader(serverIp.equals(leaderIp));
		result.setJobStatus(getJobStatus(jobName, curatorFrameworkOp));
		result.setJobVersion(getJobVersion(jobName, serverIp, curatorFrameworkOp));
		return result;
	}

	private HealthCheckJobServer getJobServerVersion(final String jobName, final String executorName,
			RegistryCenterConfiguration registryCenterConfig) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		HealthCheckJobServer result = new HealthCheckJobServer();
		result.setExecutorName(executorName);
		result.setJobName(jobName);
		result.setNamespace(registryCenterConfig.getNamespace());
		result.setVersion(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executorName, "version")));
		return result;
	}

	private ServerStatus getServerStatus(final String jobName, final String serverIp,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String ip = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorNodePath(serverIp, "ip"));
		return ServerStatus.getServerStatus(ip);
	}

	private String getJobVersion(String jobName, String serverIp,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String jobVersion = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, serverIp, "jobVersion"));
		return jobVersion == null ? "" : jobVersion;
	}

	@Override
	public Collection<ExecutionInfo> getExecutionInfo(final String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		if (JobStatus.STOPPED.equals(getJobStatus(jobName))) {
			return Collections.emptyList();
		}
		// update report node
		curatorFrameworkOp.update(JobNodePath.getReportPath(jobName), System.currentTimeMillis());
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}
		String executionRootpath = JobNodePath.getExecutionNodePath(jobName);
		if (!curatorFrameworkOp.checkExists(executionRootpath)) {
			return Collections.emptyList();
		}

		List<ExecutionInfo> result = new ArrayList<>();
		Map<String, String> effectiveItemServerMapping = getEffectiveItemServerMapping(jobName, curatorFrameworkOp);
		Iterator<Map.Entry<String, String>> iterator = effectiveItemServerMapping.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, String> next = iterator.next();
			String item = next.getKey();
			String server = next.getValue();
			result.add(getExecutionInfo(jobName, item, server, curatorFrameworkOp));
		}
		Collections.sort(result);
		return result;
	}

	private ExecutionInfo getExecutionInfo(String jobName, String item, String server, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		ExecutionInfo result = new ExecutionInfo();
		result.setJobName(jobName);
		result.setItem(Integer.parseInt(item));

		String failoverNodePath = JobNodePath.getExecutionNodePath(jobName, item, "failover");
		String runningNodePath = JobNodePath.getExecutionNodePath(jobName, item, "running");
		String completedNodePath = JobNodePath.getExecutionNodePath(jobName, item, "completed");
		String failedNodePath = JobNodePath.getExecutionNodePath(jobName, item, "failed");
		String timeoutNodePath = JobNodePath.getExecutionNodePath(jobName, item, "timeout");
		String enabledReportNodePath = JobNodePath.getConfigNodePath(jobName, "enabledReport");

		boolean failover = curatorFrameworkOp.checkExists(failoverNodePath);
		boolean running = curatorFrameworkOp.checkExists(runningNodePath);
		boolean completed = curatorFrameworkOp.checkExists(completedNodePath);
		boolean failed = curatorFrameworkOp.checkExists(failedNodePath);
		boolean timeout = curatorFrameworkOp.checkExists(timeoutNodePath);

		if (failover) {
			result.setExecutorName(curatorFrameworkOp.getData(failoverNodePath));
			result.setFailover(true);
		} else if (running) {
			String runningData = curatorFrameworkOp.getData(runningNodePath);
			result.setExecutorName(Strings.isNullOrEmpty(runningData) ? server : runningData);
		} else if (completed) {
			String completedData = curatorFrameworkOp.getData(completedNodePath);
			result.setExecutorName(Strings.isNullOrEmpty(completedData) ? server : completedData);
		} else {
			result.setExecutorName(server);
		}

		boolean isEnabledReport = false;
		if (curatorFrameworkOp.checkExists(enabledReportNodePath)) {
			isEnabledReport = Boolean.valueOf(curatorFrameworkOp.getData(enabledReportNodePath));
		} else {
			String jobType = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType"));
			if (JobType.JAVA_JOB.name().equals(jobType) || JobType.SHELL_JOB.name().equals(jobType)) {
				isEnabledReport = true;
			}
		}
		result.setStatus(ExecutionStatus.getExecutionStatus(running, completed, failed, timeout, isEnabledReport));

		String jobMsg = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "jobMsg"));
		result.setJobMsg(jobMsg);

		String timeZoneStr = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone"));
		if (timeZoneStr == null || timeZoneStr.trim().length() == 0) {
			timeZoneStr = SaturnConstants.TIME_ZONE_ID_DEFAULT;
		}
		result.setTimeZone(timeZoneStr);
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(timeZone);
		String lastBeginTime = curatorFrameworkOp
				.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastBeginTime"));
		result.setLastBeginTime(null == lastBeginTime ? null : sdf.format(new Date(Long.parseLong(lastBeginTime))));
		String nextFireTime = curatorFrameworkOp
				.getData(JobNodePath.getExecutionNodePath(jobName, item, "nextFireTime"));
		result.setNextFireTime(null == nextFireTime ? null : sdf.format(new Date(Long.parseLong(nextFireTime))));
		String lastCompleteTime = curatorFrameworkOp
				.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastCompleteTime"));
		if (lastCompleteTime != null) {
			long lastCompleteTimeLong = Long.parseLong(lastCompleteTime);
			if (lastBeginTime == null) {
				result.setLastCompleteTime(sdf.format(new Date(lastCompleteTimeLong)));
			} else {
				long lastBeginTimeLong = Long.parseLong(lastBeginTime);
				if (lastCompleteTimeLong >= lastBeginTimeLong) {
					result.setLastCompleteTime(sdf.format(new Date(lastCompleteTimeLong)));
				}
			}
		}
		if (running) {
			long mtime = curatorFrameworkOp.getMtime(JobNodePath.getExecutionNodePath(jobName, item, "running"));
			result.setTimeConsumed((new Date().getTime() - mtime) / 1000);
		}
		return result;
	}

	private Map<String, String> getEffectiveItemServerMapping(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		Map<String, String> mapping = new HashMap<>();
		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		if (curatorFrameworkOp.checkExists(serverNodePath)) {
			List<String> servers = curatorFrameworkOp.getChildren(serverNodePath);
			if (servers != null) {
				for (String server : servers) {
					String shardingData = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, server, "sharding"));
					if (shardingData != null && !shardingData.trim().isEmpty()) {
						String[] split = shardingData.split(",");
						for (String tmp : split) {
							if (tmp != null) {
								tmp = tmp.trim();
								if (!tmp.isEmpty()) {
									mapping.put(tmp, server);
								}
							}
						}
					}
				}
			}
		}
		return mapping;
	}


	@Override
	public ExecutionInfo getExecutionJobLog(String jobName, int item) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		ExecutionInfo result = new ExecutionInfo();
		String logMsg = curatorFrameworkOp
				.getData(JobNodePath.getExecutionNodePath(jobName, String.valueOf(item), "jobLog"));
		result.setLogMsg(logMsg);
		return result;
	}

	@Override
	public String getJobType(String jobName) {
		return getJobType(jobName, curatorRepository.inSessionClient());
	}

	@Override
	public String getJobType(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		return curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType"));
	}

	/**
	 * 查找所有$SaturnExecutors下online的executor加上preferList配置中被删除的executor
	 *
	 * @param jobName 作业名称
	 * @return 所有executors服务器列表:executorName(ip)
	 */
	@Override
	public List<ExecutorProvided> getAllExecutors(String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		return getAllExecutors(jobName, curatorFrameworkOp);
	}

	@Override
	public List<ExecutorProvided> getAllExecutors(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		List<ExecutorProvided> executorProvidedList = new ArrayList<>();
		String executorsNodePath = SaturnExecutorsNode.getExecutorsNodePath();
		if (!curatorFrameworkOp.checkExists(executorsNodePath)) {
			return null;
		}
		List<String> executors = curatorFrameworkOp.getChildren(executorsNodePath);
		if (executors != null && executors.size() > 0) {
			for (String executor : executors) {
				if (curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorTaskNodePath(executor))) {
					continue;// 过滤容器中的Executor，容器资源只需要可以选择taskId即可
				}
				ExecutorProvided executorProvided = new ExecutorProvided();
				executorProvided.setExecutorName(executor);
				executorProvided.setNoTraffic(curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorNoTrafficNodePath(executor)));
				String ip = curatorFrameworkOp.getData(SaturnExecutorsNode.getExecutorIpNodePath(executor));
				if (StringUtils.isNotBlank(ip)) {
					executorProvided.setType(ExecutorProvidedType.ONLINE);
				} else {
					executorProvided.setType(ExecutorProvidedType.OFFLINE);
				}
				executorProvidedList.add(executorProvided);
			}
		}

		executorProvidedList.addAll(getContainerTaskIds(curatorFrameworkOp));

		if(jobName != null) {
			String preferListNodePath = JobNodePath.getConfigNodePath(jobName, "preferList");
			if (curatorFrameworkOp.checkExists(preferListNodePath)) {
				String preferList = curatorFrameworkOp.getData(preferListNodePath);
				if (!Strings.isNullOrEmpty(preferList)) {
					String[] preferExecutorList = preferList.split(",");
					for (String preferExecutor : preferExecutorList) {
						if (executors != null && !executors.contains(preferExecutor) && !preferExecutor.startsWith("@")) {
							ExecutorProvided executorProvided = new ExecutorProvided();
							executorProvided.setExecutorName(preferExecutor);
							executorProvided.setType(ExecutorProvidedType.DELETED);
							executorProvided.setNoTraffic(curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorNoTrafficNodePath(preferExecutor)));
							executorProvidedList.add(executorProvided);
						}
					}
				}
			}
		}
		return executorProvidedList;
	}

	@Override
	public List<ExecutorProvided> getAllExecutorsOfNamespace() {
		List<ExecutorProvided> executorProvidedList = new ArrayList<>();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();

		String executorsNodePath = SaturnExecutorsNode.getExecutorsNodePath();
		if (!curatorFrameworkOp.checkExists(executorsNodePath)) {
			return null;
		}
		List<String> executors = curatorFrameworkOp.getChildren(executorsNodePath);
		if (executors != null && executors.size() > 0) {
			for (String executor : executors) {
				if (curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorTaskNodePath(executor))) {
					continue;// 过滤容器中的Executor，容器资源只需要可以选择taskId即可
				}
				String ip = curatorFrameworkOp.getData(SaturnExecutorsNode.getExecutorIpNodePath(executor));
				if (StringUtils.isNotBlank(ip)) {// if ip exists, means the executor is online
					ExecutorProvided executorProvided = new ExecutorProvided();
					executorProvided.setExecutorName(executor);
					executorProvided.setNoTraffic(curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorNoTrafficNodePath(executor)));
					executorProvided.setType(ExecutorProvidedType.ONLINE);
					executorProvidedList.add(executorProvided);
					continue;
				}
			}
		}

		executorProvidedList.addAll(getContainerTaskIds(curatorFrameworkOp));

		return executorProvidedList;
	}

	/**
	 * 先获取DCOS节点下的taskID节点；如果没有此节点，则尝试从executor节点下获取;
	 * <p>
	 * 不存在既有DCOS容器，又有K8S容器的模式。
	 */
	private List<ExecutorProvided> getContainerTaskIds(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		List<ExecutorProvided> executorProvidedList = new ArrayList<>();

		List<String> containerTaskIds = getDCOSContainerTaskIds(curatorFrameworkOp);
		if (CollectionUtils.isEmpty(containerTaskIds)) {
			containerTaskIds = getK8SContainerTaskIds(curatorFrameworkOp);
		}

		if (!CollectionUtils.isEmpty(containerTaskIds)) {
			for(String task : containerTaskIds) {
				ExecutorProvided executorProvided = new ExecutorProvided();
				executorProvided.setExecutorName(task);
				executorProvided.setType(ExecutorProvidedType.DOCKER);
				executorProvidedList.add(executorProvided);
			}
		}

		return executorProvidedList;
	}

	private List<String> getK8SContainerTaskIds(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		List<String> taskIds = new ArrayList<>();
		String executorsNodePath = SaturnExecutorsNode.getExecutorsNodePath();
		List<String> executors = curatorFrameworkOp.getChildren(executorsNodePath);
		if (executors != null && executors.size() > 0) {
			for (String executor : executors) {
				String executorTaskNodePath = SaturnExecutorsNode.getExecutorTaskNodePath(executor);
				if (curatorFrameworkOp.checkExists(executorTaskNodePath)) {
					String taskId = curatorFrameworkOp.getData(executorTaskNodePath);
					if (taskId != null && !taskIds.contains(taskId)) {
						taskIds.add(taskId);
					}
				}
			}
		}
		return taskIds;
	}

	@Override
	public JobMigrateInfo getJobMigrateInfo(String jobName) throws SaturnJobConsoleException {
		JobMigrateInfo jobMigrateInfo = new JobMigrateInfo();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();

		List<String> tasksMigrateEnabled = new ArrayList<>();
		List<String> tasks = new ArrayList<>();
		String dcosTasksNodePath = ContainerNodePath.getDcosTasksNodePath();
		if (curatorFrameworkOp.checkExists(dcosTasksNodePath)) {
			tasks = curatorFrameworkOp.getChildren(dcosTasksNodePath);
		}
		List<String> preferTasks = new ArrayList<>();
		if (tasks != null && !tasks.isEmpty()) {
			String preferListNodePath = JobNodePath.getConfigNodePath(jobName, "preferList");
			if (curatorFrameworkOp.checkExists(preferListNodePath)) {
				String preferList = curatorFrameworkOp.getData(preferListNodePath);
				if (preferList != null) {
					String[] split = preferList.split(",");
					for (int i = 0; i < split.length; i++) {
						String prefer = split[i].trim();
						if (prefer.startsWith("@")) {
							preferTasks.add(prefer.substring(1));
						}
					}
				}
			}
			for (String tmp : tasks) {
				if (!preferTasks.contains(tmp)) {
					tasksMigrateEnabled.add(tmp);
				}
			}
		}

		jobMigrateInfo.setJobName(jobName);
		jobMigrateInfo.setTasksOld(preferTasks);
		jobMigrateInfo.setTasksMigrateEnabled(tasksMigrateEnabled);
		return jobMigrateInfo;
	}

	@Transactional
	@Override
	public void migrateJobNewTask(String jobName, String taskNew) throws SaturnJobConsoleException {
		try {
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();

			String jobConfigPreferList = getOldJobConfigPreferList(jobName, curatorFrameworkOp);

			validateMigrateJobNewTaskInfo(jobConfigPreferList, taskNew, curatorFrameworkOp);

			// replace the old task by new task
			String jobConfigPreferListNodePath = SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName);

			String newJobConfigPreferList = replaceTaskInPreferList(jobConfigPreferList, taskNew);
			savePreferList2DBAndZK(jobName, newJobConfigPreferList, curatorFrameworkOp, jobConfigPreferListNodePath);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
	}

	private String getOldJobConfigPreferList(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		String jobNodePath = SaturnExecutorsNode.getJobNodePath(jobName);
		if (!curatorFrameworkOp.checkExists(jobNodePath)) {
			throw new SaturnJobConsoleException("The job " + jobName + " does not exists");
		}
		String jobConfigPreferListNodePath = SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName);
		String jobConfigPreferList = curatorFrameworkOp.getData(jobConfigPreferListNodePath);
		if (jobConfigPreferList == null) {
			jobConfigPreferList = "";
		}

		return jobConfigPreferList;
	}

	private void validateMigrateJobNewTaskInfo(String jobConfigPreferList, String taskNew,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		if (StringUtils.isBlank(jobConfigPreferList)) {
			throw new SaturnJobConsoleException("The job has not set a docker task");
		}

		List<String> tasks = getTasks(jobConfigPreferList);

		if (!jobConfigPreferList.contains("@")) {
			throw new SaturnJobConsoleException("The job has not set a docker task");
		}
		if (tasks.isEmpty()) {
			throw new SaturnJobConsoleException("The job has not set a docker task");
		}

		if (!tasks.isEmpty() && tasks.contains(taskNew)) {
			throw new SaturnJobConsoleException("the new task is already set");
		}

		if (containsDCOSTaskId(curatorFrameworkOp)) {
			String dcosTaskNodePath = SaturnExecutorsNode.getDcosTaskNodePath(taskNew);
			if (!curatorFrameworkOp.checkExists(dcosTaskNodePath)) {
				throw new SaturnJobConsoleException("The new task does not exists");
			}
		}
	}

	private boolean containsDCOSTaskId(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		List<String> containerTaskIds = getDCOSContainerTaskIds(curatorFrameworkOp);
		return !CollectionUtils.isEmpty(containerTaskIds);
	}

	private List<String> getDCOSContainerTaskIds(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		List<String> containerTaskIds = Lists.newArrayList();

		String containerNodePath = ContainerNodePath.getDcosTasksNodePath();
		if (curatorFrameworkOp.checkExists(containerNodePath)) {
			containerTaskIds = curatorFrameworkOp.getChildren(containerNodePath);
		}

		return containerTaskIds;
	}

	private void savePreferListToDb(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			String newJobConfigPreferList) throws SaturnJobConsoleException, SaturnJobConsoleHttpException {
		String namespace = curatorFrameworkOp.getCuratorFramework().getNamespace();
		CurrentJobConfig oldCurrentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace,
				jobName);
		if (oldCurrentJobConfig == null) {
			log.error("找不到该作业的配置,namespace jobname is:" + namespace + " " + jobName);
			return;
		}
		CurrentJobConfig newCurrentJobConfig = mapper.map(oldCurrentJobConfig, CurrentJobConfig.class);
		newCurrentJobConfig.setPreferList(newJobConfigPreferList);
		try {
			currentJobConfigService.updateConfigAndSave2History(newCurrentJobConfig, oldCurrentJobConfig, null);
		} catch (Exception e) {
			log.error("exception is thrown during change perfer list in db", e);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	private String replaceTaskInPreferList(String jobConfigPreferList, String task) {
		String newJobConfigPreferList = "";
		String[] split = jobConfigPreferList.split(",");
		boolean hasReplaced = false;
		for (int i = 0; i < split.length; i++) {
			String tmp = split[i].trim();
			if (tmp.startsWith("@")) {
				newJobConfigPreferList = newJobConfigPreferList + "," + "@" + task;
			} else {
				newJobConfigPreferList = newJobConfigPreferList + "," + tmp;
			}
		}
		while (newJobConfigPreferList.startsWith(",")) {
			newJobConfigPreferList = newJobConfigPreferList.substring(1);
		}
		return newJobConfigPreferList;
	}

	private List<String> getTasks(String jobConfigPreferList) {
		List<String> tasks = new ArrayList<>();
		String[] split = jobConfigPreferList.split(",");
		for (int i = 0; i < split.length; i++) {
			String tmp = split[i].trim();
			if (tmp.startsWith("@")) {
				tasks.add(tmp.substring(1));
			}
		}
		return tasks;
	}

	@Override
	public boolean isJobEnabled(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		return Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled")));
	}

	@Override
	public boolean isJobEnabled(String jobName) {
		return isJobEnabled(jobName, curatorRepository.inSessionClient());
	}

	/**
	 * <blockquote>
	 * 
	 * <pre>
	 * 检查否是新版本的executor(新的域)
	 * 旧域：该域下必须至少有一个executor并且所有的executor都没有版本号version节点
	 * 新域：该域下必须至少有一个executor并且所有的executor都有版本号version节点(新版本的executor才在启动时添加了这个节点)
	 * 未知域：该域下没有任何executor或executor中既有新版的又有旧版的Executor
	 *
	 * &#64;param version 指定的executor的版本
	 * &#64;return 当version参数为空时：1：新域 0：旧域 -1：未知域(无法判断新旧域)
	 *         当version参数不为空时，说明要判断是否大于该版本，仅适用于1.1.0及其之后的版本比较：
	 *         	 2：该域下所有Executor的版本都大于等于指定的版本
	 *        	-2：该域下所有Executor的版本都小于指定的版本
	 *         	-3：Executor的版本存在大于、等于或小于指定的版本
	 * </pre>
	 * 
	 * </blockquote>
	 */
	@Override
	public int isNewSaturn(String version) {
		return isNewSaturn(version, null);
	}

	@Override
	public int isNewSaturn(String version, CuratorRepository.CuratorFrameworkOp cfo) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = cfo == null ? curatorRepository.inSessionClient()
				: cfo;

		if (!curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath())) {
			return -1;
		}
		List<String> executors = curatorFrameworkOp.getChildren(ExecutorNodePath.getExecutorNodePath());
		if (executors == null || executors.size() == 0) {
			return -1;
		}
		int oldExecutorSize = 0;
		int newExecutorSize = 0;
		int lessThanExecutorSize = 0;
		int moreThanExecutorSize = 0;
		boolean isCompareVersion = !Strings.isNullOrEmpty(version);
		for (String executor : executors) {
			String executorVersionPath = ExecutorNodePath.getExecutorNodePath(executor, "version");
			if (!curatorFrameworkOp.checkExists(executorVersionPath)) {
				++oldExecutorSize;
				continue;
			}
			++newExecutorSize;
			if (isCompareVersion) {// 1.1.0及之后的版本比较，1.1.0及其以后的executor才有version节点
				String executorVersion = curatorFrameworkOp.getData(executorVersionPath);
				try {
					if (Strings.isNullOrEmpty(executorVersion)) {
						++lessThanExecutorSize;// 如果取到的版本号为空串，默认认为是比当前指定版本要低
						continue;
					}
					int compareResult = compareVersion(executorVersion, version);
					if (compareResult < 0) {// 比指定版本小
						++lessThanExecutorSize;
						continue;
					}
					++moreThanExecutorSize;// 大于等于指定版本
				} catch (NumberFormatException e) {
					++lessThanExecutorSize;// 如果遇到非数字（非1.1.x）的版本号，如saturn-dev，默认认为是比当前指定版本要低
				}
			}
		}
		int executorSize = executors.size();
		if (oldExecutorSize == executorSize) {// 先判断如果是全是旧版本的话直接返回
			return 0;
		}
		if (isCompareVersion) {// 新版本才存在需要比较版本号的情况
			if (lessThanExecutorSize > 0 && moreThanExecutorSize > 0) {
				return -3;
			}
			if (lessThanExecutorSize == executorSize) {
				return -2;
			}
			if (moreThanExecutorSize == executorSize) {
				return 2;
			}
			return -1;// 该域下的executor有些有version节点，有些没有version节点，无法判断
		}
		if (newExecutorSize == executorSize) {
			return 1;
		}
		return -1;
	}

	/**
	 * 比较两个executor的版本，以“.”分割成数组，从第一个数开始逐一比较
	 * 
	 * <p>
	 * Examples: <blockquote>
	 * 
	 * <pre>
	 *     1.0.1 < 1.1.0
	 *     1.0.1 < 1.0.10
	 *     1.0.9 < 1.0.10
	 *     1.0.1 = 1.0.1
	 *     2.0.0 > 1.1.9
	 *     1.0.1.0 > 1.0.0.10
	 * </blockquote>
	 * </pre>
	 * 
	 * @param version1 executor1的版本
	 * @param version2 executor2的版本
	 * @return 1:version1的版本大于version2的版本 0:version1的版本等于version2的版本 -1:version1的版本小于version2的版本
	 */
	private int compareVersion(String version1, String version2) throws NumberFormatException {
		String[] version1Arr = version1.split("\\.");
		String[] version2Arr = version2.split("\\.");
		int versionLength = Math.min(version1Arr.length, version2Arr.length);
		for (int i = 0; i < versionLength; i++) {
			int v1 = Integer.parseInt(version1Arr[i]);
			int v2 = Integer.parseInt(version2Arr[i]);
			if (v1 > v2) {// 只要比较到某一位v1大于v2，就认为version1比version2大，如1.1.0和1.0.1.1的第二位就可以看出1.1.0>1.0.1.1
				return 1;
			}
			if (v1 < v2) {
				return -1;
			}
		}
		if (version1Arr.length == version2Arr.length) {// 1.0.1 = 1.0.1
			return 0;
		}
		if (version1Arr.length > version2Arr.length) {// 1.0.0.1 > 1.0.0
			return 1;
		}
		return -1;// 1.0.0 < 1.0.0.1
	}

	@Override
	public String formatTimeByJobTimeZone(String jobName, Long time) {
		if (time != null) {
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
			String timeZoneStr = getTimeZone(jobName, curatorFrameworkOp);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone(timeZoneStr));
			return timeZoneStr + " " + sdf.format(new Date(time));
		}
		return null;
	}

	private String getTimeZone(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String timeZoneStr = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone"));
		if (timeZoneStr == null || timeZoneStr.trim().length() == 0) {
			timeZoneStr = SaturnConstants.TIME_ZONE_ID_DEFAULT;
		}
		return timeZoneStr;
	}

	@Override
	public Long calculateJobNextTime(String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		try {
			// 计算异常作业,根据$Jobs/jobName/execution/item/nextFireTime，如果小于当前时间且作业不在running，则为异常
			// 只有java/shell作业有cron
			String jobType = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType"));
			if (JobType.JAVA_JOB.name().equals(jobType) || JobType.SHELL_JOB.name().equals(jobType)) {
				// enabled 的作业才需要判断
				String enabledPath = JobNodePath.getConfigNodePath(jobName, "enabled");
				if (Boolean.valueOf(curatorFrameworkOp.getData(enabledPath))) {
					String enabledReportPath = JobNodePath.getConfigNodePath(jobName, "enabledReport");
					String enabledReportVal = curatorFrameworkOp.getData(enabledReportPath);
					// 开启上报运行信息
					if (enabledReportVal == null || "true".equals(enabledReportVal)) {
						long nextFireTimeAfterThis = 0l;
						String executionRootpath = JobNodePath.getExecutionNodePath(jobName);
						// 有execution节点
						if (curatorFrameworkOp.checkExists(executionRootpath)) {
							List<String> items = curatorFrameworkOp.getChildren(executionRootpath);
							// 有分片
							if (items != null && !items.isEmpty()) {
								for (String itemStr : items) {
									// 针对stock-update域的不上报节点信息但又有分片残留的情况
									List<String> itemChildren = curatorFrameworkOp
											.getChildren(JobNodePath.getExecutionItemNodePath(jobName, itemStr));
									if (itemChildren.size() == 2) {
										return null;
									} else {
										String runningNodePath = JobNodePath.getExecutionNodePath(jobName, itemStr,
												"running");
										boolean isItemRunning = curatorFrameworkOp.checkExists(runningNodePath);
										if (isItemRunning) {
											try { // 以防节点不存在
												return curatorFrameworkOp.getMtime(runningNodePath);
											} catch (Exception e) {
												log.error(e.getMessage(), e);
											}
										}
										String completedPath = JobNodePath.getExecutionNodePath(jobName, itemStr,
												"completed");
										boolean isItemCompleted = curatorFrameworkOp.checkExists(completedPath);
										if (isItemCompleted) {
											long thisCompleteMtime = curatorFrameworkOp.getMtime(completedPath);
											if (thisCompleteMtime > nextFireTimeAfterThis) {
												nextFireTimeAfterThis = thisCompleteMtime;
											}
										}
									}
								}
							}
						}
						// 对比enabled's mtime 和 completed's mtime
						long enabledMtime = curatorFrameworkOp.getMtime(enabledPath);
						if (enabledMtime > nextFireTimeAfterThis) {
							nextFireTimeAfterThis = enabledMtime;
						}
						return getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(nextFireTimeAfterThis, jobName,
								curatorFrameworkOp);
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

	/**
	 * 该时间是否在作业暂停时间段范围内。
	 * <p>
	 * 特别的，无论pausePeriodDate，还是pausePeriodTime，如果解析发生异常，则忽略该节点，视为没有配置该日期或时分段。
	 *
	 * @return 该时间是否在作业暂停时间段范围内。
	 */
	private static boolean isInPausePeriod(Date date, String pausePeriodDate, String pausePeriodTime,
			TimeZone timeZone) {
		Calendar calendar = Calendar.getInstance(timeZone);
		calendar.setTime(date);
		int M = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH begin from 0.
		int d = calendar.get(Calendar.DAY_OF_MONTH);
		int h = calendar.get(Calendar.HOUR_OF_DAY);
		int m = calendar.get(Calendar.MINUTE);

		boolean dateIn = false;
		boolean pausePeriodDateIsEmpty = (pausePeriodDate == null || pausePeriodDate.trim().isEmpty());
		if (!pausePeriodDateIsEmpty) {
			String[] periodsDate = pausePeriodDate.split(",");
			if (periodsDate != null) {
				for (String period : periodsDate) {
					String[] tmp = period.trim().split("-");
					if (tmp != null && tmp.length == 2) {
						String left = tmp[0].trim();
						String right = tmp[1].trim();
						String[] MdLeft = left.split("/");
						String[] MdRight = right.split("/");
						if (MdLeft != null && MdLeft.length == 2 && MdRight != null && MdRight.length == 2) {
							try {
								int MLeft = Integer.parseInt(MdLeft[0]);
								int dLeft = Integer.parseInt(MdLeft[1]);
								int MRight = Integer.parseInt(MdRight[0]);
								int dRight = Integer.parseInt(MdRight[1]);
								dateIn = (M > MLeft || M == MLeft && d >= dLeft)
										&& (M < MRight || M == MRight && d <= dRight);// NOSONAR
								if (dateIn) {
									break;
								}
							} catch (NumberFormatException e) {
								dateIn = false;
								break;
							}
						} else {
							dateIn = false;
							break;
						}
					} else {
						dateIn = false;
						break;
					}
				}
			}
		}
		boolean timeIn = false;
		boolean pausePeriodTimeIsEmpty = (pausePeriodTime == null || pausePeriodTime.trim().isEmpty());
		if (!pausePeriodTimeIsEmpty) {
			String[] periodsTime = pausePeriodTime.split(",");
			if (periodsTime != null) {
				for (String period : periodsTime) {
					String[] tmp = period.trim().split("-");
					if (tmp != null && tmp.length == 2) {
						String left = tmp[0].trim();
						String right = tmp[1].trim();
						String[] hmLeft = left.split(":");
						String[] hmRight = right.split(":");
						if (hmLeft != null && hmLeft.length == 2 && hmRight != null && hmRight.length == 2) {
							try {
								int hLeft = Integer.parseInt(hmLeft[0]);
								int mLeft = Integer.parseInt(hmLeft[1]);
								int hRight = Integer.parseInt(hmRight[0]);
								int mRight = Integer.parseInt(hmRight[1]);
								timeIn = (h > hLeft || h == hLeft && m >= mLeft)
										&& (h < hRight || h == hRight && m <= mRight);// NOSONAR
								if (timeIn) {
									break;
								}
							} catch (NumberFormatException e) {
								timeIn = false;
								break;
							}
						} else {
							timeIn = false;
							break;
						}
					} else {
						timeIn = false;
						break;
					}
				}
			}
		}

		if (pausePeriodDateIsEmpty) {
			if (pausePeriodTimeIsEmpty) {
				return false;
			} else {
				return timeIn;
			}
		} else {
			if (pausePeriodTimeIsEmpty) {
				return dateIn;
			} else {
				return dateIn && timeIn;
			}
		}
	}

	@Override
	public Long getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(long nextFireTimeAfterThis, String jobName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String cronPath = JobNodePath.getConfigNodePath(jobName, "cron");
		String cronVal = curatorFrameworkOp.getData(cronPath);
		CronExpression cronExpression = null;
		try {
			cronExpression = new CronExpression(cronVal);
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			return null;
		}
		String timeZoneStr = getTimeZone(jobName, curatorFrameworkOp);
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
		cronExpression.setTimeZone(timeZone);

		Date nextFireTime = cronExpression.getTimeAfter(new Date(nextFireTimeAfterThis));
		String pausePeriodDatePath = JobNodePath.getConfigNodePath(jobName, "pausePeriodDate");
		String pausePeriodDate = curatorFrameworkOp.getData(pausePeriodDatePath);
		String pausePeriodTimePath = JobNodePath.getConfigNodePath(jobName, "pausePeriodTime");
		String pausePeriodTime = curatorFrameworkOp.getData(pausePeriodTimePath);

		while (nextFireTime != null && isInPausePeriod(nextFireTime, pausePeriodDate, pausePeriodTime, timeZone)) {
			nextFireTime = cronExpression.getTimeAfter(nextFireTime);
		}
		if (null == nextFireTime) {
			return null;
		}
		return nextFireTime.getTime();
	}

	@Override
	public List<String> getAllJobGroups() {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		List<String> jobNames = new ArrayList<>();
		try {
			jobNames = getAllUnSystemJobs(curatorFrameworkOp);
		} catch (SaturnJobConsoleException e) {
			log.error(e.getMessage(), e);
		}
		List<String> result = new ArrayList<>(jobNames.size());
		for (String jobName : jobNames) {
			String groups = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "groups"));
			if (Strings.isNullOrEmpty(groups) || result.contains(groups)) {
				continue;
			}
			result.add(groups);
		}
		return result;
	}

	@Override
	public JobMigrateInfo getAllJobMigrateInfo() throws SaturnJobConsoleException {
		JobMigrateInfo jobMigrateInfo = new JobMigrateInfo();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();

		List<String> tasksMigrateEnabled = new ArrayList<>();
		List<String> tasks = new ArrayList<>();
		String dcosTasksNodePath = ContainerNodePath.getDcosTasksNodePath();
		if (curatorFrameworkOp.checkExists(dcosTasksNodePath)) {
			tasks = curatorFrameworkOp.getChildren(dcosTasksNodePath);
		}
		if (tasks != null && !tasks.isEmpty()) {
			for (String tmp : tasks) {
				tasksMigrateEnabled.add(tmp);
			}
		}

		jobMigrateInfo.setTasksMigrateEnabled(tasksMigrateEnabled);
		return jobMigrateInfo;
	}

	@Override
	public void batchMigrateJobNewTask(String jobNames, String taskNew) throws SaturnJobConsoleException {
		for (String jobName : jobNames.split(",")) {
			List<String> preferTasks = getPreferTasksByJobName(jobName);
			if (preferTasks.size() == 0) {
				throw new SaturnJobConsoleException(jobName + ":The job has not set a docker task");
			}
			if (preferTasks.contains(taskNew)) {
				throw new SaturnJobConsoleException(jobName + ":The taskNew is equals to current task");
			}
		}

		for (String jobName : jobNames.split(",")) {
			try {
				this.migrateJobNewTask(jobName, taskNew);
			} catch (SaturnJobConsoleException e) {
				throw new SaturnJobConsoleException(jobName + ":" + e.getMessage());
			}
		}
	}

	@Override
	public void batchSetPreferExecutors(String jobNames, String executorListStr) throws SaturnJobConsoleException {
		for (String jobName : jobNames.split(",")) {
			try {
				this.setPreferExecutors(jobName, executorListStr);
			} catch (SaturnJobConsoleException e) {
				throw new SaturnJobConsoleException(jobName + ":" + e.getMessage());
			}
		}
	}

	@Transactional
	@Override
	public void setPreferExecutors(String jobName, String executorListStr) throws SaturnJobConsoleException {
		try {
			String jobConfigPreferListNodePath = SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName);
			savePreferList2DBAndZK(jobName, executorListStr, curatorRepository.inSessionClient(),
					jobConfigPreferListNodePath);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
	}

	private void savePreferList2DBAndZK(String jobName, String executorListStr,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobConfigPreferListNodePath)
			throws SaturnJobConsoleException {
		savePreferListToDb(jobName, curatorFrameworkOp, executorListStr);
		curatorFrameworkOp.update(jobConfigPreferListNodePath, executorListStr);
		// delete and create the forceShard node
		String jobConfigForceShardNodePath = SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName);
		curatorFrameworkOp.delete(jobConfigForceShardNodePath);
		curatorFrameworkOp.create(jobConfigForceShardNodePath);
	}

	private List<String> getPreferTasksByJobName(String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();

		List<String> tasks = new ArrayList<>();
		String dcosTasksNodePath = ContainerNodePath.getDcosTasksNodePath();
		if (curatorFrameworkOp.checkExists(dcosTasksNodePath)) {
			tasks = curatorFrameworkOp.getChildren(dcosTasksNodePath);
		}
		List<String> preferTasks = new ArrayList<>();
		if (tasks != null && !tasks.isEmpty()) {
			String preferListNodePath = JobNodePath.getConfigNodePath(jobName, "preferList");
			if (curatorFrameworkOp.checkExists(preferListNodePath)) {
				String preferList = curatorFrameworkOp.getData(preferListNodePath);
				if (preferList != null) {
					String[] split = preferList.split(",");
					for (int i = 0; i < split.length; i++) {
						String prefer = split[i].trim();
						if (prefer.startsWith("@")) {
							preferTasks.add(prefer.substring(1));
						}
					}
				}
			}
		}

		return preferTasks;
	}
}
