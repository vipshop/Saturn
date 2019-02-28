package com.vip.saturn.job.console.service.impl;

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
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.*;
import com.vip.saturn.job.console.vo.GetJobConfigVo;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.lang.Boolean;
import java.text.ParseException;
import java.util.*;

import static com.vip.saturn.job.console.exception.SaturnJobConsoleException.ERROR_CODE_BAD_REQUEST;
import static com.vip.saturn.job.console.exception.SaturnJobConsoleException.ERROR_CODE_NOT_EXISTED;

/**
 * @author hebelala
 */
public class JobServiceImpl implements JobService {

	public static final String CONFIG_ITEM_LOAD_LEVEL = "loadLevel";
	public static final String CONFIG_ITEM_ENABLED = "enabled";
	public static final String CONFIG_ITEM_DESCRIPTION = "description";
	public static final String CONFIG_ITEM_CUSTOM_CONTEXT = "customContext";
	public static final String CONFIG_ITEM_JOB_TYPE = "jobType";
	public static final String CONFIG_ITEM_JOB_MODE = "jobMode";
	public static final String CONFIG_ITEM_SHARDING_ITEM_PARAMETERS = "shardingItemParameters";
	public static final String CONFIG_ITEM_JOB_PARAMETER = "jobParameter";
	public static final String CONFIG_ITEM_QUEUE_NAME = "queueName";
	public static final String CONFIG_ITEM_CHANNEL_NAME = "channelName";
	public static final String CONFIG_ITEM_FAILOVER = "failover";
	public static final String CONFIG_ITEM_MONITOR_EXECUTION = "monitorExecution";
	public static final String CONFIG_ITEM_TIMEOUT_4_ALARM_SECONDS = "timeout4AlarmSeconds";
	public static final String CONFIG_ITEM_TIMEOUT_SECONDS = "timeoutSeconds";
	public static final String CONFIG_ITEM_TIME_ZONE = "timeZone";
	public static final String CONFIG_ITEM_CRON = "cron";
	public static final String CONFIG_ITEM_PAUSE_PERIOD_DATE = "pausePeriodDate";
	public static final String CONFIG_ITEM_PAUSE_PERIOD_TIME = "pausePeriodTime";
	public static final String CONFIG_ITEM_PROCESS_COUNT_INTERVAL_SECONDS = "processCountIntervalSeconds";
	public static final String CONFIG_ITEM_SHARDING_TOTAL_COUNT = "shardingTotalCount";
	public static final String CONFIG_ITEM_SHOW_NORMAL_LOG = "showNormalLog";
	public static final String CONFIG_ITEM_JOB_DEGREE = "jobDegree";
	public static final String CONFIG_ITEM_ENABLED_REPORT = "enabledReport";
	public static final String CONFIG_ITEM_PREFER_LIST = "preferList";
	public static final String CONFIG_ITEM_USE_DISPREFER_LIST = "useDispreferList";
	public static final String CONFIG_ITEM_LOCAL_MODE = "localMode";
	public static final String CONFIG_ITEM_USE_SERIAL = "useSerial";
	public static final String CONFIG_ITEM_DEPENDENCIES = "dependencies";
	public static final String CONFIG_ITEM_GROUPS = "groups";
	public static final String CONFIG_ITEM_JOB_CLASS = "jobClass";
	public static final String CONFIG_ITEM_RERUN = "rerun";
	public static final String CONFIG_ITEM_DOWNSTREAM = "downStream";
	public static final String CONFIG_ITEM_UPSTREAM = "upStream";
	private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);
	private static final int DEFAULT_MAX_JOB_NUM = 100;
	private static final int DEFAULT_INTERVAL_TIME_OF_ENABLED_REPORT = 5;
	// 最大允许显示的job log为zk默认的max jute buffer size
	private static final int DEFAULT_MAX_ZNODE_DATA_LENGTH = 1048576;
	private static final String ERR_MSG_PENDING_STATUS =
			"job:[{}] item:[{}] on executor:[{}] execution status is " + "PENDING as {}";
	private static final String ERR_MSG_TOO_LONG_TO_DISPLAY = "Not display the log as the length is out of max length";

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private CurrentJobConfigService currentJobConfigService;

	@Resource
	private SystemConfigService systemConfigService;

	private Random random = new Random();

	private MapType customContextType = TypeFactory.defaultInstance()
			.constructMapType(HashMap.class, String.class, String.class);

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


	@Override
	public List<String> getGroups(String namespace) throws SaturnJobConsoleException {
		List<String> groups = new ArrayList<>();
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		if (unSystemJobs != null) {
			for (JobConfig jobConfig : unSystemJobs) {
				String jobGroups = jobConfig.getGroups();
				if (StringUtils.isBlank(jobGroups)) {
					jobGroups = SaturnConstants.NO_GROUPS_LABEL;
				}
				if (!groups.contains(jobGroups)) {
					groups.add(jobGroups);
				}
			}
		}
		return groups;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void enableJob(String namespace, String jobName, String updatedBy) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, "不能启用该作业（" + jobName + "），因为该作业不存在");
		}
		if (jobConfig.getEnabled()) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "该作业（" + jobName + "）已经处于启用状态");
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		boolean allShardsFinished = isAllShardsFinished(jobName, curatorFrameworkOp);
		if (!allShardsFinished) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "不能启用该作业（" + jobName + "），因为该作业不处于STOPPED状态");
		}
		jobConfig.setEnabled(true);
		jobConfig.setLastUpdateTime(new Date());
		jobConfig.setLastUpdateBy(updatedBy);
		currentJobConfigService.updateByPrimaryKey(jobConfig);
		curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED), true);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void disableJob(String namespace, String jobName, String updatedBy) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, "不能禁用该作业（" + jobName + "），因为该作业不存在");
		}
		if (!jobConfig.getEnabled()) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "该作业（" + jobName + "）已经处于禁用状态");
		}
		jobConfig.setEnabled(Boolean.FALSE);
		jobConfig.setLastUpdateTime(new Date());
		jobConfig.setLastUpdateBy(updatedBy);
		currentJobConfigService.updateByPrimaryKey(jobConfig);
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED), false);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void removeJob(String namespace, String jobName) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig4DB == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, "不能删除该作业（" + jobName + "），因为该作业不存在");
		}
		String upStream = jobConfig4DB.getUpStream();
		if (StringUtils.isNotBlank(upStream)) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED,
					"不能删除该作业（" + jobName + "），因为该作业存在上游作业（" + upStream + "），请先断开上下游关系再删除");
		}
		String downStream = jobConfig4DB.getDownStream();
		if (StringUtils.isNotBlank(downStream)) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED,
					"不能删除该作业（" + jobName + "），因为该作业存在下游作业（" + downStream + "），请先断开上下游关系再删除");
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		JobStatus jobStatus = getJobStatus(jobName, curatorFrameworkOp, jobConfig4DB.getEnabled());

		if (JobStatus.STOPPED != jobStatus) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					String.format("不能删除该作业(%s)，因为该作业不处于STOPPED状态", jobName));
		}

		Stat stat = curatorFrameworkOp.getStat(JobNodePath.getJobNodePath(jobName));
		if (stat != null) {
			long createTimeDiff = System.currentTimeMillis() - stat.getCtime();
			if (createTimeDiff < SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						String.format("不能删除该作业(%s)，因为该作业创建时间距离现在不超过%d分钟", jobName,
								SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT / 60000));
			}
		}
		// remove job from db
		currentJobConfigService.deleteByPrimaryKey(jobConfig4DB.getId());
		// remove job from zk
		removeJobFromZk(jobName, curatorFrameworkOp);
	}

	/**
	 * 删除zk上的作业结点。先持久化config/toDelete结点，让executor收到该事件，shutdown自身的该作业。如果所有executor都已经shutdown该作业，则才可以安全删除作业结点。
	 * @return 等待executor shutdown作业，等待一定时间后，如果executor还没完全shutdown，则放弃等待，返回false。 否则，在等待时间内，executor都shutdown完全，则删除作业结点，并返回true。
	 */
	private boolean removeJobFromZk(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
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
				return true;
			}
			// (2)如果该作业servers下没有任何executor，可直接删除作业节点
			List<String> executors = curatorFrameworkOp.getChildren(jobServerPath);
			if (CollectionUtils.isEmpty(executors)) {
				curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
				return true;
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
				return true;
			}
			try {
				Thread.sleep(200);
			} catch (Exception e) {
				throw new SaturnJobConsoleException(e);
			}
		}

		return false;
	}

	@Override
	public List<ExecutorProvided> getCandidateExecutors(String namespace, String jobName)
			throws SaturnJobConsoleException {
		JobConfig4DB currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (currentJobConfig == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED,
					"不能获取该作业（" + jobName + "）可选择的优先Executor，因为该作业不存在");
		}
		List<ExecutorProvided> executorProvidedList = new ArrayList<>();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String executorsNodePath = SaturnExecutorsNode.getExecutorsNodePath();
		if (!curatorFrameworkOp.checkExists(executorsNodePath)) {
			return executorProvidedList;
		}
		List<String> executors = curatorFrameworkOp.getChildren(executorsNodePath);
		if (executors == null) {
			executors = new ArrayList<>();
		}
		if (!executors.isEmpty()) {
			for (String executor : executors) {
				if (curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorTaskNodePath(executor))) {
					continue;// 过滤容器中的Executor，容器资源只需要可以选择taskId即可
				}
				ExecutorProvided executorProvided = new ExecutorProvided();
				executorProvided.setType(ExecutorProvidedType.PHYSICAL);
				executorProvided.setExecutorName(executor);
				executorProvided.setNoTraffic(
						curatorFrameworkOp.checkExists(SaturnExecutorsNode.getExecutorNoTrafficNodePath(executor)));
				String ip = curatorFrameworkOp.getData(SaturnExecutorsNode.getExecutorIpNodePath(executor));
				if (StringUtils.isNotBlank(ip)) {
					executorProvided.setStatus(ExecutorProvidedStatus.ONLINE);
					executorProvided.setIp(ip);
				} else {
					executorProvided.setStatus(ExecutorProvidedStatus.OFFLINE);
				}
				executorProvidedList.add(executorProvided);
			}
		}

		List<ExecutorProvided> dockerExecutorProvided = getContainerTaskIds(curatorFrameworkOp);
		executorProvidedList.addAll(dockerExecutorProvided);

		if (StringUtils.isBlank(jobName)) {
			return executorProvidedList;
		}

		String preferListNodePath = JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PREFER_LIST);

		if (!curatorFrameworkOp.checkExists(preferListNodePath)) {
			return executorProvidedList;
		}

		String preferList = curatorFrameworkOp.getData(preferListNodePath);
		if (Strings.isNullOrEmpty(preferList)) {
			return executorProvidedList;
		}

		handlerPreferListString(curatorFrameworkOp, preferList, executors, dockerExecutorProvided,
				executorProvidedList);

		return executorProvidedList;
	}

	private void handlerPreferListString(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String preferList,
			List<String> executors, List<ExecutorProvided> dockerExecutorProvided,
			List<ExecutorProvided> executorProvidedList) {
		String[] preferExecutorList = preferList.split(",");
		for (String preferExecutor : preferExecutorList) {
			if (!preferExecutor.startsWith("@")) {
				if (!executors.contains(preferExecutor)) {
					ExecutorProvided executorProvided = new ExecutorProvided();
					executorProvided.setExecutorName(preferExecutor);
					executorProvided.setType(ExecutorProvidedType.PHYSICAL);
					executorProvided.setStatus(ExecutorProvidedStatus.DELETED);
					executorProvided.setNoTraffic(curatorFrameworkOp
							.checkExists(SaturnExecutorsNode.getExecutorNoTrafficNodePath(preferExecutor)));
					executorProvidedList.add(executorProvided);
				}
			} else {
				String executorName = preferExecutor.substring(1);
				boolean include = false;
				for (ExecutorProvided executorProvided : dockerExecutorProvided) {
					if (executorProvided.getExecutorName().equals(executorName)) {
						include = true;
						break;
					}
				}
				if (!include) {
					ExecutorProvided executorProvided = new ExecutorProvided();
					executorProvided.setExecutorName(executorName);
					executorProvided.setType(ExecutorProvidedType.DOCKER);
					executorProvided.setStatus(ExecutorProvidedStatus.DELETED);
					executorProvidedList.add(executorProvided);
				}
			}
		}
	}

	/**
	 * 先获取DCOS节点下的taskID节点；如果没有此节点，则尝试从executor节点下获取;
	 * <p>
	 * 不存在既有DCOS容器，又有K8S容器的模式。
	 */
	protected List<ExecutorProvided> getContainerTaskIds(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
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
		if (executors != null) {
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

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void setPreferList(String namespace, String jobName, String preferList, String updatedBy)
			throws SaturnJobConsoleException {
		// save to db
		JobConfig4DB oldJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (oldJobConfig == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, "设置该作业（" + jobName + "）优先Executor失败，因为该作业不存在");
		}
		// 启用状态的本地模式作业，不能设置preferList
		Boolean enabled = oldJobConfig.getEnabled();
		Boolean localMode = oldJobConfig.getLocalMode();
		if (enabled != null && enabled && localMode != null && localMode) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					String.format("启用状态的本地模式作业(%s)，不能设置优先Executor，请先禁用它", jobName));
		}
		JobConfig4DB newJobConfig = new JobConfig4DB();
		BeanUtils.copyProperties(oldJobConfig, newJobConfig);
		newJobConfig.setPreferList(preferList);
		currentJobConfigService.updateNewAndSaveOld2History(newJobConfig, oldJobConfig, updatedBy);

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
	public List<String> getCandidateUpStream(String namespace) throws SaturnJobConsoleException {
		List<String> candidateDownStream = new ArrayList<>();
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		for (JobConfig temp : unSystemJobs) {
			if (canBeUpStream(temp)) {
				candidateDownStream.add(temp.getJobName());
			}
		}
		Collections.sort(candidateDownStream);
		return candidateDownStream;
	}

	@Override
	public List<String> getCandidateDownStream(String namespace) throws SaturnJobConsoleException {
		List<String> candidateDownStream = new ArrayList<>();
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		for (JobConfig temp : unSystemJobs) {
			if (canBeDownStream(temp)) {
				candidateDownStream.add(temp.getJobName());
			}
		}
		Collections.sort(candidateDownStream);
		return candidateDownStream;
	}

	private void validateJobConfig(String namespace, JobConfig jobConfig, List<JobConfig> unSystemJobs,
			Set<JobConfig> streamChangedJobs) throws SaturnJobConsoleException {
		// 作业名必填
		String jobName = jobConfig.getJobName();
		if (StringUtils.isBlank(jobName)) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "作业名必填");
		}
		// 作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_
		if (!jobName.matches("[0-9a-zA-Z_]*")) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_");
		}
		// 依赖的作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,
		if (jobConfig.getDependencies() != null && !jobConfig.getDependencies().matches("[0-9a-zA-Z_,]*")) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "依赖的作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,");
		}
		// 作业类型必填
		if (StringUtils.isBlank(jobConfig.getJobType())) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "作业类型必填");
		}
		// 验证作业类型
		JobType jobType = JobType.getJobType(jobConfig.getJobType());
		if (jobType == JobType.UNKNOWN_JOB) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "作业类型未知");
		}
		// 如果是JAVA作业，作业实现类必填
		if (JobType.isJava(jobType) && StringUtils.isBlank(jobConfig.getJobClass())) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "对于java作业，作业实现类必填");
		}
		// 如果是消息作业，queue必填
		if (JobType.isMsg(jobType)) {
			validateQueue(jobConfig);
		}
		// 校验cron
		validateCronFieldOfJobConfig(jobConfig);
		// 校验shardingItemParameters
		validateShardingItemFieldOfJobConfig(jobConfig);
		// 不能添加系统作业
		if (jobConfig.getJobMode() != null && jobConfig.getJobMode().startsWith(JobMode.SYSTEM_PREFIX)) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "作业模式有误，不能添加系统作业");
		}
		// 校验上下游作业，并联动更新其他相关联作业的上下游
		validateStreamAndLinkingUpdateOtherJobs(namespace, jobConfig, unSystemJobs, streamChangedJobs);
	}

	protected void validateQueue(JobConfig jobConfig) throws SaturnJobConsoleException {
		if (StringUtils.isBlank(jobConfig.getQueueName())) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "对于消息作业，queue必填");
		}
	}

	private void validateStreamAndLinkingUpdateOtherJobs(String namespace, JobConfig jobConfig,
			List<JobConfig> unSystemJobs, Set<JobConfig> streamChangedJobs) throws SaturnJobConsoleException {
		Set<String> downStream = parseStreamToList(jobConfig.getDownStream());
		if (!downStream.isEmpty()) {
			validateDownStreamBasic(jobConfig, true);
		}
		Set<String> upStream = parseStreamToList(jobConfig.getUpStream());
		if (!upStream.isEmpty()) {
			validateUpStreamBasic(jobConfig, true);
		}
		// 如果是添加新作业，合并jobConfig和unSystemJobs
		List<JobConfig> newUnSystemJobs = new ArrayList<>();
		newUnSystemJobs.addAll(unSystemJobs);
		boolean included = false;
		for (JobConfig otherJob : newUnSystemJobs) {
			if (otherJob.getJobName().equals(jobConfig.getJobName())) {
				included = true;
				break;
			}
		}
		if (!included) {
			newUnSystemJobs.add(jobConfig);
		}
		// 校验上游作业，更新上游作业的downStream
		validateAndUpdateStream(jobConfig, upStream, newUnSystemJobs, streamChangedJobs, false);
		// 校验下游作业，更新下游作业的upStream
		validateAndUpdateStream(jobConfig, downStream, newUnSystemJobs, streamChangedJobs, true);
		// 校验该作业处于的路径是否有环
		getAncestors(namespace, jobConfig, newUnSystemJobs, new Stack<String>(), true);
		// 格式化上游作业，去除多余空格
		jobConfig.setUpStream(formatStream(upStream));
		// 格式化下游作业，去除多余空格
		jobConfig.setDownStream(formatStream(downStream));
	}

	private Set<String> parseStreamToList(String stream) {
		Set<String> streamList = new HashSet<>();
		if (StringUtils.isBlank(stream)) {
			return streamList;
		}
		String[] split = stream.split(",");
		for (String temp : split) {
			if (StringUtils.isNotBlank(temp)) {
				streamList.add(temp.trim());
			}
		}
		return streamList;
	}

	private void validateDownStreamBasic(JobConfig jobConfig, boolean isCurrentJob) throws SaturnJobConsoleException {
		// 只能是cron/passive作业，才能配置下游
		JobType jobType = JobType.getJobType(jobConfig.getJobType());
		if (!JobType.isCron(jobType) && !JobType.isPassive(jobType)) {
			if (isCurrentJob) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "只能是定时作业或者被动作业，才能配置下游作业");
			} else {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						"配置的上游作业(" + jobConfig.getJobName() + ")不是定时作业或被动作业");
			}
		}
		// 不能是本地模式作业，因为本地模式不能保证分片数1
		if (jobConfig.getLocalMode() != null && jobConfig.getLocalMode()) {
			if (isCurrentJob) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "非本地模式作业，才能配置下游作业");
			} else {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						"配置的上游作业(" + jobConfig.getJobName() + ")不能是本地模式作业");
			}
		}
		// 只能只有一个分片，才能配置下游
		if (jobConfig.getShardingTotalCount() != 1) {
			if (isCurrentJob) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "分片数为1，才能配置下游作业");
			} else {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						"配置的上游作业(" + jobConfig.getJobName() + ")分片数必须为1");
			}
		}
	}

	private void validateUpStreamBasic(JobConfig jobConfig, boolean isCurrentJob) throws SaturnJobConsoleException {
		// 只能是passive作业，才能配置上游
		JobType jobType = JobType.getJobType(jobConfig.getJobType());
		if (!JobType.isPassive(jobType)) {
			if (isCurrentJob) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "只能是被动作业，才能配置上游作业");
			} else {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						"配置的下游作业(" + jobConfig.getJobName() + ")不是被动作业");
			}
		}
	}

	private void validateAndUpdateStream(JobConfig jobConfig, Set<String> stream, List<JobConfig> unSystemJobs,
			Set<JobConfig> streamChangedJobs, boolean isDownStream) throws SaturnJobConsoleException {
		String jobName = jobConfig.getJobName();
		for (String elem : stream) {
			if (elem.equals(jobName)) {
				if (isDownStream) {
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "下游作业(" + elem + ")不能是该作业本身");
				} else {
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "上游作业(" + elem + ")不能是该作业本身");
				}
			}
			boolean found = false;
			for (JobConfig otherJob : unSystemJobs) {
				if (elem.equals(otherJob.getJobName())) {
					if (isDownStream) {
						validateUpStreamBasic(otherJob, false);
						otherJob.setUpStream(appendToStream(jobName, otherJob.getUpStream()));
						streamChangedJobs.add(otherJob);
					} else {
						validateDownStreamBasic(otherJob, false);
						otherJob.setDownStream(appendToStream(jobName, otherJob.getDownStream()));
						streamChangedJobs.add(otherJob);
					}
					found = true;
					break;
				}
			}
			if (!found) {
				if (isDownStream) {
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "下游作业(" + elem + ")不存在");
				} else {
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "上游作业(" + elem + ")不存在");
				}
			}
		}
		for (JobConfig otherJob : unSystemJobs) {
			String otherJobName = otherJob.getJobName();
			if (otherJobName.equals(jobName)) {
				continue;
			}
			if (stream.contains(otherJobName)) {
				continue;
			}
			if (isDownStream) {
				String upStream = removeFromStreamIfNecessary(jobName, otherJob.getUpStream());
				if (upStream != null) {
					otherJob.setUpStream(upStream);
					streamChangedJobs.add(otherJob);
				}
			} else {
				String downStream = removeFromStreamIfNecessary(jobName, otherJob.getDownStream());
				if (downStream != null) {
					otherJob.setDownStream(downStream);
					streamChangedJobs.add(otherJob);
				}
			}
		}
	}

	private String appendToStream(String jobName, String stream) {
		Set<String> streamSet = parseStreamToList(stream);
		if (StringUtils.isNotBlank(jobName)) {
			streamSet.add(jobName);
		}
		return formatStream(streamSet);
	}

	private String removeFromStreamIfNecessary(String jobName, String stream) {
		Set<String> streamSet = parseStreamToList(stream);
		if (StringUtils.isNotBlank(jobName)) {
			if (streamSet.remove(jobName)) {
				return formatStream(streamSet);
			}
		}
		return null;
	}

	private String formatStream(Set<String> streamSet) {
		StringBuilder sb = new StringBuilder();
		for (String temp : streamSet) {
			if (StringUtils.isNotBlank(temp)) {
				sb.append(temp).append(',');
			}
		}
		int length = sb.length();
		if (length > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	private Set<String> getAncestors(String namespace, JobConfig jobConfig, List<JobConfig> unSystemJobs,
			Stack<String> onePathRecords, boolean throwExceptionWhenHasARing) throws SaturnJobConsoleException {
		onePathRecords.push(jobConfig.getJobName());
		Set<String> ancestors = new HashSet<>();
		Set<String> upStream = parseStreamToList(jobConfig.getUpStream());
		for (String parent : upStream) {
			for (JobConfig otherJobConfig : unSystemJobs) {
				if (parent.equals(otherJobConfig.getJobName())) {
					if (onePathRecords.search(parent) != -1) {
						onePathRecords.push(parent);
						if (throwExceptionWhenHasARing) {
							throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
									String.format("该域(%s)作业编排有误，存在环: %s", namespace, onePathRecords));
						} else {
							log.error("{} job arrange error, because it includes a ring: {}", namespace,
									onePathRecords);
							onePathRecords.pop();
						}
					}
					if (!ancestors.contains(parent)) {
						ancestors.add(parent);
						ancestors.addAll(getAncestors(namespace, otherJobConfig, unSystemJobs, onePathRecords,
								throwExceptionWhenHasARing));
					}
					break;
				}
			}
		}
		onePathRecords.pop();
		return ancestors;
	}

	private Set<String> getDescendants(String namespace, JobConfig jobConfig, List<JobConfig> unSystemJobs,
			Stack<String> onePathRecords, boolean throwExceptionWhenHasARing) throws SaturnJobConsoleException {
		onePathRecords.push(jobConfig.getJobName());
		Set<String> descendants = new HashSet<>();
		Set<String> downStream = parseStreamToList(jobConfig.getDownStream());
		for (String child : downStream) {
			for (JobConfig otherJobConfig : unSystemJobs) {
				if (child.equals(otherJobConfig.getJobName())) {
					if (onePathRecords.search(child) != -1) {
						onePathRecords.push(child);
						if (throwExceptionWhenHasARing) {
							throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
									String.format("该域(%s)作业编排有误，存在环: %", namespace, onePathRecords));
						} else {
							log.error("{} job arrange error, because it includes a ring: {}", namespace,
									onePathRecords);
							onePathRecords.pop();
						}
					}
					if (!descendants.contains(child)) {
						descendants.add(child);
						descendants.addAll(getDescendants(namespace, otherJobConfig, unSystemJobs, onePathRecords,
								throwExceptionWhenHasARing));
					}
					break;
				}
			}
		}
		onePathRecords.pop();
		return descendants;
	}

	private void validateCronFieldOfJobConfig(JobConfig jobConfig) throws SaturnJobConsoleException {
		if (JobType.isCron(JobType.getJobType(jobConfig.getJobType()))) {
			// cron表达式必填
			if (jobConfig.getCron() == null || jobConfig.getCron().trim().isEmpty()) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "对于cron作业，cron表达式必填");
			}
			// cron表达式语法验证
			try {
				CronExpression.validateExpression(jobConfig.getCron());
			} catch (ParseException e) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "cron表达式语法有误" + e);
			}
		} else {
			jobConfig.setCron(""); // 其他类型的不需要持久化保存cron表达式
		}
	}

	private void validateShardingItemFieldOfJobConfig(JobConfig jobConfig) throws SaturnJobConsoleException {
		if (jobConfig.getLocalMode() != null && jobConfig.getLocalMode()) {
			if (jobConfig.getShardingItemParameters() == null) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "对于本地模式作业，分片参数必填。");
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
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "对于本地模式作业，分片参数必须包含如*=xx。");
				}
			}
		} else {
			// 分片参数不能小于分片总数
			if (jobConfig.getShardingTotalCount() == null || jobConfig.getShardingTotalCount() < 1) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "分片数不能为空，并且不能小于1");
			}
			if ((jobConfig.getShardingTotalCount() > 0) && (jobConfig.getShardingItemParameters() == null || jobConfig
					.getShardingItemParameters().trim().isEmpty()
					|| jobConfig.getShardingItemParameters().split(",").length < jobConfig.getShardingTotalCount())) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "分片参数不能小于分片总数");
			}
			validateShardingItemFormat(jobConfig);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void addJob(String namespace, JobConfig jobConfig, String createdBy) throws SaturnJobConsoleException {
		addOrCopyJob(namespace, jobConfig, null, createdBy);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void copyJob(String namespace, JobConfig jobConfig, String jobNameCopied, String createdBy)
			throws SaturnJobConsoleException {
		addOrCopyJob(namespace, jobConfig, jobNameCopied, createdBy);
	}

	private void addOrCopyJob(String namespace, JobConfig jobConfig, String jobNameCopied, String createdBy)
			throws SaturnJobConsoleException {
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		Set<JobConfig> streamChangedJobs = new HashSet<>();
		validateJobConfig(namespace, jobConfig, unSystemJobs, streamChangedJobs);
		// 如果数据存在相同作业名，则抛异常
		// 直接再查一次，不使用unSystemJobs，因为也不能与系统作业名相同
		String jobName = jobConfig.getJobName();
		if (currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName) != null) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, String.format("该作业(%s)已经存在", jobName));
		}
		// 如果zk存在该作业，则尝试删除
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		if (curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobName))) {
			if (!removeJobFromZk(jobName, curatorFrameworkOp)) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						String.format("该作业(%s)正在删除中，请稍后再试", jobName));
			}
		}
		// 该域作业总数不能超过一定数量
		int maxJobNum = getMaxJobNum();
		if (jobIncExceeds(namespace, maxJobNum, 1)) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					String.format("总作业数超过最大限制(%d)，作业名%s创建失败", maxJobNum, jobName));
		}
		// 如果是copy作业，则从数据库中复制被拷贝的作业的配置到新的作业配置
		JobConfig myJobConfig = jobConfig;
		if (jobNameCopied != null) {
			myJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobNameCopied);
			SaturnBeanUtils.copyPropertiesIgnoreNull(jobConfig, myJobConfig);
		}
		// 设置作业配置字段默认值，并且强制纠正某些字段
		correctConfigValueWhenAddJob(myJobConfig);
		// 添加该作业到数据库
		currentJobConfigService.create(constructJobConfig4DB(namespace, myJobConfig, createdBy, createdBy));
		// 更新关联作业的上下游
		for (JobConfig streamChangedJob : streamChangedJobs) {
			currentJobConfigService.updateStream(constructJobConfig4DB(namespace, streamChangedJob, null, createdBy));
		}
		// 添加该作业配置到zk，并联动更新关联作业的上下游
		createJobConfigToZk(myJobConfig, streamChangedJobs, curatorFrameworkOp);
	}

	private JobConfig4DB constructJobConfig4DB(String namespace, JobConfig jobConfig, String createdBy,
			String updatedBy) {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		SaturnBeanUtils.copyProperties(jobConfig, jobConfig4DB);
		Date now = new Date();
		if (StringUtils.isNotBlank(createdBy)) {
			jobConfig4DB.setCreateTime(now);
			jobConfig4DB.setCreateBy(createdBy);
		}
		jobConfig4DB.setLastUpdateTime(now);
		jobConfig4DB.setLastUpdateBy(updatedBy);
		jobConfig4DB.setNamespace(namespace);
		return jobConfig4DB;
	}

	private void correctConfigValueWhenAddJob(JobConfig jobConfig) {
		jobConfig.setDefaultValues();
		jobConfig.setEnabled(false);
		JobType jobType = JobType.getJobType(jobConfig.getJobType());
		if (JobType.isShell(jobType)) {
			jobConfig.setJobClass("");
		}
		if (JobType.isMsg(jobType)) {
			jobConfig.setFailover(false);
			jobConfig.setRerun(false);
		}
		if (JobType.isPassive(jobType)) {
			jobConfig.setRerun(false);
		}
		if (jobConfig.getLocalMode()) {
			jobConfig.setFailover(false);
		}
		boolean enabledReport = getEnabledReport(jobType, jobConfig.getCron(), jobConfig.getTimeZone());
		jobConfig.setEnabledReport(enabledReport);
		if (!enabledReport) {
			jobConfig.setFailover(false);
			jobConfig.setRerun(false);
		}
	}

	@Override
	public int getMaxJobNum() {
		int result = systemConfigService.getIntegerValue(SystemConfigProperties.MAX_JOB_NUM, DEFAULT_MAX_JOB_NUM);
		return result <= 0 ? DEFAULT_MAX_JOB_NUM : result;
	}

	private int getMaxZnodeDataLength() {
		int result = systemConfigService
				.getIntegerValue(SystemConfigProperties.MAX_ZNODE_DATA_LENGTH, DEFAULT_MAX_ZNODE_DATA_LENGTH);
		return result <= 0 ? DEFAULT_MAX_ZNODE_DATA_LENGTH : result;
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
				if (!isSystemJob(jobConfig4DB)) {
					JobConfig jobConfig = new JobConfig();
					SaturnBeanUtils.copyProperties(jobConfig4DB, jobConfig);
					unSystemJobs.add(jobConfig);
				}
			}
		}
		return unSystemJobs;
	}

	private boolean isSystemJob(JobConfig jobConfig) {
		String jobMode = jobConfig.getJobMode();
		return StringUtils.isNotBlank(jobMode) && jobMode.startsWith(JobMode.SYSTEM_PREFIX);
	}

	@Override
	public List<JobConfig> getUnSystemJobsWithCondition(String namespace, Map<String, Object> condition, int page,
			int size) throws SaturnJobConsoleException {
		List<JobConfig> unSystemJobs = new ArrayList<>();
		List<JobConfig4DB> jobConfig4DBList = getJobConfigByStatusWithCondition(namespace, condition, page, size);
		if (jobConfig4DBList != null) {
			for (JobConfig4DB jobConfig4DB : jobConfig4DBList) {
				if (!isSystemJob(jobConfig4DB)) {
					JobConfig jobConfig = new JobConfig();
					SaturnBeanUtils.copyProperties(jobConfig4DB, jobConfig);
					unSystemJobs.add(jobConfig);
				}
			}
		}
		return unSystemJobs;
	}

	private List<JobConfig4DB> getJobConfigByStatusWithCondition(String namespace, Map<String, Object> condition,
			int page, int size) throws SaturnJobConsoleException {
		JobStatus jobStatus = (JobStatus) condition.get("jobStatus");
		if (jobStatus == null) {
			return currentJobConfigService.findConfigsByNamespaceWithCondition(namespace, condition,
					PageableUtil.generatePageble(page, size));
		}

		List<JobConfig4DB> jobConfig4DBList = new ArrayList<>();
		List<JobConfig4DB> enabledJobConfigList = currentJobConfigService
				.findConfigsByNamespaceWithCondition(namespace, condition, null);
		for (JobConfig4DB jobConfig4DB : enabledJobConfigList) {
			JobStatus currentJobStatus = getJobStatus(namespace, jobConfig4DB.getJobName());
			if (jobStatus.equals(currentJobStatus)) {
				jobConfig4DBList.add(jobConfig4DB);
			}
		}
		return jobConfig4DBList;
	}

	@Override
	public int countUnSystemJobsWithCondition(String namespace, Map<String, Object> condition)
			throws SaturnJobConsoleException {
		return currentJobConfigService.countConfigsByNamespaceWithCondition(namespace, condition);
	}

	@Override
	public int countEnabledUnSystemJobs(String namespace) throws SaturnJobConsoleException {
		return currentJobConfigService.countEnabledUnSystemJobsByNamespace(namespace);
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
	public List<String> getJobNames(String namespace) throws SaturnJobConsoleException {
		List<String> jobNames = currentJobConfigService.findConfigNamesByNamespace(namespace);
		return jobNames != null ? jobNames : Lists.<String>newArrayList();
	}


	@Override
	public void persistJobFromDB(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException {
		jobConfig.setDefaultValues();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		saveJobConfigToZk(jobConfig, curatorFrameworkOp);
	}

	@Override
	public void persistJobFromDB(JobConfig jobConfig, CuratorFrameworkOp curatorFrameworkOp) {
		jobConfig.setDefaultValues();
		saveJobConfigToZk(jobConfig, curatorFrameworkOp);
	}

	/**
	 * 对于被动作业，返回true；<br>
	 * 对于定时作业，根据cron和INTERVAL_TIME_OF_ENABLED_REPORT来计算是否需要上报状态 see #286
	 */
	private boolean getEnabledReport(JobType jobType, String cron, String timeZone) {
		if (JobType.isPassive(jobType)) {
			return true;
		}

		if (!JobType.isCron(jobType)) {
			return false;
		}

		boolean enabledReport = true;
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

		return enabledReport;
	}

	private void createJobConfigToZk(JobConfig jobConfig, Set<JobConfig> streamChangedJobs,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		try {
			String jobName = jobConfig.getJobName();
			// 添加作业根节点和config结点
			curatorFrameworkOp.create(JobNodePath.getConfigNodePath(jobName), "");
			CuratorFrameworkOp.CuratorTransactionOp curatorTransactionOp = curatorFrameworkOp.inTransaction();
			// 数据库有可能有重复作业的数据，去重，zk无需更新两次
			Collection<JobConfig> streamChangedJobsNew = removeDuplicateByJobName(streamChangedJobs);
			// 更新关联作业的上下游
			for (JobConfig streamChangedJob : streamChangedJobsNew) {
				String changedJobName = streamChangedJob.getJobName();
				if (!curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(changedJobName))) {
					// 数据库存在该作业，但是zk不存在该作业，为垃圾数据
					log.warn("the job({}) config node is not existing in zk", changedJobName);
					continue;
				}
				curatorTransactionOp
						.replaceIfChanged(JobNodePath.getConfigNodePath(changedJobName, CONFIG_ITEM_UPSTREAM),
								streamChangedJob.getUpStream())
						.replaceIfChanged(JobNodePath.getConfigNodePath(changedJobName, CONFIG_ITEM_DOWNSTREAM),
								streamChangedJob.getDownStream());
			}
			// 添加作业
			curatorTransactionOp
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED), jobConfig.getEnabled())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DESCRIPTION), jobConfig.getDescription())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CUSTOM_CONTEXT),
							jobConfig.getCustomContext())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE), jobConfig.getJobType())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_MODE), jobConfig.getJobMode())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS),
							jobConfig.getShardingItemParameters())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_PARAMETER),
							jobConfig.getJobParameter())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_QUEUE_NAME), jobConfig.getQueueName())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CHANNEL_NAME),
							jobConfig.getChannelName())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_FAILOVER), jobConfig.getFailover())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_MONITOR_EXECUTION), "true")
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_4_ALARM_SECONDS),
							jobConfig.getTimeout4AlarmSeconds())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_SECONDS),
							jobConfig.getTimeoutSeconds())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIME_ZONE), jobConfig.getTimeZone())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON), jobConfig.getCron())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_DATE),
							jobConfig.getPausePeriodDate())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_TIME),
							jobConfig.getPausePeriodTime())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PROCESS_COUNT_INTERVAL_SECONDS),
							jobConfig.getProcessCountIntervalSeconds())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT),
							jobConfig.getShardingTotalCount())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHOW_NORMAL_LOG),
							jobConfig.getShowNormalLog())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOAD_LEVEL), jobConfig.getLoadLevel())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_DEGREE), jobConfig.getJobDegree())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED_REPORT),
							jobConfig.getEnabledReport())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PREFER_LIST), jobConfig.getPreferList())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_USE_DISPREFER_LIST),
							jobConfig.getUseDispreferList())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOCAL_MODE), jobConfig.getLocalMode())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_USE_SERIAL), jobConfig.getUseSerial())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DEPENDENCIES),
							jobConfig.getDependencies())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_GROUPS), jobConfig.getGroups())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_RERUN), jobConfig.getRerun())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_UPSTREAM), jobConfig.getUpStream())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DOWNSTREAM), jobConfig.getDownStream())
					.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS), jobConfig.getJobClass());
			// 注意！！！ jobClass要最后更新，因为executor认为该结点为作业添加完成

			// 提交事务
			curatorTransactionOp.commit();
		} catch (Exception e) {
			log.error("create job to zk failed", e);
			throw new SaturnJobConsoleException(e);
		}
	}

	private void saveJobConfigToZk(JobConfig jobConfig, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String jobName = jobConfig.getJobName();
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED),
				jobConfig.getEnabled());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DESCRIPTION),
				jobConfig.getDescription());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CUSTOM_CONTEXT),
				jobConfig.getCustomContext());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE),
				jobConfig.getJobType());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_MODE),
				jobConfig.getJobMode());
		curatorFrameworkOp
				.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS),
						jobConfig.getShardingItemParameters());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_PARAMETER),
				jobConfig.getJobParameter());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_QUEUE_NAME),
				jobConfig.getQueueName());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CHANNEL_NAME),
				jobConfig.getChannelName());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_FAILOVER),
				jobConfig.getFailover());
		curatorFrameworkOp
				.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_MONITOR_EXECUTION), "true");
		curatorFrameworkOp
				.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_4_ALARM_SECONDS),
						jobConfig.getTimeout4AlarmSeconds());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_SECONDS),
				jobConfig.getTimeoutSeconds());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIME_ZONE),
				jobConfig.getTimeZone());
		curatorFrameworkOp
				.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON), jobConfig.getCron());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_DATE),
				jobConfig.getPausePeriodDate());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_TIME),
				jobConfig.getPausePeriodTime());
		curatorFrameworkOp.fillJobNodeIfNotExist(
				JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PROCESS_COUNT_INTERVAL_SECONDS),
				jobConfig.getProcessCountIntervalSeconds());
		curatorFrameworkOp
				.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT),
						jobConfig.getShardingTotalCount());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHOW_NORMAL_LOG),
				jobConfig.getShowNormalLog());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOAD_LEVEL),
				jobConfig.getLoadLevel());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_DEGREE),
				jobConfig.getJobDegree());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED_REPORT),
				jobConfig.getEnabledReport());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PREFER_LIST),
				jobConfig.getPreferList());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_USE_DISPREFER_LIST),
				jobConfig.getUseDispreferList());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOCAL_MODE),
				jobConfig.getLocalMode());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_USE_SERIAL),
				jobConfig.getUseSerial());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DEPENDENCIES),
				jobConfig.getDependencies());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_GROUPS),
				jobConfig.getGroups());
		curatorFrameworkOp
				.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_RERUN), jobConfig.getRerun());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_UPSTREAM),
				jobConfig.getUpStream());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DOWNSTREAM),
				jobConfig.getDownStream());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS),
				jobConfig.getJobClass());
		// 注意！！！ jobClass要最后更新，因为executor认为该结点为作业添加完成
	}

	@Override
	public List<BatchJobResult> importJobs(String namespace, MultipartFile file, String createdBy)
			throws SaturnJobConsoleException {
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
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						String.format("总作业数超过最大限制(%d)，导入失败", maxJobNum));
			}
			return doCreateJobFromImportFile(namespace, jobConfigList, createdBy);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}

	protected List<BatchJobResult> doCreateJobFromImportFile(String namespace, List<JobConfig> jobConfigList,
			String createdBy) {
		List<BatchJobResult> results = new ArrayList<>();
		for (JobConfig jobConfig : jobConfigList) {
			BatchJobResult batchJobResult = new BatchJobResult();
			batchJobResult.setJobName(jobConfig.getJobName());
			try {
				addJob(namespace, jobConfig, createdBy);
				batchJobResult.setSuccess(true);
			} catch (SaturnJobConsoleException e) {
				batchJobResult.setSuccess(false);
				batchJobResult.setMessage(e.getMessage());
				log.warn("exception: {}", e);
			} catch (Exception e) {
				batchJobResult.setSuccess(false);
				batchJobResult.setMessage(e.toString());
				log.warn("exception: {}", e);
			}
			results.add(batchJobResult);
		}
		return results;
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
		String jobName = getContents(rowCells, 0);
		if (jobName == null || jobName.trim().isEmpty()) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 1, "作业名必填。"));
		}
		if (!jobName.matches("[0-9a-zA-Z_]*")) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 1, "作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_。"));
		}
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);

		String jobType = getContents(rowCells, 1);
		if (jobType == null || jobType.trim().isEmpty()) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 2, "作业类型必填。"));
		}
		JobType jobTypeObj = JobType.getJobType(jobType);
		if (jobTypeObj == JobType.UNKNOWN_JOB) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 2, "作业类型未知。"));
		}
		jobConfig.setJobType(jobType);

		String jobClass = getContents(rowCells, 2);
		if (JobType.isJava(jobTypeObj) && (jobClass == null || jobClass.trim().isEmpty())) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 3, "对于java作业，作业实现类必填。"));
		}
		jobConfig.setJobClass(jobClass);

		String cron = getContents(rowCells, 3);
		if (JobType.isCron(jobTypeObj)) {
			if (cron == null || cron.trim().isEmpty()) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						createExceptionMessage(sheetNumber, rowNumber, 4, "对于cron作业，cron表达式必填。"));
			}
			cron = cron.trim();
			try {
				CronExpression.validateExpression(cron);
			} catch (ParseException e) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						createExceptionMessage(sheetNumber, rowNumber, 4, "cron表达式语法有误，" + e));
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
			if (tmp != null && !tmp.trim().isEmpty()) {
				try {
					shardingTotalCount = Integer.parseInt(tmp);
				} catch (NumberFormatException e) {
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
							createExceptionMessage(sheetNumber, rowNumber, 7, "分片数有误，" + e));
				}
			} else {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						createExceptionMessage(sheetNumber, rowNumber, 7, "分片数必填"));
			}
			if (shardingTotalCount < 1) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						createExceptionMessage(sheetNumber, rowNumber, 7, "分片数不能小于1"));
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
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 8, "超时（Kill线程/进程）时间有误，" + e));
		}
		jobConfig.setTimeoutSeconds(timeoutSeconds);

		jobConfig.setJobParameter(getContents(rowCells, 8));

		String shardingItemParameters = getContents(rowCells, 9);
		if (jobConfig.getLocalMode()) {
			if (shardingItemParameters == null || shardingItemParameters.trim().isEmpty()) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
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
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
							createExceptionMessage(sheetNumber, rowNumber, 10, "对于本地模式作业，分片参数必须包含如*=xx。"));
				}
			}
		} else if ((shardingTotalCount > 0) && (shardingItemParameters == null || shardingItemParameters.trim()
				.isEmpty() || shardingItemParameters.split(",").length < shardingTotalCount)) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 10, "分片参数不能小于分片总数。"));
		}
		jobConfig.setShardingItemParameters(shardingItemParameters);

		jobConfig.setQueueName(getContents(rowCells, 10));
		jobConfig.setChannelName(getContents(rowCells, 11));
		jobConfig.setPreferList(getContents(rowCells, 12));
		jobConfig.setUseDispreferList(!Boolean.parseBoolean(getContents(rowCells, 13)));

		int processCountIntervalSeconds = 300;
		try {
			String tmp = getContents(rowCells, 14);
			if (tmp != null && !tmp.trim().isEmpty()) {
				processCountIntervalSeconds = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 15, "统计处理数据量的间隔秒数有误，" + e));
		}
		jobConfig.setProcessCountIntervalSeconds(processCountIntervalSeconds);

		int loadLevel = 1;
		try {
			String tmp = getContents(rowCells, 15);
			if (tmp != null && !tmp.trim().isEmpty()) {
				loadLevel = Integer.parseInt(tmp.trim());
			}
		} catch (NumberFormatException e) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 16, "负荷有误，" + e));
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
			throw new SaturnJobConsoleException(createExceptionMessage(sheetNumber, rowNumber, 21, "作业重要等级有误，" + e));
		}
		jobConfig.setJobDegree(jobDegree);

		// 第21列，上报运行状态失效，由算法决定是否上报，看下面setEnabledReport时的逻辑，看addJob

		String jobMode = getContents(rowCells, 22);

		if (jobMode != null && jobMode.startsWith(com.vip.saturn.job.console.domain.JobMode.SYSTEM_PREFIX)) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 23, "作业模式有误，不能添加系统作业"));
		}
		jobConfig.setJobMode(jobMode);

		String dependencies = getContents(rowCells, 23);
		if (dependencies != null && !dependencies.matches("[0-9a-zA-Z_,]*")) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
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
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 26, "超时（告警）时间有误，" + e));
		}
		jobConfig.setTimeout4AlarmSeconds(timeout4AlarmSeconds);

		String timeZone = getContents(rowCells, 26);
		if (timeZone == null || timeZone.trim().length() == 0) {
			timeZone = SaturnConstants.TIME_ZONE_ID_DEFAULT;
		} else {
			timeZone = timeZone.trim();
			if (!SaturnConstants.TIME_ZONE_IDS.contains(timeZone)) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						createExceptionMessage(sheetNumber, rowNumber, 27, "时区有误"));
			}
		}
		jobConfig.setTimeZone(timeZone);

		Boolean failover = null;
		String failoverStr = getContents(rowCells, 27);
		if (StringUtils.isNotBlank(failoverStr)) {
			failover = Boolean.valueOf(failoverStr.trim());
			if (failover) {
				if (jobConfig.getLocalMode()) {
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
							createExceptionMessage(sheetNumber, rowNumber, 28, "本地模式不支持failover"));
				}
				if (JobType.isMsg(jobTypeObj)) {
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
							createExceptionMessage(sheetNumber, rowNumber, 28, "消息作业不支持failover"));
				}
				// 如果不上报运行状态，则强制设置为false
				// 上报运行状态失效，由算法决定是否上报，看下面setEnabledReport时的逻辑，看addJob
			}
		}
		jobConfig.setFailover(failover);

		Boolean rerun = null;
		String rerunStr = getContents(rowCells, 28);
		if (StringUtils.isNotBlank(rerunStr)) {
			rerun = Boolean.valueOf(rerunStr.trim());
			if (rerun) {
				if (JobType.isMsg(jobTypeObj)) {
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
							createExceptionMessage(sheetNumber, rowNumber, 29, "消息作业不支持rerun"));
				}
				if (JobType.isPassive(jobTypeObj)) {
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
							createExceptionMessage(sheetNumber, rowNumber, 29, "被动作业不支持rerun"));
				}
				// 如果不上报运行状态，则强制设置为false
				// 上报运行状态失效，由算法决定是否上报，看下面setEnabledReport时的逻辑，看addJob
			}
		}
		jobConfig.setRerun(rerun);

		String upStream = getContents(rowCells, 29);
		if (upStream != null && !upStream.matches("[0-9a-zA-Z_,]*")) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 30, "上游作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,"));
		}
		jobConfig.setUpStream(upStream);

		String downStream = getContents(rowCells, 30);
		if (downStream != null && !downStream.matches("[0-9a-zA-Z_,]*")) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					createExceptionMessage(sheetNumber, rowNumber, 31, "下游作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,"));
		}
		jobConfig.setDownStream(downStream);

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
			setExcelHeader(sheet1);
			List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
			// sort by jobName
			Collections.sort(unSystemJobs, new Comparator<JobConfig>() {
				@Override
				public int compare(JobConfig o1, JobConfig o2) {
					return o1.getJobName().compareTo(o2.getJobName());
				}
			});
			setExcelContent(namespace, sheet1, unSystemJobs);

			writableWorkbook.write();
			writableWorkbook.close();

			return tmp;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}

	protected void setExcelContent(String namespace, WritableSheet sheet1, List<JobConfig> unSystemJobs)
			throws SaturnJobConsoleException, WriteException {
		if (unSystemJobs != null && !unSystemJobs.isEmpty()) {
			CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
			for (int i = 0; i < unSystemJobs.size(); i++) {
				String jobName = unSystemJobs.get(i).getJobName();
				sheet1.addCell(new Label(0, i + 1, jobName));
				sheet1.addCell(new Label(1, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE))));
				sheet1.addCell(new Label(2, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS))));
				sheet1.addCell(new Label(3, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON))));
				sheet1.addCell(new Label(4, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DESCRIPTION))));
				sheet1.addCell(new Label(5, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOCAL_MODE))));
				sheet1.addCell(new Label(6, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT))));
				sheet1.addCell(new Label(7, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_SECONDS))));
				sheet1.addCell(new Label(8, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_PARAMETER))));
				sheet1.addCell(new Label(9, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS))));
				sheet1.addCell(new Label(10, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_QUEUE_NAME))));
				sheet1.addCell(new Label(11, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CHANNEL_NAME))));
				sheet1.addCell(new Label(12, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PREFER_LIST))));
				String useDispreferList = curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_USE_DISPREFER_LIST));
				if (useDispreferList != null) {
					useDispreferList = String.valueOf(!Boolean.parseBoolean(useDispreferList));
				}
				sheet1.addCell(new Label(13, i + 1, useDispreferList));
				sheet1.addCell(new Label(14, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PROCESS_COUNT_INTERVAL_SECONDS))));
				sheet1.addCell(new Label(15, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOAD_LEVEL))));
				sheet1.addCell(new Label(16, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHOW_NORMAL_LOG))));
				sheet1.addCell(new Label(17, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_DATE))));
				sheet1.addCell(new Label(18, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_TIME))));
				sheet1.addCell(new Label(19, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, JobServiceImpl.CONFIG_ITEM_USE_SERIAL))));
				sheet1.addCell(new Label(20, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_DEGREE))));
				sheet1.addCell(new Label(21, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED_REPORT))));
				sheet1.addCell(new Label(22, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_MODE))));
				sheet1.addCell(new Label(23, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DEPENDENCIES))));
				sheet1.addCell(new Label(24, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_GROUPS))));
				sheet1.addCell(new Label(25, i + 1, curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_4_ALARM_SECONDS))));
				sheet1.addCell(new Label(26, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIME_ZONE))));
				sheet1.addCell(new Label(27, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_FAILOVER))));
				sheet1.addCell(new Label(28, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_RERUN))));
				sheet1.addCell(new Label(29, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_UPSTREAM))));
				sheet1.addCell(new Label(30, i + 1,
						curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DOWNSTREAM))));
			}
		}
	}

	protected void setExcelHeader(WritableSheet sheet1) throws WriteException {
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
		setCellComment(dependenciesLabel, "作业的启用、禁用会检查依赖关系的作业的状态。依赖多个作业，使用英文逗号给开。该字段已过期。");
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

		sheet1.addCell(new Label(27, 0, "failover"));

		sheet1.addCell(new Label(28, 0, "失败重跑"));

		Label upStream = new Label(29, 0, "上游作业");
		setCellComment(upStream, "上游作业执行成功后，触发本作业执行。多个上游作业使用英文逗号隔开。");
		sheet1.addCell(upStream);

		Label downStream = new Label(30, 0, "下游作业");
		setCellComment(downStream, "该作业执行成功后，触发下游作业执行。多个下游作业使用英文逗号隔开。");
		sheet1.addCell(downStream);
	}

	protected void setCellComment(WritableCell cell, String comment) {
		WritableCellFeatures cellFeatures = new WritableCellFeatures();
		cellFeatures.setComment(comment);
		cell.setCellFeatures(cellFeatures);
	}

	@Override
	public ArrangeLayout getArrangeLayout(String namespace) throws SaturnJobConsoleException {
		ArrangeLayout arrangeLayout = new ArrangeLayout();
		// get all ArrangeNodes
		Map<String, ArrangeNode> nodeMap = new HashMap<>();
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		Map<String, JobConfig> unSystemJobsMap = new HashMap<>();
		for (JobConfig jobConfig : unSystemJobs) {
			String jobName = jobConfig.getJobName();
			unSystemJobsMap.put(jobName, jobConfig);
			ArrangeNode node = nodeMap.get(jobName);
			if (node == null) {
				node = new ArrangeNode();
				node.setName(jobName);
				nodeMap.put(jobName, node);
			}
			if (StringUtils.isNotBlank(jobConfig.getDownStream())) {
				for (String split : jobConfig.getDownStream().split(",")) {
					String temp = split.trim();
					if (StringUtils.isNotBlank(temp)) {
						node.getChildren().add(temp);
					}
				}
			}
		}
		Collection<ArrangeNode> nodes = nodeMap.values();
		// set paths
		for (ArrangeNode node : nodes) {
			String name = node.getName();
			for (String child : node.getChildren()) {
				ArrangePath path = new ArrangePath();
				path.setSource(name);
				path.setTarget(child);
				arrangeLayout.getPaths().add(path);
			}
		}
		Collections.sort(arrangeLayout.getPaths(), new Comparator<ArrangePath>() {
			@Override
			public int compare(ArrangePath o1, ArrangePath o2) {
				int compare1 = o1.getSource().compareTo(o2.getSource());
				return compare1 != 0 ? compare1 : o1.getTarget().compareTo(o2.getTarget());
			}
		});
		// set levels
		for (ArrangeNode node : nodes) {
			int maxLevel = getMaxArrangeNodeLevel(node, 0, nodes, new Stack<String>());
			if (maxLevel > node.getLevel()) {
				node.setLevel(maxLevel);
			}
		}
		int maxLevel = 0;
		for (ArrangeNode node : nodes) {
			maxLevel = Math.max(maxLevel, node.getLevel());
		}
		for (int i = 0; i <= maxLevel; i++) {
			arrangeLayout.getLevels().add(new ArrayList<ArrangeLevel>());
		}
		for (ArrangeNode node : nodes) {
			int level = node.getLevel();
			if (level == 0 && node.getChildren().isEmpty()) {
				continue;
			}
			ArrangeLevel arrangeLevel = new ArrangeLevel();
			SaturnBeanUtils.copyProperties(node, arrangeLevel);
			arrangeLevel.setDescription(unSystemJobsMap.get(node.getName()).getDescription());
			arrangeLevel.setJobStatus(getJobStatus(namespace, unSystemJobsMap.get(node.getName())));
			arrangeLayout.getLevels().get(level).add(arrangeLevel);
		}
		for (int i = 0; i <= maxLevel; i++) {
			Collections.sort(arrangeLayout.getLevels().get(i), new Comparator<ArrangeLevel>() {
				@Override
				public int compare(ArrangeLevel o1, ArrangeLevel o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		}
		return arrangeLayout;
	}

	private int getMaxArrangeNodeLevel(ArrangeNode currentNode, int level, Collection<ArrangeNode> nodes,
			Stack<String> onePathRecords) throws SaturnJobConsoleException {
		String currentName = currentNode.getName();
		onePathRecords.push(currentName);
		int maxLevel = level;
		for (ArrangeNode node : nodes) {
			if (node.getChildren().contains(currentName)) {
				String name = node.getName();
				if (onePathRecords.search(name) != -1) {
					onePathRecords.push(name);
					throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "作业编排不允许有环，形成环的作业有: " + onePathRecords);
				}
				maxLevel = Math.max(maxLevel, getMaxArrangeNodeLevel(node, level + 1, nodes, onePathRecords));
			}
		}
		onePathRecords.pop();
		return maxLevel;
	}

	@Override
	public JobConfig getJobConfigFromZK(String namespace, String jobName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		JobConfig result = new JobConfig();
		result.setJobName(jobName);
		result.setJobType(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)));
		result.setJobClass(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)));
		// 兼容旧版没有msg_job。
		if (StringUtils.isBlank(result.getJobType())) {
			if (result.getJobClass().indexOf("script") >= 0) {
				result.setJobType(JobType.SHELL_JOB.name());
			} else {
				result.setJobType(JobType.JAVA_JOB.name());
			}
		}
		result.setShardingTotalCount(Integer.valueOf(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT))));
		String timeZone = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIME_ZONE));
		if (Strings.isNullOrEmpty(timeZone)) {
			result.setTimeZone(SaturnConstants.TIME_ZONE_ID_DEFAULT);
		} else {
			result.setTimeZone(timeZone);
		}
		result.setCron(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)));
		result.setPausePeriodDate(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_DATE)));
		result.setPausePeriodTime(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_TIME)));
		result.setShardingItemParameters(curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)));
		result.setJobParameter(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_PARAMETER)));
		result.setProcessCountIntervalSeconds(Integer.valueOf(curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PROCESS_COUNT_INTERVAL_SECONDS))));
		String timeout4AlarmSecondsStr = curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_4_ALARM_SECONDS));
		if (Strings.isNullOrEmpty(timeout4AlarmSecondsStr)) {
			result.setTimeout4AlarmSeconds(0);
		} else {
			result.setTimeout4AlarmSeconds(Integer.valueOf(timeout4AlarmSecondsStr));
		}
		result.setTimeoutSeconds(Integer.valueOf(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_SECONDS))));
		String lv = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOAD_LEVEL));
		if (Strings.isNullOrEmpty(lv)) {
			result.setLoadLevel(1);
		} else {
			result.setLoadLevel(Integer.valueOf(lv));
		}
		String jobDegree = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_DEGREE));
		if (Strings.isNullOrEmpty(jobDegree)) {
			result.setJobDegree(0);
		} else {
			result.setJobDegree(Integer.valueOf(jobDegree));
		}
		result.setEnabled(Boolean.valueOf(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED))));// 默认是禁用的
		result.setPreferList(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PREFER_LIST)));
		String useDispreferList = curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_USE_DISPREFER_LIST));
		if (Strings.isNullOrEmpty(useDispreferList)) {
			result.setUseDispreferList(null);
		} else {
			result.setUseDispreferList(Boolean.valueOf(useDispreferList));
		}
		result.setLocalMode(Boolean.valueOf(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOCAL_MODE))));
		result.setDependencies(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DEPENDENCIES)));
		result.setGroups(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_GROUPS)));
		result.setDescription(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DESCRIPTION)));
		result.setJobMode(curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, JobServiceImpl.CONFIG_ITEM_JOB_MODE)));
		result.setUseSerial(Boolean.valueOf(curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, JobServiceImpl.CONFIG_ITEM_USE_SERIAL))));
		result.setQueueName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_QUEUE_NAME)));
		result.setChannelName(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CHANNEL_NAME)));
		if (!curatorFrameworkOp
				.checkExists(JobNodePath.getConfigNodePath(jobName, JobServiceImpl.CONFIG_ITEM_SHOW_NORMAL_LOG))) {
			curatorFrameworkOp.create(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHOW_NORMAL_LOG));
		}
		String enabledReport = curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED_REPORT));
		Boolean enabledReportValue = Boolean.valueOf(enabledReport);
		if (Strings.isNullOrEmpty(enabledReport)) {
			enabledReportValue = true;
		}
		result.setEnabledReport(enabledReportValue);
		result.setShowNormalLog(Boolean.valueOf(
				curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHOW_NORMAL_LOG))));
		return result;
	}

	@Override
	public JobConfig getJobConfig(String namespace, String jobName) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig4DB == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, String.format("该作业(%s)不存在", jobName));
		}
		JobConfig jobConfig = new JobConfig();
		SaturnBeanUtils.copyProperties(jobConfig4DB, jobConfig);
		return jobConfig;
	}

	@Override
	public JobStatus getJobStatus(String namespace, String jobName) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, "不能获取该作业（" + jobName + "）的状态，因为该作业不存在");
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		return getJobStatus(jobName, curatorFrameworkOp, jobConfig.getEnabled());
	}

	@Override
	public JobStatus getJobStatus(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		return getJobStatus(jobConfig.getJobName(), curatorFrameworkOp, jobConfig.getEnabled());
	}

	@Override
	public boolean isJobShardingAllocatedExecutor(String namespace, String jobName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String executorsPath = JobNodePath.getServerNodePath(jobName);
		List<String> executors = curatorFrameworkOp.getChildren(executorsPath);
		if (CollectionUtils.isEmpty(executors)) {
			return false;
		}
		for (String executor : executors) {
			String sharding = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executor, "sharding"));
			if (StringUtils.isNotBlank(sharding)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<String> getJobServerList(String namespace, String jobName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String executorsPath = JobNodePath.getServerNodePath(jobName);
		List<String> executors = curatorFrameworkOp.getChildren(executorsPath);
		if (executors == null || CollectionUtils.isEmpty(executors)) {
			return Lists.newArrayList();
		}

		return executors;
	}

	@Override
	public GetJobConfigVo getJobConfigVo(String namespace, String jobName) throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (jobConfig4DB == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, String.format("该作业(%s)不存在", jobName));
		}
		GetJobConfigVo getJobConfigVo = new GetJobConfigVo();
		JobConfig jobConfig = new JobConfig();
		SaturnBeanUtils.copyProperties(jobConfig4DB, jobConfig);
		jobConfig.setDefaultValues();
		getJobConfigVo.copyFrom(jobConfig);

		getJobConfigVo.setTimeZonesProvided(Arrays.asList(TimeZone.getAvailableIDs()));
		getJobConfigVo.setPreferListProvided(getCandidateExecutors(namespace, jobName));
		getJobConfigVo.setUpStreamProvided(getCandidateUpStream(namespace, jobConfig));
		getJobConfigVo.setDownStreamProvided(getCandidateDownStream(namespace, jobConfig));

		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		getJobConfigVo
				.setStatus(getJobStatus(getJobConfigVo.getJobName(), curatorFrameworkOp, getJobConfigVo.getEnabled()));

		return getJobConfigVo;
	}

	private List<String> getCandidateDownStream(String namespace, JobConfig jobConfig)
			throws SaturnJobConsoleException {
		List<String> candidateDownStream = new ArrayList<>();
		if (!canBeUpStream(jobConfig)) {
			return candidateDownStream;
		}
		Set<String> downStream = parseStreamToList(jobConfig.getDownStream());
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		Set<String> ancestors = getAncestors(namespace, jobConfig, unSystemJobs, new Stack<String>(), false);
		for (JobConfig otherJob : unSystemJobs) {
			String otherJobName = otherJob.getJobName();
			if (!jobConfig.getJobName().equals(otherJobName) && !downStream.contains(otherJobName) && !ancestors
					.contains(otherJobName) && canBeDownStream(otherJob)) {
				candidateDownStream.add(otherJobName);
			}
		}
		return candidateDownStream;
	}

	private boolean canBeDownStream(JobConfig jobConfig) {
		return JobType.isPassive(JobType.getJobType(jobConfig.getJobType()));
	}

	private List<String> getCandidateUpStream(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException {
		List<String> candidateUpStream = new ArrayList<>();
		if (!canBeDownStream(jobConfig)) {
			return candidateUpStream;
		}
		Set<String> upStream = parseStreamToList(jobConfig.getUpStream());
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		Set<String> descendants = getDescendants(namespace, jobConfig, unSystemJobs, new Stack<String>(), false);
		for (JobConfig otherJob : unSystemJobs) {
			String otherJobName = otherJob.getJobName();
			if (jobConfig.getJobName().equals(otherJobName)) {
				continue;
			}
			if (upStream.contains(otherJobName)) {
				continue;
			}
			if (descendants.contains(otherJobName)) {
				continue;
			}
			if (canBeUpStream(otherJob)) {
				candidateUpStream.add(otherJobName);
			}
		}
		return candidateUpStream;
	}

	private boolean canBeUpStream(JobConfig jobConfig) {
		JobType jobType = JobType.getJobType(jobConfig.getJobType());
		if (!JobType.isCron(jobType) && !JobType.isPassive(jobType)) {
			return false;
		}
		if (jobConfig.getLocalMode() == Boolean.TRUE) {
			return false;
		}
		if (jobConfig.getShardingTotalCount() != null && jobConfig.getShardingTotalCount() > 1) {
			return false;
		}
		return true;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateJobConfig(String namespace, JobConfig jobConfig, String updatedBy)
			throws SaturnJobConsoleException {
		JobConfig4DB oldJobConfig4DB = currentJobConfigService
				.findConfigByNamespaceAndJobName(namespace, jobConfig.getJobName());
		if (oldJobConfig4DB == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED,
					String.format("该作业(%s)不存在", jobConfig.getJobName()));
		}
		// 从数据库拿出老的数据，将需要更新的数据赋值（为空的字段视为不需要更新）
		JobConfig4DB newJobConfig4DB = new JobConfig4DB();
		SaturnBeanUtils.copyProperties(oldJobConfig4DB, newJobConfig4DB);
		SaturnBeanUtils.copyPropertiesIgnoreNull(jobConfig, newJobConfig4DB);
		// 与老的数据库中的该作业的配置对比，如果没有改变，则直接返回
		if (oldJobConfig4DB.equals(newJobConfig4DB)) {
			return;
		}
		// 设置作业配置字段默认值，并且强制纠正某些字段
		correctConfigValueWhenUpdateJob(newJobConfig4DB);
		// 校验作业配置
		List<JobConfig> unSystemJobs = getUnSystemJobs(namespace);
		Set<JobConfig> streamChangedJobs = new HashSet<>();
		validateJobConfig(namespace, newJobConfig4DB, unSystemJobs, streamChangedJobs);
		// 更新该作业到数据库
		currentJobConfigService.updateNewAndSaveOld2History(newJobConfig4DB, oldJobConfig4DB, updatedBy);
		// 更新关联作业的上下游
		for (JobConfig streamChangedJob : streamChangedJobs) {
			currentJobConfigService.updateStream(constructJobConfig4DB(namespace, streamChangedJob, null, updatedBy));
		}
		// 更新作业配置到zk，并联动更新关联作业的上下游
		updateJobConfigToZk(newJobConfig4DB, streamChangedJobs, registryCenterService.getCuratorFrameworkOp(namespace));
	}

	private void correctConfigValueWhenUpdateJob(JobConfig jobConfig) {
		// 对不符合要求的字段重新设置为默认值
		jobConfig.setDefaultValues();
		// 消息作业不failover不rerun
		JobType jobType = JobType.getJobType(jobConfig.getJobType());
		if (JobType.isMsg(jobType)) {
			jobConfig.setFailover(false);
			jobConfig.setRerun(false);
		}
		// 被动作业不rerun
		if (JobType.isPassive(jobType)) {
			jobConfig.setRerun(false);
		}
		// 本地模式不failover
		if (jobConfig.getLocalMode()) {
			jobConfig.setFailover(false);
		}
		// 不上报作业不failover不rerun
		if (!jobConfig.getEnabledReport()) {
			jobConfig.setFailover(false);
			jobConfig.setRerun(false);
		}
	}

	private void updateJobConfigToZk(JobConfig jobConfig, Set<JobConfig> streamChangedJobs,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		try {
			String jobName = jobConfig.getJobName();
			// 当关闭上报时，要清理execution节点
			if (jobConfig.getEnabledReport() == Boolean.FALSE) {
				log.info("the switch of enabledReport set to false, now deleting the execution zk node");
				String executionNodePath = JobNodePath.getExecutionNodePath(jobName);
				if (curatorFrameworkOp.checkExists(executionNodePath)) {
					curatorFrameworkOp.deleteRecursive(executionNodePath);
				}
			}
			CuratorFrameworkOp.CuratorTransactionOp curatorTransactionOp = curatorFrameworkOp.inTransaction();
			// 数据库有可能有重复作业的数据，去重，zk无需更新两次
			Collection<JobConfig> streamChangedJobsNew = removeDuplicateByJobName(streamChangedJobs);
			// 更新关联作业的上下游
			for (JobConfig streamChangedJob : streamChangedJobsNew) {
				String changedJobName = streamChangedJob.getJobName();
				if (!curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(changedJobName))) {
					// 数据库存在该作业，但是zk不存在该作业，为垃圾数据
					log.warn("the job({}) config node is not existing in ZK", changedJobName);
					continue;
				}
				curatorTransactionOp
						.replaceIfChanged(JobNodePath.getConfigNodePath(changedJobName, CONFIG_ITEM_UPSTREAM),
								streamChangedJob.getUpStream())
						.replaceIfChanged(JobNodePath.getConfigNodePath(changedJobName, CONFIG_ITEM_DOWNSTREAM),
								streamChangedJob.getDownStream());
			}
			// 更新作业
			curatorTransactionOp.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED),
					jobConfig.getEnabled())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DESCRIPTION),
							jobConfig.getDescription())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CUSTOM_CONTEXT),
							jobConfig.getCustomContext())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE),
							jobConfig.getJobType())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_MODE),
							jobConfig.getJobMode())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS),
							jobConfig.getShardingItemParameters())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_PARAMETER),
							jobConfig.getJobParameter())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_QUEUE_NAME),
							jobConfig.getQueueName())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CHANNEL_NAME),
							jobConfig.getChannelName())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_FAILOVER),
							jobConfig.getFailover())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_MONITOR_EXECUTION), "true")
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_4_ALARM_SECONDS),
							jobConfig.getTimeout4AlarmSeconds())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_SECONDS),
							jobConfig.getTimeoutSeconds())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIME_ZONE),
							jobConfig.getTimeZone())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON), jobConfig.getCron())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_DATE),
							jobConfig.getPausePeriodDate())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PAUSE_PERIOD_TIME),
							jobConfig.getPausePeriodTime()).replaceIfChanged(
					JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PROCESS_COUNT_INTERVAL_SECONDS),
					jobConfig.getProcessCountIntervalSeconds())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT),
							jobConfig.getShardingTotalCount())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHOW_NORMAL_LOG),
							jobConfig.getShowNormalLog())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOAD_LEVEL),
							jobConfig.getLoadLevel())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_DEGREE),
							jobConfig.getJobDegree())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED_REPORT),
							jobConfig.getEnabledReport())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PREFER_LIST),
							jobConfig.getPreferList())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_USE_DISPREFER_LIST),
							jobConfig.getUseDispreferList())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOCAL_MODE),
							jobConfig.getLocalMode())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_USE_SERIAL),
							jobConfig.getUseSerial())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DEPENDENCIES),
							jobConfig.getDependencies())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_GROUPS), jobConfig.getGroups())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_RERUN), jobConfig.getRerun())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_UPSTREAM),
							jobConfig.getUpStream())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DOWNSTREAM),
							jobConfig.getDownStream())
					.replaceIfChanged(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS),
							jobConfig.getJobClass());

			// 提交事务
			curatorTransactionOp.commit();
		} catch (Exception e) {
			log.error("update job to zk failed", e);
			throw new SaturnJobConsoleException(e);
		}
	}

	private Collection<JobConfig> removeDuplicateByJobName(Set<JobConfig> streamChangedJobs) {
		Map<String, JobConfig> streamChangedJobsMap = new HashMap<>();
		for (JobConfig streamChangedJob : streamChangedJobs) {
			String jobName = streamChangedJob.getJobName();
			if (streamChangedJobsMap.containsKey(jobName)) {
				log.warn("the DB have duplicated jobName({})", jobName);
			} else {
				streamChangedJobsMap.put(jobName, streamChangedJob);
			}
		}
		return streamChangedJobsMap.values();
	}

	@Override
	public List<String> getAllJobNamesFromZK(String namespace) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String jobsNodePath = JobNodePath.get$JobsNodePath();
		List<String> jobs = curatorFrameworkOp.getChildren(jobsNodePath);
		if (jobs == null) {
			return Lists.newArrayList();
		}

		List<String> allJobs = new ArrayList<>();
		for (String job : jobs) {
			// 如果config节点存在才视为正常作业，其他异常作业在其他功能操作时也忽略
			if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(job))) {
				allJobs.add(job);
			}
		}
		Collections.sort(allJobs);
		return allJobs;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateJobCron(String namespace, String jobName, String cron, Map<String, String> customContext,
			String updatedBy) throws SaturnJobConsoleException {
		String cron0 = cron;
		if (cron0 != null && !cron0.trim().isEmpty()) {
			try {
				cron0 = cron0.trim();
				CronExpression.validateExpression(cron0);
			} catch (ParseException e) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "The cron expression is invalid: " + cron);
			}
		} else {
			cron0 = "";
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))) {
			String newCustomContextStr = null;

			String oldCustomContextStr = curatorFrameworkOp
					.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CUSTOM_CONTEXT));
			Map<String, String> oldCustomContextMap = toCustomContext(oldCustomContextStr);
			if (customContext != null && !customContext.isEmpty()) {
				oldCustomContextMap.putAll(customContext);
				newCustomContextStr = toCustomContext(oldCustomContextMap);
				if (newCustomContextStr.length() > 8000) {
					throw new SaturnJobConsoleException("The all customContext is out of db limit (Varchar[8000])");
				}
				if (newCustomContextStr.getBytes().length > 1024 * 1024) {
					throw new SaturnJobConsoleException("The all customContext is out of zk limit memory(1M)");
				}
			}

			String newCron = null;
			String oldCron = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON));
			if (cron0 != null && oldCron != null && !cron0.equals(oldCron.trim())) {
				newCron = cron0;
			}
			if (newCustomContextStr != null || newCron != null) {
				saveCronToDb(jobName, curatorFrameworkOp, newCustomContextStr, newCron, updatedBy);
			}
			if (newCustomContextStr != null) {
				curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CUSTOM_CONTEXT),
						newCustomContextStr);
			}
			if (newCron != null) {
				curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON), newCron);
			}
		} else {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, "The job does not exists: " + jobName);
		}
	}

	private void saveCronToDb(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			String newCustomContextStr, String newCron, String updatedBy) throws SaturnJobConsoleException {
		String namespace = curatorFrameworkOp.getCuratorFramework().getNamespace();
		JobConfig4DB jobConfig4DB = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
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
		currentJobConfigService.updateNewAndSaveOld2History(newJobConfig4DB, jobConfig4DB, updatedBy);
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
		List<String> executors = curatorFrameworkOp.getChildren(serverNodePath);
		List<JobServer> result = new ArrayList<>();
		if (executors != null && !executors.isEmpty()) {
			String leaderIp = curatorFrameworkOp.getData(JobNodePath.getLeaderNodePath(jobName, "election/host"));
			JobStatus jobStatus = getJobStatus(namespace, jobName);
			for (String each : executors) {
				JobServer jobServer = getJobServer(jobName, leaderIp, each, curatorFrameworkOp);
				jobServer.setJobStatus(jobStatus);
				result.add(jobServer);
			}
		}
		return result;
	}

	@Override
	public List<JobServerStatus> getJobServersStatus(String namespace, String jobName)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		List<String> executors = getJobServerList(namespace, jobName);
		List<JobServerStatus> result = new ArrayList<>();
		if (executors != null && !executors.isEmpty()) {
			for (String each : executors) {
				result.add(getJobServerStatus(jobName, each, curatorFrameworkOp));
			}
		}

		return result;
	}

	private JobServerStatus getJobServerStatus(String jobName, String executorName,
			CuratorFrameworkOp curatorFrameworkOp) {
		JobServerStatus result = new JobServerStatus();
		result.setExecutorName(executorName);
		result.setJobName(jobName);
		result.setServerStatus(getJobServerStatus0(jobName, executorName, curatorFrameworkOp));
		return result;
	}

	private ServerStatus getJobServerStatus0(String jobName, String executorName,
			CuratorFrameworkOp curatorFrameworkOp) {
		String status = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executorName, "status"));
		return ServerStatus.getServerStatus(status);
	}

	private JobServer getJobServer(String jobName, String leaderIp, String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		JobServer result = new JobServer();
		result.setExecutorName(executorName);
		result.setIp(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executorName, "ip")));
		result.setVersion(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executorName, "version")));
		String processSuccessCount = curatorFrameworkOp
				.getData(JobNodePath.getServerNodePath(jobName, executorName, "processSuccessCount"));
		result.setProcessSuccessCount(null == processSuccessCount ? 0 : Integer.parseInt(processSuccessCount));
		String processFailureCount = curatorFrameworkOp
				.getData(JobNodePath.getServerNodePath(jobName, executorName, "processFailureCount"));
		result.setProcessFailureCount(null == processFailureCount ? 0 : Integer.parseInt(processFailureCount));
		result.setSharding(
				curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executorName, "sharding")));
		result.setStatus(getJobServerStatus0(jobName, executorName, curatorFrameworkOp));
		result.setLeader(executorName.equals(leaderIp));
		result.setJobVersion(getJobVersion(jobName, executorName, curatorFrameworkOp));
		result.setContainer(curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorTaskNodePath(executorName)));

		return result;
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
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					String.format("该作业(%s)不处于READY状态，不能立即执行", jobName));
		}
		List<JobServerStatus> jobServersStatus = getJobServersStatus(namespace, jobName);
		if (jobServersStatus != null && !jobServersStatus.isEmpty()) {
			boolean hasOnlineExecutor = false;
			CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
			for (JobServerStatus jobServerStatus : jobServersStatus) {
				if (ServerStatus.ONLINE.equals(jobServerStatus.getServerStatus())) {
					hasOnlineExecutor = true;
					String executorName = jobServerStatus.getExecutorName();
					String path = JobNodePath.getRunOneTimePath(jobName, executorName);
					if (curatorFrameworkOp.checkExists(path)) {
						curatorFrameworkOp.delete(path);
					}
					curatorFrameworkOp.create(path, "null");
					log.info("runAtOnce namespace:{}, jobName:{}, executorName:{}", namespace, jobName, executorName);
				}
			}
			if (!hasOnlineExecutor) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "没有ONLINE的executor，不能立即执行");
			}
		} else {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					String.format("没有executor接管该作业(%s)，不能立即执行", jobName));
		}
	}

	@Override
	public void stopAtOnce(String namespace, String jobName) throws SaturnJobConsoleException {
		JobStatus jobStatus = getJobStatus(namespace, jobName);
		if (!JobStatus.STOPPING.equals(jobStatus)) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					String.format("该作业(%s)不处于STOPPING状态，不能立即终止", jobName));
		}
		List<String> jobServerList = getJobServerList(namespace, jobName);
		if (jobServerList != null && !jobServerList.isEmpty()) {
			CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
			for (String executorName : jobServerList) {
				String path = JobNodePath.getStopOneTimePath(jobName, executorName);
				if (curatorFrameworkOp.checkExists(path)) {
					curatorFrameworkOp.delete(path);
				}
				curatorFrameworkOp.create(path);
				log.info("stopAtOnce namespace:{}, jobName:{}, executorName:{}", namespace, jobName, executorName);
			}
		} else {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					String.format("没有executor接管该作业(%s)，不能立即终止", jobName));
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
		String executionNodePath = JobNodePath.getExecutionNodePath(jobName);
		List<String> shardItems = curatorFrameworkOp.getChildren(executionNodePath);
		if (shardItems == null || shardItems.isEmpty()) {
			return Lists.newArrayList();
		}

		List<ExecutionInfo> result = Lists.newArrayList();
		Map<String, String> itemExecutorMap = buildItem2ExecutorMap(jobName, curatorFrameworkOp);
		for (Map.Entry<String, String> itemExecutorEntry : itemExecutorMap.entrySet()) {
			result.add(buildExecutionInfo(jobName, itemExecutorEntry.getKey(), itemExecutorEntry.getValue(),
					curatorFrameworkOp, jobConfig));
		}

		// 可能有漏掉的running分片，比如新的机器接管了failover分片
		for (String shardItem : shardItems) {
			if (itemExecutorMap.containsKey(shardItem)) {
				// 已经在之前的步骤计算了
				continue;
			}
			String runningNodePath = JobNodePath.getExecutionNodePath(jobName, shardItem, "running");
			boolean running = curatorFrameworkOp.checkExists(runningNodePath);
			if (running) {
				result.add(buildExecutionInfo(jobName, shardItem, null, curatorFrameworkOp, jobConfig));
			}
		}

		Collections.sort(result);

		return result;
	}

	@Override
	public String getExecutionLog(String namespace, String jobName, String jobItem) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		String jobLogNodePath = JobNodePath.getExecutionNodePath(jobName, jobItem, "jobLog");
		Stat stat = curatorFrameworkOp.getStat(jobLogNodePath);
		if (stat.getDataLength() > getMaxZnodeDataLength()) {
			log.warn("job log of job={} item={} exceed max length, will not display the original log", jobName,
					jobItem);
			return ERR_MSG_TOO_LONG_TO_DISPLAY;
		}

		return curatorFrameworkOp.getData(jobLogNodePath);
	}

	@Override
	public List<JobConfig4DB> getJobsByQueue(String queue) {
		return currentJobConfigService.findConfigByQueue(queue);
	}

	private void updateReportNodeAndWait(String jobName, CuratorFrameworkOp curatorFrameworkOp, long sleepInMill) {
		curatorFrameworkOp.update(JobNodePath.getReportPath(jobName), System.currentTimeMillis());
		try {
			Thread.sleep(sleepInMill);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private ExecutionInfo buildExecutionInfo(String jobName, String shardItem, String executorName,
			CuratorFrameworkOp curatorFrameworkOp, JobConfig jobConfig) {
		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setJobName(jobName);
		executionInfo.setItem(Integer.parseInt(shardItem));

		setExecutorNameAndStatus(jobName, shardItem, executorName, curatorFrameworkOp, executionInfo, jobConfig);

		// jobMsg
		String jobMsg = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, shardItem, "jobMsg"));
		executionInfo.setJobMsg(jobMsg);

		// timeZone
		String timeZoneStr = jobConfig.getTimeZone();
		if (StringUtils.isBlank(timeZoneStr)) {
			timeZoneStr = SaturnConstants.TIME_ZONE_ID_DEFAULT;
		}
		executionInfo.setTimeZone(timeZoneStr);
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
		// last begin time
		String lastBeginTime = curatorFrameworkOp
				.getData(JobNodePath.getExecutionNodePath(jobName, shardItem, "lastBeginTime"));
		executionInfo.setLastBeginTime(SaturnConsoleUtils.parseMillisecond2DisplayTime(lastBeginTime, timeZone));
		// next fire time, ignore if jobType is Msg
		JobType jobType = JobType.getJobType(jobConfig.getJobType());
		if (JobType.isCron(jobType)) {
			String nextFireTime = curatorFrameworkOp
					.getData(JobNodePath.getExecutionNodePath(jobName, shardItem, "nextFireTime"));
			executionInfo.setNextFireTime(SaturnConsoleUtils.parseMillisecond2DisplayTime(nextFireTime, timeZone));
		} else {
			executionInfo.setNextFireTime(null);
		}
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

					executionInfo.setLastTimeConsumedInSec((lastCompleteTimeLong - lastBeginTimeLong) / 1000d);
				}
			}
		}

		return executionInfo;
	}

	private void setExecutorNameAndStatus(String jobName, String shardItem, String executorName,
			CuratorFrameworkOp curatorFrameworkOp, ExecutionInfo executionInfo, JobConfig jobConfig) {
		boolean isEnabledReport = jobConfig.getEnabledReport();
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
			// 不能立即返回还是要看看是否failed或者timeout
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
			// 不能立即返回还是要看看是否正在failover
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

		if (servers == null || servers.isEmpty()) {
			return Maps.newHashMap();
		}

		Map<String, String> resultMap = new HashMap<>();
		for (String server : servers) {
			resolveShardingData(jobName, curatorFrameworkOp, resultMap, server);
		}
		return resultMap;
	}

	private void resolveShardingData(String jobName, CuratorFrameworkOp curatorFrameworkOp,
			Map<String, String> resultMap, String server) {
		String shardingData = curatorFrameworkOp.getData(JobNodePath.getServerSharding(jobName, server));
		if (StringUtils.isBlank(shardingData)) {
			return;
		}

		String[] shardingValues = shardingData.split(",");
		for (String value : shardingValues) {
			if (StringUtils.isBlank(value)) {
				continue;
			}

			resultMap.put(value.trim(), server);
		}
	}

	private void validateShardingItemFormat(JobConfig jobConfig) throws SaturnJobConsoleException {
		String parameters = jobConfig.getShardingItemParameters();
		String[] kvs = parameters.trim().split(",");
		for (int i = 0; i < kvs.length; i++) {
			String keyAndValue = kvs[i];
			if (!keyAndValue.contains("=")) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, String.format("分片参数'%s'格式有误", keyAndValue));
			}
			String key = keyAndValue.trim().split("=")[0].trim();
			boolean isNumeric = StringUtils.isNumeric(key);
			if (!isNumeric) {
				throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
						String.format("分片参数'%s'格式有误", jobConfig.getShardingItemParameters()));
			}
		}
	}
}
