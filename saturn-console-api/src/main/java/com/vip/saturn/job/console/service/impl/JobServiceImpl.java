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
import jxl.Cell;
import jxl.CellType;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.*;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.io.File;
import java.lang.Boolean;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.*;

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

	private Random random = new Random();

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

	@Override
	public void importJobs(String namespace, MultipartFile file) throws SaturnJobConsoleException {
		try {
			Workbook workbook = Workbook.getWorkbook(file.getInputStream());

			Sheet[] sheets = workbook.getSheets();
			List<JobConfig> jobConfigList = new ArrayList<>();
			// 第一行为配置项提示，从第二行开始为作业配置信息
			// 先获取数据并检测内容格式的正确性
			for (int i = 0; i < sheets.length; i++) {
				Sheet sheet = sheets[i];
				int rows = sheet.getRows();
				for (int row = 1; row < rows; row++) {
					Cell[] rowCells = sheet.getRow(row);
					// 如果这一行的表格全为空，则跳过这一行。
					if (!isBlankRow(rowCells)) {
						jobConfigList.add(convertJobConfig(i + 1, row + 1, rowCells));
					}
				}
			}
			int maxJobNum = getMaxJobNum();
			if (jobIncExceeds(namespace, maxJobNum, jobConfigList.size())) {
				throw new SaturnJobConsoleException(String.format("总作业数超过最大限制(%d)，导入失败", maxJobNum));
			}
			// 再进行添加
			for (JobConfig jobConfig : jobConfigList) { // TODO 配合前台提示信息做更好
				addJob(namespace, jobConfig);
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}

	private boolean isBlankRow(Cell[] rowCells) {
		for (int i = 0; i < rowCells.length; i++) {
			if (!CellType.EMPTY.equals(rowCells[i].getType())) {
				return false;
			}
		}
		return true;
	}

	private JobConfig convertJobConfig(int sheetNumber, int rowNumber, Cell[] rowCells)
			throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();

		String jobName = getContents(rowCells, 0);
		if (jobName == null || jobName.trim().isEmpty()) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 1, "作业名必填。"));
		}
		if (!jobName.matches("[0-9a-zA-Z_]*")) {
			throw new SaturnJobConsoleException(
					createExceptionMessage(sheetNumber, rowNumber, 1, "作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_。"));
		}
		jobConfig.setJobName(jobName);

		String jobType = getContents(rowCells, 1);
		if (jobType == null || jobType.trim().isEmpty()) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 2, "作业类型必填。"));
		}
		if (JobType.getJobType(jobType).equals(JobType.UNKOWN_JOB)) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 2, "作业类型未知。"));
		}
		jobConfig.setJobType(jobType);

		String jobClass = getContents(rowCells, 2);
		if (jobType.equals(JobType.JAVA_JOB.name())) {
			if (jobClass == null || jobClass.trim().isEmpty()) {
				throw new SaturnJobConsoleException(
						createExceptionMessage(sheetNumber, rowNumber, 3, "对于JAVA作业，作业实现类必填。"));
			}
		}
		jobConfig.setJobClass(jobClass);

		String cron = getContents(rowCells, 3);
		if (jobType.equals(JobType.JAVA_JOB.name())
				|| jobType.equals(JobType.SHELL_JOB.name())) {
			if (cron == null || cron.trim().isEmpty()) {
				throw new SaturnJobConsoleException(
						createExceptionMessage(sheetNumber, rowNumber, 4, "对于JAVA/SHELL作业，cron表达式必填。"));
			}
			cron = cron.trim();
			try {
				CronExpression.validateExpression(cron);
			} catch (ParseException e) {
				throw new SaturnJobConsoleException(
						createExceptionMessage(sheetNumber, rowNumber, 4, "cron表达式语法有误，" + e.toString()));
			}
		} else {
			cron = "";// 其他类型的不需要持久化保存cron表达式
		}

		jobConfig.setCron(cron);

		jobConfig.setDescription(getContents(rowCells, 4));

		jobConfig.setLocalMode(Boolean.valueOf(getContents(rowCells, 5)));

		int shardingTotalCount = 1;
		if (jobConfig.getLocalMode()) {
			jobConfig.setShardingTotalCount(shardingTotalCount);
		} else {
			String tmp = getContents(rowCells, 6);
			if (tmp != null) {
				try {
					shardingTotalCount = Integer.parseInt(tmp);
				} catch (NumberFormatException e) {
					throw new SaturnJobConsoleException(
							createExceptionMessage(sheetNumber, rowNumber, 7, "分片数有误，" + e.toString()));
				}
			} else {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 7, "分片数必填"));
			}
			if (shardingTotalCount < 1) {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 7, "分片数不能小于1"));
			}
			jobConfig.setShardingTotalCount(shardingTotalCount);
		}

		int timeoutSeconds = 0;
		try {
			String tmp = getContents(rowCells, 7);
			if (tmp != null && !tmp.trim().isEmpty()) {
				timeoutSeconds = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(
					createExceptionMessage(sheetNumber, rowNumber, 8, "超时（Kill线程/进程）时间有误，" + e.toString()));
		}
		jobConfig.setTimeoutSeconds(timeoutSeconds);

		jobConfig.setJobParameter(getContents(rowCells, 8));

		String shardingItemParameters = getContents(rowCells, 9);
		if (jobConfig.getLocalMode()) {
			if (shardingItemParameters == null) {
				throw new SaturnJobConsoleException(
						createExceptionMessage(sheetNumber, rowNumber, 10, "对于本地模式作业，分片参数必填。"));
			} else {
				String[] split = shardingItemParameters.split(",");
				boolean includeXing = false;
				for (String tmp : split) {
					String[] split2 = tmp.split("=");
					if ("*".equalsIgnoreCase(split2[0].trim())) {
						includeXing = true;
						break;
					}
				}
				if (!includeXing) {
					throw new SaturnJobConsoleException(
							createExceptionMessage(sheetNumber, rowNumber, 10, "对于本地模式作业，分片参数必须包含如*=xx。"));
				}
			}
		} else if (shardingTotalCount > 0) {
			if (shardingItemParameters == null || shardingItemParameters.trim().isEmpty()
					|| shardingItemParameters.split(",").length < shardingTotalCount) {
				throw new SaturnJobConsoleException(
						createExceptionMessage(sheetNumber, rowNumber, 10, "分片参数不能小于分片总数。"));
			}
		}
		jobConfig.setShardingItemParameters(shardingItemParameters);

		jobConfig.setQueueName(getContents(rowCells, 10));
		jobConfig.setChannelName(getContents(rowCells, 11));
		jobConfig.setPreferList(getContents(rowCells, 12));
		jobConfig.setUseDispreferList(!Boolean.valueOf(getContents(rowCells, 13)));

		int processCountIntervalSeconds = 300;
		try {
			String tmp = getContents(rowCells, 14);
			if (tmp != null && !tmp.trim().isEmpty()) {
				processCountIntervalSeconds = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(
					createExceptionMessage(sheetNumber, rowNumber, 15, "统计处理数据量的间隔秒数有误，" + e.toString()));
		}
		jobConfig.setProcessCountIntervalSeconds(processCountIntervalSeconds);

		int loadLevel = 1;
		try {
			String tmp = getContents(rowCells, 15);
			if (tmp != null && !tmp.trim().isEmpty()) {
				loadLevel = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(
					createExceptionMessage(sheetNumber, rowNumber, 16, "负荷有误，" + e.toString()));
		}
		jobConfig.setLoadLevel(loadLevel);

		jobConfig.setShowNormalLog(Boolean.valueOf(getContents(rowCells, 16)));

		jobConfig.setPausePeriodDate(getContents(rowCells, 17));

		jobConfig.setPausePeriodTime(getContents(rowCells, 18));

		jobConfig.setUseSerial(Boolean.valueOf(getContents(rowCells, 19)));

		int jobDegree = 0;
		try {
			String tmp = getContents(rowCells, 20);
			if (tmp != null && !tmp.trim().isEmpty()) {
				jobDegree = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(
					createExceptionMessage(sheetNumber, rowNumber, 21, "作业重要等级有误，" + e.toString()));
		}
		jobConfig.setJobDegree(jobDegree);

		// 第21列，上报运行状态失效，由算法决定是否上报，看下面setEnabledReport时的逻辑，看addJob

		String jobMode = getContents(rowCells, 22);

		if (jobMode != null && jobMode.startsWith(com.vip.saturn.job.console.domain.JobMode.SYSTEM_PREFIX)) {
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 23, "作业模式有误，不能添加系统作业"));
		}
		jobConfig.setJobMode(jobMode);

		String dependencies = getContents(rowCells, 23);
		;
		if (dependencies != null && !dependencies.matches("[0-9a-zA-Z_,]*")) {
			throw new SaturnJobConsoleException(
					createExceptionMessage(sheetNumber, rowNumber, 24, "依赖的作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,"));
		}
		jobConfig.setDependencies(dependencies);

		jobConfig.setGroups(getContents(rowCells, 24));

		int timeout4AlarmSeconds = 0;
		try {
			String tmp = getContents(rowCells, 25);
			if (tmp != null && !tmp.trim().isEmpty()) {
				timeout4AlarmSeconds = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(
					createExceptionMessage(sheetNumber, rowNumber, 26, "超时（告警）时间有误，" + e.toString()));
		}
		jobConfig.setTimeout4AlarmSeconds(timeout4AlarmSeconds);

		String timeZone = getContents(rowCells, 26);
		if (timeZone == null || timeZone.trim().length() == 0) {
			timeZone = SaturnConstants.TIME_ZONE_ID_DEFAULT;
		} else {
			timeZone = timeZone.trim();
			if (!SaturnConstants.TIME_ZONE_IDS.contains(timeZone)) {
				throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 27, "时区有误"));
			}
		}
		jobConfig.setTimeZone(timeZone);

		return jobConfig;
	}

	private String getContents(Cell[] rowCell, int column) {
		if (rowCell.length > column) {
			return rowCell[column].getContents();
		}
		return null;
	}

	private String createExceptionMessage(int sheetNumber, int rowNumber, int columnNumber, String message) {
		return "内容格式有误，错误发生在表格页:" + sheetNumber + "，行号:" + rowNumber + "，列号:" + columnNumber + "，错误信息：" + message;
	}

	@Override
	public File exportJobs(String namespace) throws SaturnJobConsoleException {
		try {
			File tmp = new File(SaturnConstants.CACHES_FILE_PATH,
					"tmp_exportFile_" + System.currentTimeMillis() + "_" + random.nextInt(1000) + ".xls");
			if (!tmp.exists()) {
				FileUtils.forceMkdir(tmp.getParentFile());
				tmp.createNewFile();
			}
			WritableWorkbook writableWorkbook = Workbook.createWorkbook(tmp);
			WritableSheet sheet1 = writableWorkbook.createSheet("Sheet1", 0);
			sheet1.addCell(new Label(0, 0, "作业名称"));
			sheet1.addCell(new Label(1, 0, "作业类型"));
			sheet1.addCell(new Label(2, 0, "作业实现类"));
			sheet1.addCell(new Label(3, 0, "cron表达式"));
			sheet1.addCell(new Label(4, 0, "作业描述"));

			Label localModeLabel = new Label(5, 0, "本地模式");
			setCellComment(localModeLabel, "对于非本地模式，默认为false；对于本地模式，该配置无效，固定为true");
			sheet1.addCell(localModeLabel);

			Label shardingTotalCountLabel = new Label(6, 0, "分片数");
			setCellComment(shardingTotalCountLabel, "对本地作业无效");
			sheet1.addCell(shardingTotalCountLabel);

			Label timeoutSecondsLabel = new Label(7, 0, "超时（Kill线程/进程）时间");
			setCellComment(timeoutSecondsLabel, "0表示无超时");
			sheet1.addCell(timeoutSecondsLabel);

			sheet1.addCell(new Label(8, 0, "自定义参数"));
			sheet1.addCell(new Label(9, 0, "分片序列号/参数对照表"));
			sheet1.addCell(new Label(10, 0, "Queue名"));
			sheet1.addCell(new Label(11, 0, "执行结果发送的Channel"));

			Label preferListLabel = new Label(12, 0, "优先Executor");
			setCellComment(preferListLabel, "可填executorName，多个元素使用英文逗号隔开");
			sheet1.addCell(preferListLabel);

			Label usePreferListOnlyLabel = new Label(13, 0, "只使用优先Executor");
			setCellComment(usePreferListOnlyLabel, "默认为false");
			sheet1.addCell(usePreferListOnlyLabel);

			sheet1.addCell(new Label(14, 0, "统计处理数据量的间隔秒数"));
			sheet1.addCell(new Label(15, 0, "负荷"));
			sheet1.addCell(new Label(16, 0, "显示控制台输出日志"));
			sheet1.addCell(new Label(17, 0, "暂停日期段"));
			sheet1.addCell(new Label(18, 0, "暂停时间段"));

			Label useSerialLabel = new Label(19, 0, "串行消费");
			setCellComment(useSerialLabel, "默认为false");
			sheet1.addCell(useSerialLabel);

			Label jobDegreeLabel = new Label(20, 0, "作业重要等级");
			setCellComment(jobDegreeLabel, "0:没有定义,1:非线上业务,2:简单业务,3:一般业务,4:重要业务,5:核心业务");
			sheet1.addCell(jobDegreeLabel);

			Label enabledReportLabel = new Label(21, 0, "上报运行状态");
			setCellComment(enabledReportLabel, "对于定时作业，默认为true；对于消息作业，默认为false");
			sheet1.addCell(enabledReportLabel);

			Label jobModeLabel = new Label(22, 0, "作业模式");
			setCellComment(jobModeLabel, "用户不能添加系统作业");
			sheet1.addCell(jobModeLabel);

			Label dependenciesLabel = new Label(23, 0, "依赖的作业");
			setCellComment(dependenciesLabel, "作业的启用、禁用会检查依赖关系的作业的状态。依赖多个作业，使用英文逗号给开。");
			sheet1.addCell(dependenciesLabel);

			Label groupsLabel = new Label(24, 0, "所属分组");
			setCellComment(groupsLabel, "作业所属分组，一个作业只能属于一个分组，一个分组可以包含多个作业");
			sheet1.addCell(groupsLabel);

			Label timeout4AlarmSecondsLabel = new Label(25, 0, "超时（告警）时间");
			setCellComment(timeout4AlarmSecondsLabel, "0表示无超时");
			sheet1.addCell(timeout4AlarmSecondsLabel);

			Label timeZoneLabel = new Label(26, 0, "时区");
			setCellComment(timeZoneLabel, "作业运行时区");
			sheet1.addCell(timeZoneLabel);

			List<CurrentJobConfig> unSystemJobs = getUnSystemJobs(namespace);
			if (unSystemJobs != null && !unSystemJobs.isEmpty()) {
				CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
						.getCuratorFrameworkOp(namespace);
				for (int i = 0; i < unSystemJobs.size(); i++) {
					String jobName = unSystemJobs.get(i).getJobName();
					sheet1.addCell(new Label(0, i + 1, jobName));
					sheet1.addCell(new Label(1, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType"))));
					sheet1.addCell(new Label(2, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobClass"))));
					sheet1.addCell(new Label(3, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron"))));
					sheet1.addCell(new Label(4, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "description"))));
					sheet1.addCell(new Label(5, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "localMode"))));
					sheet1.addCell(new Label(6, i + 1,
							curatorFrameworkOp
									.getData(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"))));
					sheet1.addCell(new Label(7, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"))));
					sheet1.addCell(new Label(8, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobParameter"))));
					sheet1.addCell(new Label(9, i + 1, curatorFrameworkOp
							.getData(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters"))));
					sheet1.addCell(new Label(10, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "queueName"))));
					sheet1.addCell(new Label(11, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "channelName"))));
					sheet1.addCell(new Label(12, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList"))));
					String useDispreferList = curatorFrameworkOp
							.getData(JobNodePath.getConfigNodePath(jobName, "useDispreferList"));
					if (useDispreferList != null) {
						useDispreferList = String.valueOf(!Boolean.valueOf(useDispreferList));
					}
					sheet1.addCell(new Label(13, i + 1, useDispreferList));
					sheet1.addCell(new Label(14, i + 1, curatorFrameworkOp
							.getData(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"))));
					sheet1.addCell(new Label(15, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"))));
					sheet1.addCell(new Label(16, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "showNormalLog"))));
					sheet1.addCell(new Label(17, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate"))));
					sheet1.addCell(new Label(18, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime"))));
					sheet1.addCell(new Label(19, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useSerial"))));
					sheet1.addCell(new Label(20, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobDegree"))));
					sheet1.addCell(new Label(21, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabledReport"))));
					sheet1.addCell(new Label(22, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobMode"))));
					sheet1.addCell(new Label(23, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "dependencies"))));
					sheet1.addCell(new Label(24, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "groups"))));
					sheet1.addCell(new Label(25, i + 1, curatorFrameworkOp
							.getData(JobNodePath.getConfigNodePath(jobName, "timeout4AlarmSeconds"))));
					sheet1.addCell(new Label(26, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone"))));
				}
			}

			writableWorkbook.write();
			writableWorkbook.close();

			return tmp;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}

	private void setCellComment(WritableCell cell, String comment) {
		WritableCellFeatures cellFeatures = new WritableCellFeatures();
		cellFeatures.setComment(comment);
		cell.setCellFeatures(cellFeatures);
	}
}
