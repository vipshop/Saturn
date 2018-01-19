package com.vip.saturn.job.console.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.domain.ExecutionInfo.ExecutionStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.AlarmStatisticsService;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.*;
import com.vip.saturn.job.console.vo.GetJobConfigVo;
import com.vip.saturn.job.console.vo.UpdateJobConfigVo;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import jxl.Cell;
import jxl.CellType;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.data.Stat;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.lang.Boolean;
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

	private static final String ERR_MSG_PENDING_STATUS = "job:[{}] item:[{}] on executor:[{}] execution status is PENDING as {}";

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private CurrentJobConfigService currentJobConfigService;

	@Resource
	private SystemConfigService systemConfigService;

	@Resource
	private AlarmStatisticsService alarmStatisticsService;

	private Random random = new Random();

	private MapType customContextType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class,
			String.class);

	@Override
	public JobOverviewVo getJobOverviewVo(String namespace) throws SaturnJobConsoleException {
		JobOverviewVo jobOverviewVo = new JobOverviewVo();
		try {
			List<JobListElementVo> jobList = new ArrayList<>();
			int enabledNumber = 0;
			List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
			if (unSystemJobs != null) {
				CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
						.getCuratorFrameworkOp(namespace);
				for (JobConfig jobConfig : unSystemJobs) {
					try {
						JobListElementVo jobListElementVo = new JobListElementVo();
						SaturnBeanUtils.copyProperties(jobConfig, jobListElementVo);

						jobListElementVo.setDefaultValues();

						JobType jobType = JobType.getJobType(jobConfig.getJobType());
						if (JobType.UNKOWN_JOB.equals(jobType)) {
							if (jobListElementVo.getJobClass() != null
									&& jobListElementVo.getJobClass().indexOf("SaturnScriptJob") != -1) {
								jobListElementVo.setJobType(JobType.SHELL_JOB.name());
							} else {
								jobListElementVo.setJobType(JobType.JAVA_JOB.name());
							}
						}

						jobListElementVo.setStatus(
								getJobStatus(jobConfig.getJobName(), curatorFrameworkOp, jobConfig.getEnabled()));

						updateJobInfoShardingList(curatorFrameworkOp, jobConfig.getJobName(), jobListElementVo);

						if (jobListElementVo.getEnabled()) {
							enabledNumber++;
						}
						jobList.add(jobListElementVo);
					} catch (Exception e) {
						log.error("list job " + jobConfig.getJobName() + " error", e);
					}
				}
			}
			jobOverviewVo.setJobs(jobList);
			jobOverviewVo.setEnabledNumber(enabledNumber);
			jobOverviewVo.setTotalNumber(jobList.size());

			// 获取该域下的异常作业数量，捕获所有异常，打日志，不抛到前台
			try {
				String result = alarmStatisticsService.getAbnormalJobsByNamespace(namespace);
				if (result != null) {
					List<AbnormalJob> abnormalJobs = JSON.parseArray(result, AbnormalJob.class);
					if (abnormalJobs != null) {
						jobOverviewVo.setAbnormalNumber(abnormalJobs.size());
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}

		return jobOverviewVo;
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

	private void updateJobInfoShardingList(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName,
			JobListElementVo jobListElementVo) {
		if (JobStatus.STOPPED.equals(jobListElementVo.getStatus())) {// 作业如果是STOPPED状态，不需要显示已分配的executor
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
			jobListElementVo.setShardingList(shardingListSb.substring(0, shardingListSb.length() - 1));
		}
	}

	@Override
	public List<String> getGroups(String namespace) throws SaturnJobConsoleException {
		List<String> groups = new ArrayList<>();
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		if (unSystemJobs != null) {
			for (JobConfig jobConfig : unSystemJobs) {
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
		JobConfig4DB currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (currentJobConfig == null) {
			throw new SaturnJobConsoleException("不能获取该作业（" + jobName + "）依赖的所有作业，因为该作业不存在");
		}
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
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
				for (JobConfig jobConfig : unSystemJobs) {
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
		JobConfig4DB currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (currentJobConfig == null) {
			throw new SaturnJobConsoleException("不能获取依赖该作业（" + jobName + "）的所有作业，因为该作业不存在");
		}
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		if (unSystemJobs != null) {
			for (JobConfig jobConfig : unSystemJobs) {
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
		JobConfig4DB jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
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
		JobConfig4DB jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
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

	@Transactional
	@Override
	public void removeJob(String namespace, String jobName) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
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
							String.format("不能删除该作业(%s)，因为该作业创建时间距离现在不超过%d分钟", jobName,
									SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT
											/ 60000));
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
					return;
				}
				// (2)如果该作业servers下没有任何executor，可直接删除作业节点
				List<String> executors = curatorFrameworkOp.getChildren(jobServerPath);
				if (CollectionUtils.isEmpty(executors)) {
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
					return;
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
					return;
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					throw new SaturnJobConsoleException(e);
				}
			}
		} else {
			throw new SaturnJobConsoleException(String.format("不能删除该作业(%s)，因为该作业不处于STOPPED状态", jobName));
		}
	}

	@Override
	public List<ExecutorProvided> getCandidateExecutors(String namespace, String jobName)
			throws SaturnJobConsoleException {
		List<ExecutorProvided> executorProvidedList = new ArrayList<>();
		JobConfig4DB currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
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
	 * 先获取DCOS节点下的taskID节点；如果没有此节点，则尝试从executor节点下获取; <p> 不存在既有DCOS容器，又有K8S容器的模式。
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
	public void setPreferList(String namespace, String jobName, String preferList) throws SaturnJobConsoleException {
		// save to db
		JobConfig4DB oldJobConfig = currentJobConfigService
				.findConfigByNamespaceAndJobName(namespace, jobName);
		if (oldJobConfig == null) {
			throw new SaturnJobConsoleException("设置该作业（" + jobName + "）优先Executor失败，因为该作业不存在");
		}
		JobConfig4DB newJobConfig = new JobConfig4DB();
		BeanUtils.copyProperties(oldJobConfig, newJobConfig);
		newJobConfig.setPreferList(preferList);
		try {
			currentJobConfigService.updateNewAndSaveOld2History(newJobConfig, oldJobConfig, null);
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

	private void validateJobConfig(JobConfig jobConfig) throws SaturnJobConsoleException {
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

	@Transactional
	@Override
	public void addJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException {
		addOrCopyJob(namespace, jobConfig, null);
	}

	@Transactional
	@Override
	public void copyJob(String namespace, JobConfig jobConfig, String jobNameCopied) throws SaturnJobConsoleException {
		addOrCopyJob(namespace, jobConfig, jobNameCopied);
	}

	private void addOrCopyJob(String namespace, JobConfig jobConfig, String jobNameCopied)
			throws SaturnJobConsoleException {
		validateJobConfig(jobConfig);
		String jobName = jobConfig.getJobName();
		JobConfig4DB oldJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (oldJobConfig != null) {
			throw new SaturnJobConsoleException(String.format("该作业(%s)已经存在", jobName));
		}
		int maxJobNum = getMaxJobNum();
		if (jobIncExceeds(namespace, maxJobNum, 1)) {
			throw new SaturnJobConsoleException(String.format("总作业数超过最大限制(%d)，作业名%s创建失败", maxJobNum, jobName));
		} else {
			if (jobNameCopied == null) {
				persistJob(namespace, jobConfig);
			} else {
				JobConfig4DB jobConfig4DBCopied = currentJobConfigService
						.findConfigByNamespaceAndJobName(namespace, jobNameCopied);
				SaturnBeanUtils.copyPropertiesIgnoreNull(jobConfig, jobConfig4DBCopied);
				persistJob(namespace, jobConfig4DBCopied);
			}
		}
	}

	private void persistJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		if (curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobConfig.getJobName()))) {
			curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobConfig.getJobName()));
		}
		correctConfigValueIfNeeded(jobConfig);
		saveJobConfigToDb(namespace, jobConfig);
		saveJobConfigToZk(jobConfig, curatorFrameworkOp);
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
	public List<JobConfig> getUnSystemJobs(String namespace) throws SaturnJobConsoleException {
		List<JobConfig> unSystemJobs = new ArrayList<>();
		List<JobConfig4DB> jobConfig4DBList = currentJobConfigService.findConfigsByNamespace(namespace);
		if (jobConfig4DBList != null) {
			for (JobConfig4DB jobConfig4DB : jobConfig4DBList) {
				if (!(StringUtils.isNotBlank(jobConfig4DB.getJobMode()) && jobConfig4DB.getJobMode()
						.startsWith(JobMode.SYSTEM_PREFIX))) {
					JobConfig jobConfig = new JobConfig();
					SaturnBeanUtils.copyProperties(jobConfig4DB, jobConfig);
					unSystemJobs.add(jobConfig);
				}
			}
		}
		return unSystemJobs;
	}

	@Override
	public List<String> getUnSystemJobNames(String namespace) throws SaturnJobConsoleException {
		List<String> unSystemJobs = new ArrayList<>();
		List<JobConfig4DB> jobConfig4DBList = currentJobConfigService.findConfigsByNamespace(namespace);
		if (jobConfig4DBList != null) {
			for (JobConfig4DB jobConfig4DB : jobConfig4DBList) {
				if (!(StringUtils.isNotBlank(jobConfig4DB.getJobMode()) && jobConfig4DB.getJobMode()
						.startsWith(JobMode.SYSTEM_PREFIX))) {
					unSystemJobs.add(jobConfig4DB.getJobName());
				}
			}
		}
		return unSystemJobs;
	}

	@Override
	public void persistJobFromDB(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException {
		jobConfig.setDefaultValues();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		saveJobConfigToZk(jobConfig, curatorFrameworkOp);
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
		JobConfig4DB oldJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
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
		JobConfig4DB currentJobConfig = new JobConfig4DB();
		SaturnBeanUtils.copyProperties(jobConfig, currentJobConfig);
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

	private void saveJobConfigToZk(JobConfig jobConfig,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String jobName = jobConfig.getJobName();
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "enabled"),
				jobConfig.getEnabled());
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
	public List<ImportJobResult> importJobs(String namespace, MultipartFile file) throws SaturnJobConsoleException {
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
			List<ImportJobResult> results = new ArrayList<>();
			for (JobConfig jobConfig : jobConfigList) {
				ImportJobResult importJobResult = new ImportJobResult();
				importJobResult.setJobName(jobConfig.getJobName());
				try {
					addJob(namespace, jobConfig);
					importJobResult.setSuccess(true);
				} catch (SaturnJobConsoleException e) {
					importJobResult.setSuccess(false);
					importJobResult.setMessage(e.getMessage());
				} catch (Exception e) {
					importJobResult.setSuccess(false);
					importJobResult.setMessage(e.toString());
				}
				results.add(importJobResult);
			}
			return results;
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

			List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
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

	@Override
	public JobConfig getJobConfigFromZK(String namespace, String jobName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		JobConfig result = new JobConfig();
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
				Boolean.valueOf(
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled"))));// 默认是禁用的
		result.setPreferList(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList")));
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
		return result;
	}

	@Override
	public JobConfig getJobConfig(String namespace, String jobName) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig4DB == null) {
			throw new SaturnJobConsoleException(String.format("该作业(%s)不存在", jobName));
		}
		JobConfig jobConfig = new JobConfig();
		SaturnBeanUtils.copyProperties(jobConfig4DB, jobConfig);
		return jobConfig;
	}

	@Override
	public JobStatus getJobStatus(String namespace, String jobName) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig == null) {
			throw new SaturnJobConsoleException("不能获取该作业（" + jobName + "）的状态，因为该作业不存在");
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		return getJobStatus(jobName, curatorFrameworkOp, jobConfig.getEnabled());
	}

	@Override
	public GetJobConfigVo getJobConfigVo(String namespace, String jobName) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig4DB == null) {
			throw new SaturnJobConsoleException(String.format("该作业(%s)不存在", jobName));
		}
		GetJobConfigVo getJobConfigVo = new GetJobConfigVo();
		JobConfig jobConfig = new JobConfig();
		SaturnBeanUtils.copyProperties(jobConfig4DB, jobConfig);
		getJobConfigVo.copyFrom(jobConfig);

		getJobConfigVo.setTimeZonesProvided(Arrays.asList(TimeZone.getAvailableIDs()));
		getJobConfigVo.setPreferListProvided(getCandidateExecutors(namespace, jobName));

		List<String> unSystemJobNames = getUnSystemJobNames(namespace);
		if (unSystemJobNames != null) {
			unSystemJobNames.remove(jobName);
			getJobConfigVo.setDependenciesProvided(unSystemJobNames);
		}

		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		getJobConfigVo.setStatus(
				getJobStatus(getJobConfigVo.getJobName(), curatorFrameworkOp, getJobConfigVo.getEnabled()));

		return getJobConfigVo;
	}

	@Transactional
	@Override
	public void updateJobConfig(String namespace, UpdateJobConfigVo updateJobConfigVo)
			throws SaturnJobConsoleException {
		JobConfig jobConfig = updateJobConfigVo.toJobConfig();
		JobConfig4DB jobConfig4DB = currentJobConfigService
				.findConfigByNamespaceAndJobName(namespace, jobConfig.getJobName());
		if (jobConfig4DB == null) {
			throw new SaturnJobConsoleException(String.format("该作业(%s)不存在", jobConfig.getJobName()));
		}
		jobConfig.setDefaultValues();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		BooleanWrapper bw = new BooleanWrapper(false);
		CuratorRepository.CuratorFrameworkOp.CuratorTransactionOp curatorTransactionOp = null;
		try {
			curatorTransactionOp = curatorFrameworkOp.inTransaction()
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "jobMode"),
							jobConfig.getJobMode(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "shardingTotalCount"),
							jobConfig.getShardingTotalCount(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "loadLevel"),
							jobConfig.getLoadLevel(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "jobDegree"),
							jobConfig.getJobDegree(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "enabledReport"),
							jobConfig.getEnabledReport(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "timeZone"),
							StringUtils.trim(jobConfig.getTimeZone()), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "cron"),
							StringUtils.trim(jobConfig.getCron()), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "pausePeriodDate"),
							jobConfig.getPausePeriodDate(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "pausePeriodTime"),
							jobConfig.getPausePeriodTime(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "shardingItemParameters"),
							jobConfig.getShardingItemParameters(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "jobParameter"),
							jobConfig.getJobParameter(), bw)
					.replaceIfchanged(
							JobNodePath.getConfigNodePath(jobConfig.getJobName(), "processCountIntervalSeconds"),
							jobConfig.getProcessCountIntervalSeconds(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "timeout4AlarmSeconds"),
							jobConfig.getTimeout4AlarmSeconds(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "timeoutSeconds"),
							jobConfig.getTimeoutSeconds(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "dependencies"),
							jobConfig.getDependencies(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "groups"),
							jobConfig.getGroups(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "description"),
							jobConfig.getDescription(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "channelName"),
							StringUtils.trim(jobConfig.getChannelName()), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "queueName"),
							StringUtils.trim(jobConfig.getQueueName()), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "showNormalLog"),
							jobConfig.getShowNormalLog(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "preferList"),
							jobConfig.getPreferList(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "useDispreferList"),
							jobConfig.getUseDispreferList(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "failover"),
							jobConfig.getFailover(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "localMode"),
							jobConfig.getLocalMode(), bw)
					.replaceIfchanged(JobNodePath.getConfigNodePath(jobConfig.getJobName(), "useSerial"),
							jobConfig.getUseSerial(), bw);
			// 当enabledReport关闭上报时，要清理execution节点
			if (jobConfig.getEnabledReport() != null && !jobConfig.getEnabledReport()) {
				log.info("the switch of enabledReport set to false, now deleteJob the execution zk node");
				String executionNodePath = JobNodePath.getExecutionNodePath(jobConfig.getJobName());
				if (curatorFrameworkOp.checkExists(executionNodePath)) {
					curatorFrameworkOp.deleteRecursive(executionNodePath);
				}
			}
		} catch (Exception e) {
			log.error("update settings to zk failed: {}", e);
			throw new SaturnJobConsoleException(e);
		}
		try {
			// config changed, update current config and save a copy to history config.
			if (bw.isValue()) {
				JobConfig4DB newJobConfig4DB = new JobConfig4DB();
				SaturnBeanUtils.copyProperties(jobConfig4DB, newJobConfig4DB);
				SaturnBeanUtils.copyPropertiesIgnoreNull(jobConfig, newJobConfig4DB);
				currentJobConfigService.updateNewAndSaveOld2History(newJobConfig4DB, jobConfig4DB, null);
			}
			if (curatorTransactionOp != null) {
				curatorTransactionOp.commit();
			}
		} catch (Exception e) {
			log.error("update settings to db failed: {}", e);
			throw new SaturnJobConsoleException(e);
		}
	}

	@Override
	public List<String> getAllJobNamesFromZK(String namespace) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		List<String> allJobs = new ArrayList<>();
		String jobsNodePath = JobNodePath.get$JobsNodePath();
		if (curatorFrameworkOp.checkExists(jobsNodePath)) {
			List<String> jobs = curatorFrameworkOp.getChildren(jobsNodePath);
			if (jobs != null && jobs.size() > 0) {
				for (String job : jobs) {
					// 如果config节点存在才视为正常作业，其他异常作业在其他功能操作时也忽略
					if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(job))) {
						allJobs.add(job);
					}
				}
			}
		}
		Collections.sort(allJobs);
		return allJobs;
	}

	@Transactional
	@Override
	public void updateJobCron(String namespace, String jobName, String cron, Map<String, String> customContext)
			throws SaturnJobConsoleException {
		String cron0 = cron;
		if (cron0 != null && !cron0.trim().isEmpty()) {
			try {
				cron0 = cron0.trim();
				CronExpression.validateExpression(cron0);
			} catch (ParseException e) {
				throw new SaturnJobConsoleException("The cron expression is valid: " + cron);
			}
		} else {
			cron0 = "";
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))) {
			String newCustomContextStr = null;
			String newCron = null;
			String oldCustomContextStr = curatorFrameworkOp
					.getData(JobNodePath.getConfigNodePath(jobName, "customContext"));
			Map<String, String> oldCustomContextMap = toCustomContext(oldCustomContextStr);
			if (customContext != null && !customContext.isEmpty()) {
				oldCustomContextMap.putAll(customContext);
				newCustomContextStr = toCustomContext(oldCustomContextMap);
				if (newCustomContextStr.getBytes().length > 1024 * 1024) {
					throw new SaturnJobConsoleException("The all customContext is out of zk limit memory(1M)");
				}
			}
			String oldCron = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron"));
			if (cron0 != null && oldCron != null && !cron0.equals(oldCron.trim())) {
				newCron = cron0;
			}
			if (newCustomContextStr != null || newCron != null) {
				saveCronToDb(jobName, curatorFrameworkOp, newCustomContextStr, newCron);
			}
			if (newCustomContextStr != null) {
				curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, "customContext"), newCustomContextStr);
			}
			if (newCron != null) {
				curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, "cron"), newCron);
			}
		} else {
			throw new SaturnJobConsoleException("The job is not found: " + jobName);
		}
	}

	private void saveCronToDb(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			String newCustomContextStr, String newCron)
			throws SaturnJobConsoleException {
		String namespace = curatorFrameworkOp.getCuratorFramework().getNamespace();
		JobConfig4DB jobConfig4DB = currentJobConfigService.findConfigByNamespaceAndJobName(namespace,
				jobName);
		if (jobConfig4DB == null) {
			String errorMsg = "在DB找不到该作业的配置, namespace：" + namespace + " jobName:" + jobName;
			log.error(errorMsg);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMsg);
		}
		JobConfig4DB newJobConfig4DB = new JobConfig4DB();
		SaturnBeanUtils.copyProperties(jobConfig4DB, newJobConfig4DB);
		if (newCustomContextStr != null) {
			newJobConfig4DB.setCustomContext(newCustomContextStr);
		}
		if (newCron != null) {
			newJobConfig4DB.setCron(newCron);
		}

		try {
			currentJobConfigService.updateNewAndSaveOld2History(newJobConfig4DB, jobConfig4DB, null);
		} catch (Exception e) {
			log.error("exception is thrown during change job state in db", e);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	/**
	 * 将str转为map
	 *
	 * @param customContextStr str字符串
	 * @return 自定义上下文map
	 */
	private Map<String, String> toCustomContext(String customContextStr) {
		Map<String, String> customContext = null;
		if (customContextStr != null) {
			customContext = JsonUtils.fromJSON(customContextStr, customContextType);
		}
		if (customContext == null) {
			customContext = new HashMap<>();
		}
		return customContext;
	}

	/**
	 * 将map转为str字符串
	 *
	 * @param customContextMap 自定义上下文map
	 * @return 自定义上下文str
	 */
	private String toCustomContext(Map<String, String> customContextMap) {
		String result = JsonUtils.toJSON(customContextMap);
		if (result == null) {
			result = "";
		}
		return result.trim();
	}

	@Override
	public List<JobServer> getJobServers(String namespace, String jobName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		List<String> executors = new ArrayList<>();
		if (curatorFrameworkOp.checkExists(serverNodePath)) {
			executors = curatorFrameworkOp.getChildren(serverNodePath);
		}
		String leaderIp = curatorFrameworkOp.getData(JobNodePath.getLeaderNodePath(jobName, "election/host"));
		List<JobServer> result = new ArrayList<>();
		if (executors != null) {
			for (String each : executors) {
				result.add(getJobServer(namespace, jobName, leaderIp, each, curatorFrameworkOp));
			}
		}
		return result;
	}

	private JobServer getJobServer(String namespace, String jobName, String leaderIp, String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		JobServer result = new JobServer();
		result.setExecutorName(executorName);
		result.setIp(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executorName, "ip")));
		result.setVersion(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executorName, "version")));
//		String processSuccessCount = curatorFrameworkOp
//				.getData(JobNodePath.getServerNodePath(jobName, executorName, "processSuccessCount"));
//		result.setProcessSuccessCount(null == processSuccessCount ? 0 : Integer.parseInt(processSuccessCount));
//		String processFailureCount = curatorFrameworkOp
//				.getData(JobNodePath.getServerNodePath(jobName, executorName, "processFailureCount"));
//		result.setProcessFailureCount(null == processFailureCount ? 0 : Integer.parseInt(processFailureCount));
		result.setSharding(
				curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executorName, "sharding")));
		result.setStatus(getServerStatus(executorName, curatorFrameworkOp));
		result.setLeader(executorName.equals(leaderIp));
		result.setJobStatus(getJobStatus(namespace, jobName));
		result.setJobVersion(getJobVersion(jobName, executorName, curatorFrameworkOp));
		result.setContainer(curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorTaskNodePath(executorName)));

		return result;
	}

	private ServerStatus getServerStatus(String executorName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String ip = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorNodePath(executorName, "ip"));
		return ServerStatus.getServerStatus(ip);
	}

	private String getJobVersion(String jobName, String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String jobVersion = curatorFrameworkOp
				.getData(JobNodePath.getServerNodePath(jobName, executorName, "jobVersion"));
		return jobVersion == null ? "" : jobVersion;
	}

	@Override
	public void runAtOnce(String namespace, String jobName) throws SaturnJobConsoleException {
		JobStatus jobStatus = getJobStatus(namespace, jobName);
		if (!JobStatus.READY.equals(jobStatus)) {
			throw new SaturnJobConsoleException(String.format("该作业(%s)不处于READY状态，不能立即执行", jobName));
		}
		List<JobServer> jobServers = getJobServers(namespace, jobName);
		if (jobServers != null && !jobServers.isEmpty()) {
			boolean hasOnlineExecutor = false;
			CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
			for (JobServer jobServer : jobServers) {
				if (ServerStatus.ONLINE.equals(jobServer.getStatus())) {
					hasOnlineExecutor = true;
					String executorName = jobServer.getExecutorName();
					String path = JobNodePath.getRunOneTimePath(jobName, executorName);
					if (curatorFrameworkOp.checkExists(path)) {
						curatorFrameworkOp.delete(path);
					}
					curatorFrameworkOp.create(path);
					log.info("runAtOnce namespace:{}, jobName:{}, executorName:{}", namespace, jobName, executorName);
				}
			}
			if (!hasOnlineExecutor) {
				throw new SaturnJobConsoleException("没有ONLINE的executor，不能立即执行");
			}
		} else {
			throw new SaturnJobConsoleException(String.format("没有executor接管该作业(%s)，不能立即执行", jobName));
		}
	}

	@Override
	public void stopAtOnce(String namespace, String jobName) throws SaturnJobConsoleException {
		JobStatus jobStatus = getJobStatus(namespace, jobName);
		if (!JobStatus.STOPPING.equals(jobStatus)) {
			throw new SaturnJobConsoleException(String.format("该作业(%s)不处于STOPPING状态，不能立即终止", jobName));
		}
		List<JobServer> jobServers = getJobServers(namespace, jobName);
		if (jobServers != null && !jobServers.isEmpty()) {
			CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
			for (JobServer jobServer : jobServers) {
				String executorName = jobServer.getExecutorName();
				String path = JobNodePath.getStopOneTimePath(jobName, executorName);
				if (curatorFrameworkOp.checkExists(path)) {
					curatorFrameworkOp.delete(path);
				}
				curatorFrameworkOp.create(path);
				log.info("stopAtOnce namespace:{}, jobName:{}, executorName:{}", namespace, jobName, executorName);
			}
		} else {
			throw new SaturnJobConsoleException(String.format("没有executor接管该作业(%s)，不能立即终止", jobName));
		}
	}

	@Override
	public List<ExecutionInfo> getExecutionStatus(String namespace, String jobName) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		JobConfig jobConfig = getJobConfig(namespace, jobName);
		if (!jobConfig.getEnabled() && JobStatus.STOPPED.equals(getJobStatus(jobName, curatorFrameworkOp, false))) {
			return Lists.newArrayList();
		}

		// update report node and sleep for 500ms
		updateReportNodeAndWait(jobName, curatorFrameworkOp, 500L);
		// 如果execution节点不存在则返回空List
		if (!curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName))) {
			return Lists.newArrayList();
		}

		List<ExecutionInfo> result = Lists.newArrayList();
		Map<String, String> itemExecutorMap = buildItem2ExecutorMap(jobName, curatorFrameworkOp);
		for (String shardItem : itemExecutorMap.keySet()) {
			result.add(buildExecutionInfo(jobName, shardItem, itemExecutorMap.get(shardItem),
					curatorFrameworkOp));
		}
		Collections.sort(result);

		return result;
	}

	@Override
	public String getExecutionLog(String namespace, String jobName, String jobItem) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		return curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, jobItem, "jobLog"));
	}

	private void updateReportNodeAndWait(String jobName, CuratorFrameworkOp curatorFrameworkOp, long sleepInMill) {
		curatorFrameworkOp.update(JobNodePath.getReportPath(jobName), System.currentTimeMillis());
		try {
			Thread.sleep(sleepInMill);
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}
	}

	private ExecutionInfo buildExecutionInfo(String jobName, String shardItem, String executorName,
			CuratorFrameworkOp curatorFrameworkOp) {
		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setJobName(jobName);
		executionInfo.setItem(Integer.parseInt(shardItem));

		setExecutorNameAndStatus(jobName, shardItem, executorName, curatorFrameworkOp, executionInfo);

		// jobMsg
		String jobMsg = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, shardItem, "jobMsg"));
		executionInfo.setJobMsg(jobMsg);

		// timeZone
		String timeZoneStr = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone"));
		if (StringUtils.isBlank(timeZoneStr)) {
			timeZoneStr = SaturnConstants.TIME_ZONE_ID_DEFAULT;
		}
		executionInfo.setTimeZone(timeZoneStr);
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
		// last begin time
		String lastBeginTime = curatorFrameworkOp
				.getData(JobNodePath.getExecutionNodePath(jobName, shardItem, "lastBeginTime"));
		executionInfo
				.setLastBeginTime(SaturnConsoleUtils.parseMillisecond2DisplayTime(lastBeginTime, timeZone));
		// next fire time
		String nextFireTime = curatorFrameworkOp
				.getData(JobNodePath.getExecutionNodePath(jobName, shardItem, "nextFireTime"));
		executionInfo.setNextFireTime(SaturnConsoleUtils.parseMillisecond2DisplayTime(nextFireTime, timeZone));
		// last complete time
		String lastCompleteTime = curatorFrameworkOp
				.getData(JobNodePath.getExecutionNodePath(jobName, shardItem, "lastCompleteTime"));
		if (lastCompleteTime != null) {
			long lastCompleteTimeLong = Long.parseLong(lastCompleteTime);
			if (lastBeginTime == null) {
				executionInfo.setLastCompleteTime(
						SaturnConsoleUtils.parseMillisecond2DisplayTime(lastCompleteTime, timeZone));
			} else {
				long lastBeginTimeLong = Long.parseLong(lastBeginTime);
				if (lastCompleteTimeLong >= lastBeginTimeLong) {
					executionInfo.setLastCompleteTime(
							SaturnConsoleUtils.parseMillisecond2DisplayTime(lastCompleteTime, timeZone));

					executionInfo.setLastTimeConsumedInSec((lastBeginTimeLong - lastBeginTimeLong) / 1000d);
				}
			}
		}

		return executionInfo;
	}

	private void setExecutorNameAndStatus(String jobName, String shardItem, String executorName,
			CuratorFrameworkOp curatorFrameworkOp, ExecutionInfo executionInfo) {
		boolean isEnabledReport = SaturnConsoleUtils.checkIfJobIsEnabledReport(jobName, curatorFrameworkOp);
		if (!isEnabledReport) {
			executionInfo.setExecutorName(executorName);
			executionInfo.setStatus(ExecutionStatus.BLANK);
			return;
		}

		boolean isCompleted = false;
		String completedNodePath = JobNodePath.getCompletedNodePath(jobName, shardItem);
		String completedData = curatorFrameworkOp.getData(completedNodePath);
		if (completedData != null) {
			isCompleted = true;
			executionInfo.setExecutorName(StringUtils.isNotBlank(completedData) ? completedData : executorName);
			//不能立即返回还是要看看是否failed或者timeout
		}

		String failedNodePath = JobNodePath.getFailedNodePath(jobName, shardItem);
		if (curatorFrameworkOp.checkExists(failedNodePath)) {
			if (isCompleted) {
				executionInfo.setStatus(ExecutionStatus.FAILED);
			} else {
				log.warn(ERR_MSG_PENDING_STATUS, jobName, shardItem, executorName,
						"no completed node found but only failed node");
				executionInfo.setExecutorName(executorName);
				executionInfo.setStatus(ExecutionStatus.PENDING);
			}
			return;
		}

		String timeoutNodePath = JobNodePath.getTimeoutNodePath(jobName, shardItem);
		if (curatorFrameworkOp.checkExists(timeoutNodePath)) {
			if (isCompleted) {
				executionInfo.setStatus(ExecutionStatus.TIMEOUT);
			} else {
				log.warn(ERR_MSG_PENDING_STATUS, jobName, shardItem, executorName,
						"no completed node found but only timeout node");
				executionInfo.setExecutorName(executorName);
				executionInfo.setStatus(ExecutionStatus.PENDING);
			}
			return;
		}

		// 只有completed节点没有timeout/failed意味着成功，于是立即返回
		if (isCompleted) {
			executionInfo.setStatus(ExecutionStatus.COMPLETED);
			return;
		}

		boolean isRunning = false;
		String runningNodePath = JobNodePath.getRunningNodePath(jobName, shardItem);
		String runningData = curatorFrameworkOp.getData(runningNodePath);
		if (runningData != null) {
			isRunning = true;
			executionInfo.setExecutorName(StringUtils.isBlank(runningData) ? executorName : runningData);
			long mtime = curatorFrameworkOp.getMtime(runningNodePath);
			executionInfo.setTimeConsumed((new Date().getTime() - mtime) / 1000);
			executionInfo.setStatus(ExecutionStatus.RUNNING);
			//不能立即返回还是要看看是否正在failover
		}

		String failoverNodePath = JobNodePath.getFailoverNodePath(jobName, shardItem);
		String failoverData = curatorFrameworkOp.getData(failoverNodePath);
		if (failoverData != null) {
			// 设置为failover节点的真是executorName
			executionInfo.setExecutorName(failoverData);
			executionInfo.setFailover(true);
			// 如果有failover节点，running应该配对出现，否则显示pending状态
			if (!isRunning) {
				log.warn(ERR_MSG_PENDING_STATUS, jobName, shardItem, executorName,
						"no running node found but only failover node");
				executionInfo.setStatus(ExecutionStatus.PENDING);
			}

			return;
		}

		if (!isRunning) {
			log.warn(ERR_MSG_PENDING_STATUS, jobName, shardItem, executorName,
					"no running node or completed node found");
			executionInfo.setStatus(ExecutionStatus.PENDING);
		}
	}

	private Map<String, String> buildItem2ExecutorMap(String jobName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {

		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		List<String> servers = curatorFrameworkOp.getChildren(serverNodePath);

		if (servers == null || servers.size() == 0) {
			return Maps.newHashMap();
		}

		Map<String, String> resultMap = new HashMap<>();
		for (String server : servers) {
			String shardingData = curatorFrameworkOp.getData(JobNodePath.getServerSharding(jobName, server));
			if (StringUtils.isBlank(shardingData)) {
				continue;
			}

			String[] shardingValues = shardingData.split(",");
			for (String value : shardingValues) {
				if (StringUtils.isBlank(value)) {
					continue;
				}

				resultMap.put(value.trim(), server);
			}
		}
		return resultMap;
	}

}
