package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.vip.saturn.job.console.domain.ExecutorProvided;
import com.vip.saturn.job.console.domain.ExecutorProvidedType;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.*;
import com.vip.saturn.job.console.vo.*;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @author hebelala
 */
@Service
public class JobServiceImpl implements JobService {

	private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

	private static final int DEFAULT_MAX_JOB_NUM = 100;

	private static final int DEFAULT_INTERVAL_TIME_OF_ENABLED_REPORT = 5;

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private CurrentJobConfigService currentJobConfigService;

	@Resource
	private SystemConfigService systemConfigService;

	private MapperFacade mapper;

	@Autowired
	public void setMapperFactory(MapperFactory mapperFactory) {
		this.mapper = mapperFactory.getMapperFacade();
	}

	@Override
	public List<JobInfo> getJobs(String namespace) throws SaturnJobConsoleException {
		List<JobInfo> list = new ArrayList<>();
		try {
			List<CurrentJobConfig> unSystemJobs = getUnSystemJobs(namespace);
			if (unSystemJobs != null) {
				CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
						.getCuratorFrameworkOp(namespace);
				for (CurrentJobConfig jobConfig : unSystemJobs) {
					try {
						JobInfo jobInfo = new JobInfo();
						jobInfo.setJobName(jobConfig.getJobName());
						jobInfo.setDescription(jobConfig.getDescription());
						jobInfo.setJobClass(jobConfig.getJobClass());
						jobInfo.setJobType(JobType.getJobType(jobConfig.getJobType()));
						if (JobType.UNKOWN_JOB.equals(jobInfo.getJobType())) {
							if (jobInfo.getJobClass() != null
									&& jobInfo.getJobClass().indexOf("SaturnScriptJob") != -1) {
								jobInfo.setJobType(JobType.SHELL_JOB);
							} else {
								jobInfo.setJobType(JobType.JAVA_JOB);
							}
						}
						jobInfo.setJobEnabled(jobConfig.getEnabled());
						jobInfo.setStatus(
								getJobStatus(jobConfig.getJobName(), curatorFrameworkOp, jobConfig.getEnabled()));
						jobInfo.setJobParameter(jobConfig.getJobParameter());
						jobInfo.setShardingItemParameters(jobConfig.getShardingItemParameters());
						jobInfo.setQueueName(jobConfig.getQueueName());
						jobInfo.setChannelName(jobConfig.getChannelName());
						jobInfo.setLoadLevel(String.valueOf(jobConfig.getLoadLevel()));
						String jobDegree =
								jobConfig.getJobDegree() == null ? "0" : String.valueOf(jobConfig.getJobDegree());
						jobInfo.setJobDegree(jobDegree);
						jobInfo.setShardingTotalCount(String.valueOf(jobConfig.getShardingTotalCount()));

						if (jobConfig.getTimeout4AlarmSeconds() == null) {
							jobInfo.setTimeout4AlarmSeconds(0);
						} else {
							jobInfo.setTimeout4AlarmSeconds(jobConfig.getTimeout4AlarmSeconds());
						}
						jobInfo.setTimeoutSeconds(jobConfig.getTimeoutSeconds());
						jobInfo.setPausePeriodDate(jobConfig.getPausePeriodDate());
						jobInfo.setPausePeriodTime(jobConfig.getPausePeriodTime());
						jobInfo.setShowNormalLog(jobConfig.getShowNormalLog());
						jobInfo.setLocalMode(jobConfig.getLocalMode());
						jobInfo.setUseSerial(jobConfig.getUseSerial());
						jobInfo.setUseDispreferList(jobConfig.getUseDispreferList());
						jobInfo.setProcessCountIntervalSeconds(jobConfig.getProcessCountIntervalSeconds());
						jobInfo.setGroups(jobConfig.getGroups());
						String preferList = jobConfig.getPreferList();
						jobInfo.setPreferList(preferList);
						if (!StringUtils.isBlank(preferList)) {
							String containerTaskIdsNodePath = ContainerNodePath.getDcosTasksNodePath();
							List<String> containerTaskIds = curatorFrameworkOp.getChildren(containerTaskIdsNodePath);
							jobInfo.setMigrateEnabled(isMigrateEnabled(preferList, containerTaskIds));
						} else {
							jobInfo.setMigrateEnabled(false);
						}
						String timeZone = jobConfig.getTimeZone();
						if (Strings.isNullOrEmpty(timeZone)) {
							jobInfo.setTimeZone(SaturnConstants.TIME_ZONE_ID_DEFAULT);
						} else {
							jobInfo.setTimeZone(timeZone);
						}
						jobInfo.setCron(jobConfig.getCron());

						updateJobInfoStatus(curatorFrameworkOp, jobConfig.getJobName(), jobInfo);

						list.add(jobInfo);
					} catch (Exception e) {
						log.error("list job " + jobConfig.getJobName() + " error", e);
					}
				}
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}

		return list;
	}

	private boolean isSystemJob(CurrentJobConfig jobConfig) {
		return StringUtils.isNotBlank(jobConfig.getJobMode()) && jobConfig.getJobMode()
				.startsWith(JobMode.SYSTEM_PREFIX);
	}

	private JobStatus getJobStatus(final String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			boolean enabled) {
		// see if all the shards is finished.
		boolean isAllShardsFinished = isAllShardsFinished(jobName, curatorFrameworkOp);
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

	private void updateJobInfoStatus(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName,
			JobInfo jobInfo) {
		if (JobStatus.STOPPED.equals(jobInfo.getStatus())) {// 作业如果是STOPPED状态，不需要显示已分配的executor
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
			jobInfo.setShardingList(shardingListSb.substring(0, shardingListSb.length() - 1));
		}
	}

	@Override
	public List<String> getGroups(String namespace) throws SaturnJobConsoleException {
		List<String> groups = new ArrayList<>();
		List<CurrentJobConfig> unSystemJobs = getUnSystemJobs(namespace);
		if (unSystemJobs != null) {
			for (CurrentJobConfig jobConfig : unSystemJobs) {
				String jobGroups = jobConfig.getGroups();
				if (jobGroups != null && !groups.contains(jobGroups)) {
					groups.add(jobGroups);
				}
			}
		}
		return groups;
	}

	@Override
	public List<DependencyJob> getDependingJobs(String namespace, String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependencyJobs = new ArrayList<>();
		CurrentJobConfig currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (currentJobConfig == null) {
			throw new SaturnJobConsoleException("不能获取该作业（" + jobName + "）依赖的所有作业，因为该作业不存在");
		}
		List<CurrentJobConfig> unSystemJobs = getUnSystemJobs(namespace);
		if (unSystemJobs != null) {
			String dependencies = currentJobConfig.getDependencies();
			List<String> dependencyList = new ArrayList<>();
			if (StringUtils.isNotBlank(dependencies)) {
				String[] split = dependencies.split(",");
				for (String tmp : split) {
					if (StringUtils.isNotBlank(tmp)) {
						dependencyList.add(tmp.trim());
					}
				}
			}
			if (!dependencyList.isEmpty()) {
				for (CurrentJobConfig jobConfig : unSystemJobs) {
					if (jobConfig.getJobName().equals(jobName)) {
						continue;
					}
					if (dependencyList.contains(jobConfig.getJobName())) {
						DependencyJob dependencyJob = new DependencyJob();
						dependencyJob.setJobName(jobConfig.getJobName());
						dependencyJob.setEnabled(jobConfig.getEnabled());
						dependencyJobs.add(dependencyJob);
					}
				}
			}
		}
		return dependencyJobs;
	}

	@Override
	public List<DependencyJob> getDependedJobs(String namespace, String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependencyJobs = new ArrayList<>();
		CurrentJobConfig currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (currentJobConfig == null) {
			throw new SaturnJobConsoleException("不能获取依赖该作业（" + jobName + "）的所有作业，因为该作业不存在");
		}
		List<CurrentJobConfig> unSystemJobs = getUnSystemJobs(namespace);
		if (unSystemJobs != null) {
			for (CurrentJobConfig jobConfig : unSystemJobs) {
				if (jobConfig.getJobName().equals(jobName)) {
					continue;
				}
				String dependencies = jobConfig.getDependencies();
				if (StringUtils.isNotBlank(dependencies)) {
					String[] split = dependencies.split(",");
					for (String tmp : split) {
						if (jobName.equals(tmp.trim())) {
							DependencyJob dependencyJob = new DependencyJob();
							dependencyJob.setJobName(jobConfig.getJobName());
							dependencyJob.setEnabled(jobConfig.getEnabled());
							dependencyJobs.add(dependencyJob);
						}
					}
				}
			}
		}
		return dependencyJobs;
	}

	@Transactional
	@Override
	public void enableJob(String namespace, String jobName) throws SaturnJobConsoleException {
		CurrentJobConfig jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig == null) {
			throw new SaturnJobConsoleException("不能删除该作业（" + jobName + "），因为该作业不存在");
		}
		if (jobConfig.getEnabled()) {
			throw new SaturnJobConsoleException("该作业（" + jobName + "）已经处于启用状态");
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		boolean allShardsFinished = isAllShardsFinished(jobName, curatorFrameworkOp);
		if (!allShardsFinished) {
			throw new SaturnJobConsoleException("不能启用该作业（" + jobName + "），因为该作业不处于STOPPED状态");
		}
		jobConfig.setEnabled(true);
		jobConfig.setLastUpdateTime(new Date());
		try {
			currentJobConfigService.updateByPrimaryKey(jobConfig);
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
		curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, "enabled"), true);
	}

	@Transactional
	@Override
	public void disableJob(String namespace, String jobName) throws SaturnJobConsoleException {
		CurrentJobConfig jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig == null) {
			throw new SaturnJobConsoleException("不能禁用该作业（" + jobName + "），因为该作业不存在");
		}
		if (!jobConfig.getEnabled()) {
			throw new SaturnJobConsoleException("该作业（" + jobName + "）已经处于禁用状态");
		}
		jobConfig.setEnabled(false);
		jobConfig.setLastUpdateTime(new Date());
		try {
			currentJobConfigService.updateByPrimaryKey(jobConfig);
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, "enabled"), false);
	}

	@Override
	public void removeJob(String namespace, String jobName) throws SaturnJobConsoleException {
		CurrentJobConfig jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig == null) {
			throw new SaturnJobConsoleException("不能删除该作业（" + jobName + "），因为该作业不存在");
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		JobStatus jobStatus = getJobStatus(jobName, curatorFrameworkOp, jobConfig.getEnabled());
		if (JobStatus.STOPPED.equals(jobStatus)) {
			Stat stat = curatorFrameworkOp.getStat(JobNodePath.getJobNodePath(jobName));
			if (stat != null) {
				long createTimeDiff = System.currentTimeMillis() - stat.getCtime();
				if (createTimeDiff < SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT) {
					throw new SaturnJobConsoleException(
							"不能删除该作业（" + jobName + "），因为该作业创建时间距离现在不超过" + (SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT
									/ 6000) + "分钟");
				}
			}
			// remove job from db
			try {
				currentJobConfigService.deleteByPrimaryKey(jobConfig.getId());
			} catch (Exception e) {
				throw new SaturnJobConsoleException(e);
			}
			// remove job from zk
			// 1.作业的executor全online的情况，添加toDelete节点，触发监听器动态删除节点
			String toDeleteNodePath = JobNodePath.getConfigNodePath(jobName, "toDelete");
			if (curatorFrameworkOp.checkExists(toDeleteNodePath)) {
				curatorFrameworkOp.deleteRecursive(toDeleteNodePath);
			}
			curatorFrameworkOp.create(toDeleteNodePath);

			for (int i = 0; i < 20; i++) {
				// 2.作业的executor全offline的情况，或有几个online，几个offline的情况
				String jobServerPath = JobNodePath.getServerNodePath(jobName);
				if (!curatorFrameworkOp.checkExists(jobServerPath)) {
					// (1)如果不存在$Job/JobName/servers节点，说明该作业没有任何executor接管，可直接删除作业节点
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
				}
				// (2)如果该作业servers下没有任何executor，可直接删除作业节点
				List<String> executors = curatorFrameworkOp.getChildren(jobServerPath);
				if (CollectionUtils.isEmpty(executors)) {
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
				}
				// (3)只要该作业没有一个能运行的该作业的executor在线，那么直接删除作业节点
				boolean hasOnlineExecutor = false;
				for (String executor : executors) {
					if (curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executor, "ip"))
							&& curatorFrameworkOp.checkExists(JobNodePath.getServerStatus(jobName, executor))) {
						hasOnlineExecutor = true;
					} else {
						curatorFrameworkOp.deleteRecursive(JobNodePath.getServerNodePath(jobName, executor));
					}
				}
				if (!hasOnlineExecutor) {
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					throw new SaturnJobConsoleException(e);
				}
			}
		} else {
			throw new SaturnJobConsoleException("不能删除该作业（" + jobName + "），因为该作业不处于STOPPED状态");
		}
	}

	@Override
	public List<ExecutorProvided> getExecutors(String namespace, String jobName) throws SaturnJobConsoleException {
		List<ExecutorProvided> executorProvidedList = new ArrayList<>();
		CurrentJobConfig currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (currentJobConfig == null) {
			throw new SaturnJobConsoleException("不能获取该作业（" + jobName + "）可选择的优先Executor，因为该作业不存在");
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String executorsNodePath = SaturnExecutorsNode.getExecutorsNodePath();
		if (!curatorFrameworkOp.checkExists(executorsNodePath)) {
			return executorProvidedList;
		}
		List<String> executors = curatorFrameworkOp.getChildren(executorsNodePath);
		if (executors != null && executors.size() > 0) {
			for (String executor : executors) {
				if (curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorTaskNodePath(executor))) {
					continue;// 过滤容器中的Executor，容器资源只需要可以选择taskId即可
				}
				ExecutorProvided executorProvided = new ExecutorProvided();
				executorProvided.setExecutorName(executor);
				executorProvided.setNoTraffic(
						curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorNoTrafficNodePath(executor)));
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

		if (StringUtils.isNotBlank(jobName)) {
			String preferListNodePath = JobNodePath.getConfigNodePath(jobName, "preferList");
			if (curatorFrameworkOp.checkExists(preferListNodePath)) {
				String preferList = curatorFrameworkOp.getData(preferListNodePath);
				if (!Strings.isNullOrEmpty(preferList)) {
					String[] preferExecutorList = preferList.split(",");
					for (String preferExecutor : preferExecutorList) {
						if (executors != null && !executors.contains(preferExecutor) && !preferExecutor
								.startsWith("@")) {
							ExecutorProvided executorProvided = new ExecutorProvided();
							executorProvided.setExecutorName(preferExecutor);
							executorProvided.setType(ExecutorProvidedType.DELETED);
							executorProvided.setNoTraffic(curatorFrameworkOp
									.checkExists(SaturnExecutorsNode.getExecutorNoTrafficNodePath(preferExecutor)));
							executorProvidedList.add(executorProvided);
						}
					}
				}
			}
		}
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
			for (String task : containerTaskIds) {
				ExecutorProvided executorProvided = new ExecutorProvided();
				executorProvided.setExecutorName(task);
				executorProvided.setType(ExecutorProvidedType.DOCKER);
				executorProvidedList.add(executorProvided);
			}
		}

		return executorProvidedList;
	}

	private List<String> getDCOSContainerTaskIds(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		List<String> containerTaskIds = Lists.newArrayList();

		String containerNodePath = ContainerNodePath.getDcosTasksNodePath();
		if (curatorFrameworkOp.checkExists(containerNodePath)) {
			containerTaskIds = curatorFrameworkOp.getChildren(containerNodePath);
		}

		return containerTaskIds;
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
	public List<ExecutorProvided> getOnlineExecutors(String namespace) throws SaturnJobConsoleException {
		List<ExecutorProvided> executorProvidedList = new ArrayList<>();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String executorsNodePath = SaturnExecutorsNode.getExecutorsNodePath();
		if (!curatorFrameworkOp.checkExists(executorsNodePath)) {
			return executorProvidedList;
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
					executorProvided.setNoTraffic(
							curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorNoTrafficNodePath(executor)));
					executorProvided.setType(ExecutorProvidedType.ONLINE);
					executorProvidedList.add(executorProvided);
					continue;
				}
			}
		}

		executorProvidedList.addAll(getContainerTaskIds(curatorFrameworkOp));

		return executorProvidedList;
	}

	@Override
	public void setPreferList(String namespace, String jobName, String preferList) throws SaturnJobConsoleException {
		// save to db
		CurrentJobConfig oldJobConfig = currentJobConfigService
				.findConfigByNamespaceAndJobName(namespace, jobName);
		if (oldJobConfig == null) {
			throw new SaturnJobConsoleException("设置该作业（" + jobName + "）优先Executor失败，因为该作业不存在");
		}
		CurrentJobConfig newJobConfig = mapper.map(oldJobConfig, CurrentJobConfig.class);
		newJobConfig.setPreferList(preferList);
		try {
			currentJobConfigService.updateConfigAndSave2History(newJobConfig, oldJobConfig, null);
		} catch (Exception e) {
			log.error("exception is thrown during change preferList in db", e);
			throw new SaturnJobConsoleException(e);
		}

		// save to zk
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String jobConfigPreferListNodePath = SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName);
		curatorFrameworkOp.update(jobConfigPreferListNodePath, preferList);
		// delete and create the forceShard node
		String jobConfigForceShardNodePath = SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName);
		curatorFrameworkOp.delete(jobConfigForceShardNodePath);
		curatorFrameworkOp.create(jobConfigForceShardNodePath);
	}

	@Override
	public void validateJobConfig(JobConfig jobConfig) throws SaturnJobConsoleException {
		// 作业名必填
		if (jobConfig.getJobName() == null || jobConfig.getJobName().trim().isEmpty()) {
			throw new SaturnJobConsoleException("作业名必填");
		}
		// 作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_
		if (!jobConfig.getJobName().matches("[0-9a-zA-Z_]*")) {
			throw new SaturnJobConsoleException("作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_");
		}
		// 依赖的作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,
		if (jobConfig.getDependencies() != null && !jobConfig.getDependencies().matches("[0-9a-zA-Z_,]*")) {
			throw new SaturnJobConsoleException("依赖的作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,");
		}
		// 作业类型必填
		if (jobConfig.getJobType() == null || jobConfig.getJobType().trim().isEmpty()) {
			throw new SaturnJobConsoleException("作业类型必填");
		}
		// 验证作业类型
		if (JobType.getJobType(jobConfig.getJobType()).equals(JobType.UNKOWN_JOB)) {
			throw new SaturnJobConsoleException("作业类型未知");
		}
		// 如果是JAVA作业
		if (jobConfig.getJobType().equals(JobType.JAVA_JOB.name())) {
			// 作业实现类必填
			if (jobConfig.getJobClass() == null || jobConfig.getJobClass().trim().isEmpty()) {
				throw new SaturnJobConsoleException("对于JAVA作业，作业实现类必填");
			}
		}
		// 如果是JAVA/SHELL作业
		if (jobConfig.getJobType().equals(JobType.JAVA_JOB.name())
				|| jobConfig.getJobType().equals(JobType.SHELL_JOB.name())) {
			// cron表达式必填
			if (jobConfig.getCron() == null || jobConfig.getCron().trim().isEmpty()) {
				throw new SaturnJobConsoleException("对于JAVA/SHELL作业，cron表达式必填");
			}
			// cron表达式语法验证
			try {
				CronExpression.validateExpression(jobConfig.getCron());
			} catch (ParseException e) {
				throw new SaturnJobConsoleException("cron表达式语法有误，" + e.toString());
			}
		} else {
			jobConfig.setCron(""); // 其他类型的不需要持久化保存cron表达式
		}
		if (jobConfig.getLocalMode() != null && jobConfig.getLocalMode()) {
			if (jobConfig.getShardingItemParameters() == null) {
				throw new SaturnJobConsoleException("对于本地模式作业，分片参数必填。");
			} else {
				String[] split = jobConfig.getShardingItemParameters().split(",");
				boolean includeXing = false;
				for (String tmp : split) {
					String[] split2 = tmp.split("=");
					if ("*".equalsIgnoreCase(split2[0].trim())) {
						includeXing = true;
						break;
					}
				}
				if (!includeXing) {
					throw new SaturnJobConsoleException("对于本地模式作业，分片参数必须包含如*=xx。");
				}
			}
		} else {
			// 分片参数不能小于分片总数
			if (jobConfig.getShardingTotalCount() == null || jobConfig.getShardingTotalCount() < 1) {
				throw new SaturnJobConsoleException("分片数不能为空，并且不能小于1");
			}
			if (jobConfig.getShardingTotalCount() > 0) {
				if (jobConfig.getShardingItemParameters() == null
						|| jobConfig.getShardingItemParameters().trim().isEmpty()
						|| jobConfig.getShardingItemParameters().split(",").length < jobConfig
						.getShardingTotalCount()) {
					throw new SaturnJobConsoleException("分片参数不能小于分片总数");
				}
			}
		}
		// 不能添加系统作业
		if (jobConfig.getJobMode() != null && jobConfig.getJobMode().startsWith(JobMode.SYSTEM_PREFIX)) {
			throw new SaturnJobConsoleException("作业模式有误，不能添加系统作业");
		}
	}

	@Override
	public void addJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException {
		validateJobConfig(jobConfig);
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String jobName = jobConfig.getJobName();
		if (!curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))) {
			int maxJobNum = getMaxJobNum();
			if (jobIncExceeds(namespace, maxJobNum, 1)) {
				throw new SaturnJobConsoleException(String.format("总作业数超过最大限制(%d)，作业名%s创建失败", maxJobNum, jobName));
			} else {
				if (jobConfig.getIsCopyJob()) {
					persistJobCopied(namespace, jobConfig);
				} else {
					persistJob(namespace, jobConfig);
				}
			}
		} else {
			throw new SaturnJobConsoleException(String.format("作业名%s已经存在", jobName));
		}
	}

	@Override
	public int getMaxJobNum() {
		int result = systemConfigService.getIntegerValue(SystemConfigProperties.MAX_JOB_NUM, DEFAULT_MAX_JOB_NUM);
		return result <= 0 ? DEFAULT_MAX_JOB_NUM : result;
	}

	@Override
	public boolean jobIncExceeds(String namespace, int maxJobNum, int inc) throws SaturnJobConsoleException {
		if (maxJobNum <= 0) {
			return false;
		}
		int curJobSize = getUnSystemJobs(namespace).size();
		return (curJobSize + inc) > maxJobNum;
	}

	@Override
	public List<CurrentJobConfig> getUnSystemJobs(String namespace) throws SaturnJobConsoleException {
		List<CurrentJobConfig> unSystemJobs = new ArrayList<>();
		List<CurrentJobConfig> jobConfigList = currentJobConfigService.findConfigsByNamespace(namespace);
		if (jobConfigList != null) {
			for (CurrentJobConfig jobConfig : jobConfigList) {
				if (!isSystemJob(jobConfig)) {
					unSystemJobs.add(jobConfig);
				}
			}
		}
		return unSystemJobs;
	}

	@Transactional
	@Override
	public void persistJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		if (curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobConfig.getJobName()))) {
			curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobConfig.getJobName()));
		}
		correctConfigValueIfNeeded(jobConfig);
		saveJobConfigToDb(namespace, jobConfig);
		saveJobConfigToZkWhenPersist(jobConfig, curatorFrameworkOp, false);
	}

	/**
	 * 对作业配置的一些属性进行矫正
	 */
	private void correctConfigValueIfNeeded(JobConfig jobConfig) {
		jobConfig.setDefaultValues();
		jobConfig.setEnabled(false);
		jobConfig.setFailover(jobConfig.getLocalMode() == false);
		if (JobType.SHELL_JOB.name().equals(jobConfig.getJobType())) {
			jobConfig.setJobClass("");
		}
		jobConfig.setEnabledReport(
				getEnabledReport(jobConfig.getJobType(), jobConfig.getCron(), jobConfig.getTimeZone()));
	}

	/**
	 * 对于定时作业，根据cron和INTERVAL_TIME_OF_ENABLED_REPORT来计算是否需要上报状态 see #286
	 */
	private boolean getEnabledReport(String jobType, String cron, String timeZone) {
		boolean enabledReport = true;
		if (jobType.equals(JobType.JAVA_JOB.name()) || jobType.equals(JobType.SHELL_JOB.name())) {
			try {
				Integer intervalTimeConfigured = systemConfigService
						.getIntegerValue(SystemConfigProperties.INTERVAL_TIME_OF_ENABLED_REPORT,
								DEFAULT_INTERVAL_TIME_OF_ENABLED_REPORT);
				if (intervalTimeConfigured == null) {
					log.warn("unexpected error, get INTERVAL_TIME_OF_ENABLED_REPORT null");
					intervalTimeConfigured = DEFAULT_INTERVAL_TIME_OF_ENABLED_REPORT;
				}
				CronExpression cronExpression = new CronExpression(cron);
				cronExpression.setTimeZone(TimeZone.getTimeZone(timeZone));
				Date lastNextTime = cronExpression.getNextValidTimeAfter(new Date());
				if (lastNextTime != null) {
					for (int i = 0; i < 5; i++) {
						Date nextTime = cronExpression.getNextValidTimeAfter(lastNextTime);
						if (nextTime == null) {
							break;
						}
						long interval = nextTime.getTime() - lastNextTime.getTime();
						if (interval < intervalTimeConfigured * 1000) {
							enabledReport = false;
							break;
						}
						lastNextTime = nextTime;
					}
				}
			} catch (ParseException e) {
				log.warn(e.getMessage(), e);
			}
		} else {
			enabledReport = false;
		}
		return enabledReport;
	}

	private void saveJobConfigToDb(String namespace, JobConfig jobConfig)
			throws SaturnJobConsoleException {
		String jobName = jobConfig.getJobName();
		CurrentJobConfig oldJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (oldJobConfig != null) {
			log.warn(
					"when create a new job, a jobConfig with the same name from db exists, will delete it first. namespace:{} and jobName:{}",
					namespace, jobName);
			try {
				currentJobConfigService.deleteByPrimaryKey(oldJobConfig.getId());
			} catch (Exception e) {
				log.error("exception is thrown during delete job config in db", e);
				throw new SaturnJobConsoleException("创建作业时，数据库存在已经存在该作业的相关配置！并且清理该配置的时候失败", e);
			}
		}
		CurrentJobConfig currentJobConfig = new CurrentJobConfig();
		mapper.map(jobConfig, currentJobConfig);
		currentJobConfig.setCreateTime(new Date());
		currentJobConfig.setLastUpdateTime(new Date());
		currentJobConfig.setNamespace(namespace);
		try {
			currentJobConfigService.create(currentJobConfig);
		} catch (Exception e) {
			log.error("exception is thrown during creating job config in db", e);
			throw new SaturnJobConsoleException(e);
		}
	}

	private void saveJobConfigToZkWhenPersist(JobConfig jobConfig,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, boolean fromDB) {
		String jobName = jobConfig.getJobName();
		if (!fromDB) {
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "enabled"), "false");
		} else {
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "enabled"),
					jobConfig.getEnabled());
		}
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "description"),
				jobConfig.getDescription());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "customContext"),
				jobConfig.getCustomContext());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobType"),
				jobConfig.getJobType());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobMode"),
				jobConfig.getJobMode());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters"),
				jobConfig.getShardingItemParameters());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobParameter"),
				jobConfig.getJobParameter());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "queueName"),
				jobConfig.getQueueName());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "channelName"),
				jobConfig.getChannelName());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "failover"),
				jobConfig.getFailover());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "monitorExecution"), "true");
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "timeout4AlarmSeconds"),
				jobConfig.getTimeout4AlarmSeconds());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"),
				jobConfig.getTimeoutSeconds());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "timeZone"),
				jobConfig.getTimeZone());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "cron"), jobConfig.getCron());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate"),
				jobConfig.getPausePeriodDate());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime"),
				jobConfig.getPausePeriodTime());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"),
				jobConfig.getProcessCountIntervalSeconds());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"),
				jobConfig.getShardingTotalCount());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "showNormalLog"),
				jobConfig.getShowNormalLog());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "loadLevel"),
				jobConfig.getLoadLevel());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobDegree"),
				jobConfig.getJobDegree());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "enabledReport"),
				jobConfig.getEnabledReport());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "preferList"),
				jobConfig.getPreferList());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "useDispreferList"),
				jobConfig.getUseDispreferList());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "localMode"),
				jobConfig.getLocalMode());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "useSerial"),
				jobConfig.getUseSerial());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "dependencies"),
				jobConfig.getDependencies());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "groups"),
				jobConfig.getGroups());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobClass"),
				jobConfig.getJobClass());
	}

	@Override
	public void persistJobCopied(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		if (curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobConfig.getJobName()))) {
			curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobConfig.getJobName()));
		}
		correctConfigValueIfNeeded(jobConfig);
		saveJobConfigToDb(namespace, jobConfig);
		saveJobConfigToZkWhenCopy(jobConfig, curatorFrameworkOp);
	}

	private void saveJobConfigToZkWhenCopy(JobConfig jobConfig, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		String originJobName = jobConfig.getOriginJobName();
		List<String> jobConfigNodes = curatorFrameworkOp.getChildren(JobNodePath.getConfigNodePath(originJobName));
		if (CollectionUtils.isEmpty(jobConfigNodes)) {
			return;
		}
		Class<?> cls = jobConfig.getClass();
		String jobClassPath = "";
		String jobClassValue = "";
		for (String jobConfigNode : jobConfigNodes) {
			String jobConfigPath = JobNodePath.getConfigNodePath(originJobName, jobConfigNode);
			String jobConfigValue = curatorFrameworkOp.getData(jobConfigPath);
			try {
				Field field = cls.getDeclaredField(jobConfigNode);
				field.setAccessible(true);
				Object fieldValue = field.get(jobConfig);
				if (fieldValue != null) {
					jobConfigValue = fieldValue.toString();
				}
				if ("jobClass".equals(jobConfigNode)) {// 持久化jobClass会触发添加作业，待其他节点全部持久化完毕以后再持久化jobClass
					jobClassPath = JobNodePath.getConfigNodePath(jobConfig.getJobName(), jobConfigNode);
					jobClassValue = jobConfigValue;
				}
			} catch (NoSuchFieldException e) {// 即使JobConfig类中不存在该属性也复制（一般是旧版作业的一些节点，可以在旧版Executor上运行）
				continue;
			} catch (IllegalAccessException e) {
				throw new SaturnJobConsoleException(e);
			} finally {
				if (!"jobClass".equals(jobConfigNode)) {// 持久化jobClass会触发添加作业，待其他节点全部持久化完毕以后再持久化jobClass
					String fillJobNodePath = JobNodePath.getConfigNodePath(jobConfig.getJobName(), jobConfigNode);
					curatorFrameworkOp.fillJobNodeIfNotExist(fillJobNodePath, jobConfigValue);
				}
			}
		}
		if (!Strings.isNullOrEmpty(jobClassPath)) {
			curatorFrameworkOp.fillJobNodeIfNotExist(jobClassPath, jobClassValue);
		}
	}

}
