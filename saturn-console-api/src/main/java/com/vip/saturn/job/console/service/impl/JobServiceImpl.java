package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.vip.saturn.job.console.domain.ExecutorProvided;
import com.vip.saturn.job.console.domain.ExecutorProvidedType;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.ContainerNodePath;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author hebelala
 */
@Service
public class JobServiceImpl implements JobService {

	private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private CurrentJobConfigService currentJobConfigService;

	private MapperFacade mapper;

	@Autowired
	public void setMapperFactory(MapperFactory mapperFactory) {
		this.mapper = mapperFactory.getMapperFacade();
	}

	@Override
	public List<JobInfo> jobs(String namespace) throws SaturnJobConsoleException {
		List<JobInfo> list = new ArrayList<>();
		try {
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
					.getCuratorFrameworkOp(namespace);
			List<CurrentJobConfig> jobConfigList = currentJobConfigService.findConfigsByNamespace(namespace);
			if (jobConfigList != null) {
				for (CurrentJobConfig jobConfig : jobConfigList) {
					try {
						if (isSystemJob(jobConfig)) {
							continue;
						}
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
	public List<String> groups(String namespace) throws SaturnJobConsoleException {
		List<String> groups = new ArrayList<>();
		List<CurrentJobConfig> jobConfigList = currentJobConfigService.findConfigsByNamespace(namespace);
		if (jobConfigList != null) {
			for (CurrentJobConfig jobConfig : jobConfigList) {
				if (isSystemJob(jobConfig)) {
					continue;
				}
				String jobGroups = jobConfig.getGroups();
				if (jobGroups != null && !groups.contains(jobGroups)) {
					groups.add(jobGroups);
				}
			}
		}
		return groups;
	}

	@Override
	public List<DependencyJob> dependingJobs(String namespace, String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependencyJobs = new ArrayList<>();
		CurrentJobConfig currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (currentJobConfig == null) {
			throw new SaturnJobConsoleException("不能获取该作业（" + jobName + "）依赖的所有作业，因为该作业不存在");
		}
		List<CurrentJobConfig> jobConfigList = currentJobConfigService.findConfigsByNamespace(namespace);
		if (jobConfigList != null) {
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
				for (CurrentJobConfig jobConfig : jobConfigList) {
					if (isSystemJob(jobConfig)) {
						continue;
					}
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
	public List<DependencyJob> dependedJobs(String namespace, String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependencyJobs = new ArrayList<>();
		CurrentJobConfig currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (currentJobConfig == null) {
			throw new SaturnJobConsoleException("不能获取依赖该作业（" + jobName + "）的所有作业，因为该作业不存在");
		}
		List<CurrentJobConfig> jobConfigList = currentJobConfigService.findConfigsByNamespace(namespace);
		if (jobConfigList != null) {
			for (CurrentJobConfig jobConfig : jobConfigList) {
				if (isSystemJob(jobConfig)) {
					continue;
				}
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

}
