package com.vip.saturn.job.console.service.impl.marathon;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobMode;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.console.domain.container.*;
import com.vip.saturn.job.console.domain.container.vo.ContainerExecutorVo;
import com.vip.saturn.job.console.domain.container.vo.ContainerScaleJobVo;
import com.vip.saturn.job.console.domain.container.vo.ContainerVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.MarathonService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.ContainerNodePath;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author hebelala
 */
@Service
public class MarathonServiceImpl implements MarathonService {

	private static final Logger log = LoggerFactory.getLogger(MarathonServiceImpl.class);

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private JobService jobService;

	@Resource
	private SystemConfigService systemConfigService;

	@Override
	public ContainerToken getContainerToken(String namespace) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String dcosConfigTokenNodePath = ContainerNodePath.getDcosConfigTokenNodePath();
		String data = curatorFrameworkOp.getData(dcosConfigTokenNodePath);
		return JSON.parseObject(data, ContainerToken.class);
	}

	@Override
	public void saveContainerToken(String namespace, ContainerToken containerToken) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String dcosConfigTokenNodePath = ContainerNodePath.getDcosConfigTokenNodePath();
		try {
			// Need to update scalaJob's shardingItemParameters:
			// 1. update scale job's shardingItemParameters.
			// 2. disable job, sleep 1s.
			// 3. update shardingItemParameters, sleep 1s.
			// 4. enable job.
			Map<String, List<ContainerScaleJob>> allContainerScaleJobs = new HashMap<>();
			List<String> tasks = getTasks(curatorFrameworkOp);
			for (String task : tasks) {
				List<ContainerScaleJob> containerScaleJobs = getContainerScaleJobs(task, curatorFrameworkOp);
				allContainerScaleJobs.put(task, containerScaleJobs);
			}
			Iterator<Map.Entry<String, List<ContainerScaleJob>>> iterator = allContainerScaleJobs.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, List<ContainerScaleJob>> next = iterator.next();
				List<ContainerScaleJob> containerScaleJobs = next.getValue();
				for (ContainerScaleJob containerScaleJob : containerScaleJobs) {
					if (containerScaleJob.getEnabled()) {
						curatorFrameworkOp.update(
								JobNodePath.getConfigNodePath(
										containerScaleJob.getContainerScaleJobConfig().getJobName(), "enabled"),
								"false");
					}
				}
			}
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) { // NOSONAR
			}
			Iterator<Map.Entry<String, List<ContainerScaleJob>>> iterator2 = allContainerScaleJobs.entrySet()
					.iterator();
			while (iterator2.hasNext()) {
				Map.Entry<String, List<ContainerScaleJob>> next = iterator2.next();
				String taskId = next.getKey();
				List<ContainerScaleJob> containerScaleJobs = next.getValue();
				for (ContainerScaleJob containerScaleJob : containerScaleJobs) {
					String jobName = containerScaleJob.getContainerScaleJobConfig().getJobName();
					Integer instances = containerScaleJob.getContainerScaleJobConfig().getInstances();
					String shardingItemParametersNodePath = JobNodePath.getConfigNodePath(jobName,
							"shardingItemParameters");
					String scaleShardingItemParameters = getContainerScaleJobShardingItemParameters(containerToken,
							taskId, instances);
					curatorFrameworkOp.update(shardingItemParametersNodePath, scaleShardingItemParameters);
				}
			}
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) { // NOSONAR
			}
			Iterator<Map.Entry<String, List<ContainerScaleJob>>> iterator3 = allContainerScaleJobs.entrySet()
					.iterator();
			while (iterator3.hasNext()) {
				Map.Entry<String, List<ContainerScaleJob>> next = iterator3.next();
				List<ContainerScaleJob> containerScaleJobs = next.getValue();
				for (ContainerScaleJob containerScaleJob : containerScaleJobs) {
					if (containerScaleJob.getEnabled()) {
						curatorFrameworkOp.update(
								JobNodePath.getConfigNodePath(
										containerScaleJob.getContainerScaleJobConfig().getJobName(), "enabled"),
								"true");
					}
				}
			}
			curatorFrameworkOp.update(dcosConfigTokenNodePath, JSON.toJSONString(containerToken));
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
	}

	private List<String> getTasks(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		List<String> tasks = new ArrayList<>();
		List<String> children = curatorFrameworkOp.getChildren(ContainerNodePath.getDcosTasksNodePath());
		if (children != null) {
			tasks.addAll(children);
		}
		return tasks;
	}

	private List<ContainerScaleJob> getContainerScaleJobs(String task,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		List<ContainerScaleJob> containerScaleJobs = new ArrayList<>();
		String dcosTaskScaleJobsNodePath = ContainerNodePath.getDcosTaskScaleJobsNodePath(task);
		if (curatorFrameworkOp.checkExists(dcosTaskScaleJobsNodePath)) {
			List<String> jobs = curatorFrameworkOp.getChildren(dcosTaskScaleJobsNodePath);
			if (jobs != null) {
				for (String job : jobs) {
					ContainerScaleJob containerScaleJob = getContainerScaleJob(curatorFrameworkOp, task, job);
					if (containerScaleJob != null) {
						containerScaleJobs.add(containerScaleJob);
					}
				}
			}
		}
		Collections.sort(containerScaleJobs, new Comparator<ContainerScaleJob>() {
			@Override
			public int compare(ContainerScaleJob o1, ContainerScaleJob o2) {
				// getContainerScaleJobConfig cannot be null
				return o1.getContainerScaleJobConfig().getJobName()
						.compareTo(o2.getContainerScaleJobConfig().getJobName());
			}
		});
		return containerScaleJobs;
	}

	private ContainerScaleJob getContainerScaleJob(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String id,
			String jobName) throws SaturnJobConsoleException {
		ContainerScaleJob containerScaleJob = null;
		String data = curatorFrameworkOp.getData(ContainerNodePath.getDcosTaskScaleJobNodePath(id, jobName));
		if (data != null) {
			containerScaleJob = new ContainerScaleJob();
			ContainerScaleJobConfig containerScaleJobConfig = JSON.parseObject(data, ContainerScaleJobConfig.class);
			containerScaleJob.setContainerScaleJobConfig(containerScaleJobConfig);
			String enabledStr = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled"));
			containerScaleJob.setEnabled(Boolean.valueOf(enabledStr));
		}
		return containerScaleJob;
	}

	private String getContainerScaleJobShardingItemParameters(ContainerToken containerToken, String appId,
			Integer instances) throws SaturnJobConsoleException {
		try {
			String auth = Base64.encodeBase64String(
					(containerToken.getUserName() + ":" + containerToken.getPassword()).getBytes("UTF-8"));
			String url = "";
			if (SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI != null
					&& SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI.endsWith("/")) {
				url = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + "v2/apps/" + appId + "?force=true";
			} else {
				url = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + "/v2/apps/" + appId + "?force=true";
			}
			return "0=curl -X PUT -H \"Content-Type:application/json\" -H \"Authorization:Basic " + auth
					+ "\" --data '{\"instances\":" + instances + "}' " + url;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
	}

	@Override
	public List<ContainerVo> getContainerVos(String namespace) throws SaturnJobConsoleException {
		List<ContainerVo> containerVos = new ArrayList<>();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		List<String> allUnSystemJobs = jobService.getUnSystemJobNames(namespace);
		Map<String, List<String>> taskBindJobNames = getTaskBindJobNames(curatorFrameworkOp, allUnSystemJobs);
		ContainerToken containerToken = getContainerToken(namespace);
		List<String> tasks = getTasks(curatorFrameworkOp);
		Map<String, List<ContainerExecutorVo>> containerExecutors = getContainerExecutors(curatorFrameworkOp,
				allUnSystemJobs);
		if (!tasks.isEmpty()) {
			for (String task : tasks) {
				ContainerVo containerVo = new ContainerVo();
				containerVo.setTaskId(task);
				containerVo.setContainerExecutorVos(containerExecutors.get(task));
				containerVo.setBindingJobNames(changeTypeOfBindingJobNames(taskBindJobNames.get(task)));
				containerVo.setContainerStatus(changeTypeOfContainerStatus(containerToken, task));
				ContainerConfig containerConfig = JSON
						.parseObject(curatorFrameworkOp.getData(ContainerNodePath.getDcosTaskConfigNodePath(task)),
								ContainerConfig.class);
				containerVo.setContainerConfig(changeTypeOfContainerConfig(containerConfig));
				containerVo.setCreateTime(changeTypeOfCreateTime(containerConfig));
				containerVo.setContainerScaleJobVos(getContainerScaleJobVos(namespace, task, curatorFrameworkOp));
				containerVo.setInstancesConfigured(String.valueOf(containerConfig.getInstances()));
				containerVos.add(containerVo);
			}
		}
		return containerVos;
	}

	private Map<String, List<String>> getTaskBindJobNames(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			List<String> allUnSystemJobs) throws SaturnJobConsoleException {
		Map<String, List<String>> map = new HashMap<>();
		for (String job : allUnSystemJobs) {
			String preferListNodePath = JobNodePath.getConfigNodePath(job, "preferList");
			if (curatorFrameworkOp.checkExists(preferListNodePath)) {
				String data = curatorFrameworkOp.getData(preferListNodePath);
				if (null == data) {
					continue;
				}
				String[] split = data.split(",");
				for (int i = 0; i < split.length; i++) {
					String tmp = split[i].trim();
					if (!tmp.startsWith("@")) {
						continue;
					}
					String taskId = tmp.substring(1);
					if (!map.containsKey(taskId)) {
						map.put(taskId, new ArrayList<String>());
					}
					List<String> taskJobs = map.get(taskId);
					if (!taskJobs.contains(job)) {
						taskJobs.add(job);
					}
				}
			}
		}
		return map;
	}

	private void handleContainerExecutorsFromZK(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			Map<String, List<ContainerExecutorVo>> containerExecutors) {
		if (curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath())) {
			List<String> executors = curatorFrameworkOp.getChildren(ExecutorNodePath.getExecutorNodePath());
			if (executors != null) {
				for (String executor : executors) {
					String executorTaskNodePath = ExecutorNodePath.getExecutorTaskNodePath(executor);
					if (!curatorFrameworkOp.checkExists(executorTaskNodePath)) {
						continue;
					}
					String task = curatorFrameworkOp.getData(executorTaskNodePath);
					if (task == null) {
						continue;
					}
					if (!containerExecutors.containsKey(task)) {
						containerExecutors.put(task, new ArrayList<ContainerExecutorVo>());
					}
					ContainerExecutorVo containerExecutorVo = new ContainerExecutorVo();
					containerExecutorVo.setExecutorName(executor);
					String executorIpNodePath = ExecutorNodePath.getExecutorIpNodePath(executor);
					if (curatorFrameworkOp.checkExists(executorIpNodePath)) {
						String executorIp = curatorFrameworkOp.getData(executorIpNodePath);
						containerExecutorVo.setIp(executorIp);
					}
					containerExecutors.get(task).add(containerExecutorVo);
				}
			}
		}
	}

	private Map<String, List<ContainerExecutorVo>> getContainerExecutors(
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, List<String> allUnSystemJobs) {
		Map<String, List<ContainerExecutorVo>> containerExecutors = new HashMap<>();
		handleContainerExecutorsFromZK(curatorFrameworkOp, containerExecutors);
		for (String job : allUnSystemJobs) {
			Collection<List<ContainerExecutorVo>> values = containerExecutors.values();
			if (values == null) {
				continue;
			}
			for (List<ContainerExecutorVo> value : values) {
				if (value == null) {
					continue;
				}
				for (ContainerExecutorVo containerExecutorVo : value) {
					String jobServerShardingNodePath = JobNodePath.getServerNodePath(job,
							containerExecutorVo.getExecutorName(), "sharding");
					if (!curatorFrameworkOp.checkExists(jobServerShardingNodePath)) {
						continue;
					}
					String sharding = curatorFrameworkOp.getData(jobServerShardingNodePath);
					if (sharding == null || sharding.trim().length() == 0) {
						continue;
					}
					Boolean isRunning = isRunningOrEnabled(curatorFrameworkOp, job, sharding);
					if (!isRunning) {
						continue;
					}
					String runningJobNames = containerExecutorVo.getRunningJobNames();
					if (runningJobNames == null) {
						containerExecutorVo.setRunningJobNames(job);
					} else {
						containerExecutorVo.setRunningJobNames(runningJobNames + ",<br/>" + job);
					}
				}
			}
		}
		return containerExecutors;
	}

	private boolean isRunningOrEnabled(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String job,
			String sharding) {
		boolean isRunning = false;
		String[] split = sharding.split(",");
		for (String tmp : split) {
			String runningNodePath = JobNodePath.getExecutionNodePath(job, tmp.trim(), "running");
			if (curatorFrameworkOp.checkExists(runningNodePath)) {
				isRunning = true;
				break;
			}
		}
		if (!isRunning) {
			String enabledNodePath = JobNodePath.getConfigNodePath(job, "enabled");
			if (Boolean.parseBoolean(curatorFrameworkOp.getData(enabledNodePath))) {
				isRunning = true;
			}
		}
		return isRunning;
	}

	private String changeTypeOfBindingJobNames(List<String> bindingJobNames) {
		String result = "";
		if (bindingJobNames != null) {
			int size = bindingJobNames.size();
			for (int i = 0; i < size; i++) {
				result = result + bindingJobNames.get(i);
				if (i < size - 1) {
					result = result + ",<br/>";
				}
			}
		}
		return result;
	}

	private String changeTypeOfContainerStatus(ContainerToken containerToken, String task) {
		ContainerStatus containerStatus = null;
		String errorMessage = "";
		try {
			containerStatus = MarathonRestClient.getContainerStatus(containerToken.getUserName(),
					containerToken.getPassword(), task);
		} catch (SaturnJobConsoleException e) {
			log.info("get status error, cause of: {}", e);
			errorMessage = "<font color='red'>get task status error, message is: "
					+ StringEscapeUtils.escapeHtml4(e.getMessage()) + "</font>";

		}
		String total_count = containerStatus == null || containerStatus.getTotalCount() == null ? "-"
				: String.valueOf(containerStatus.getTotalCount());
		String healthy_count = containerStatus == null || containerStatus.getHealthyCount() == null ? "-"
				: String.valueOf(containerStatus.getHealthyCount());
		String unhealthy_count = containerStatus == null || containerStatus.getUnhealthyCount() == null ? "-"
				: String.valueOf(containerStatus.getUnhealthyCount());
		String staged_count = containerStatus == null || containerStatus.getStagedCount() == null ? "-"
				: String.valueOf(containerStatus.getStagedCount());
		String running_count = containerStatus == null || containerStatus.getRunningCount() == null ? "-"
				: String.valueOf(containerStatus.getRunningCount());
		return String.format(
				"当前实例总数：%s<br/><br/>" + "健康实例数：%s<br/>" + "非健康实例数：%s<br/><br/>" + "staged实例数：%s<br/>"
						+ "running实例数：%s<br/>" + "%s",
				total_count, healthy_count, unhealthy_count, staged_count, running_count, errorMessage);
	}

	private String changeTypeOfContainerConfig(ContainerConfig containerConfig) {
		String constraints = containerConfig.getConstraints() == null ? "-"
				: JSON.toJSONString(containerConfig.getConstraints());
		String env = containerConfig.getEnv() == null ? "-" : JSON.toJSONString(containerConfig.getEnv());
		String privileged = containerConfig.getPrivileged() == null ? "-"
				: String.valueOf(containerConfig.getPrivileged());
		String force_pull_image = containerConfig.getForcePullImage() == null ? "-"
				: String.valueOf(containerConfig.getForcePullImage());
		String volumes = containerConfig.getVolumes() == null ? "-" : JSON.toJSONString(containerConfig.getVolumes());
		String cmd = containerConfig.getCmd() == null ? "-" : containerConfig.getCmd();
		return String.format(
				"配置的实例数：%s<br/>" + "CPU：%s核<br/>" + "内存：%sM<br/>" + "镜像：%s<br/>" + "约束标识：%s<br/>" + "环境变量：%s<br/>"
						+ "是否使用特权模式：%s<br/>" + "是否强制拉镜像：%s<br/>" + "容器与宿主机的目录映射：%s<br/>" + "CMD：%s",
				containerConfig.getInstances(), containerConfig.getCpus(), containerConfig.getMem(),
				containerConfig.getImage(), constraints, env, privileged, force_pull_image, volumes, cmd);
	}

	private String changeTypeOfCreateTime(ContainerConfig containerConfig) {
		Long createTime = containerConfig.getCreateTime();
		if (createTime != null) {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(createTime);
		} else {
			return null;
		}
	}

	private List<ContainerScaleJobVo> getContainerScaleJobVos(String namespace, String taskId,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		List<ContainerScaleJobVo> containerScaleJobVos = new ArrayList<>();
		String dcosTaskScaleJobsNodePath = ContainerNodePath.getDcosTaskScaleJobsNodePath(taskId);
		if (curatorFrameworkOp.checkExists(dcosTaskScaleJobsNodePath)) {
			List<String> jobs = curatorFrameworkOp.getChildren(dcosTaskScaleJobsNodePath);
			if (jobs != null) {
				for (String job : jobs) {
					ContainerScaleJobVo containerScaleJobVo = getContainerScaleJobVo(namespace, taskId, job);
					if (containerScaleJobVo != null) {
						containerScaleJobVos.add(containerScaleJobVo);
					}
				}
			}
		}
		Collections.sort(containerScaleJobVos, new Comparator<ContainerScaleJobVo>() {
			@Override
			public int compare(ContainerScaleJobVo o1, ContainerScaleJobVo o2) {
				return o1.getJobName().compareTo(o2.getJobName());
			}
		});
		return containerScaleJobVos;
	}

	@Override
	public void checkContainerTokenNotNull(String namespace, ContainerToken containerToken)
			throws SaturnJobConsoleException {
		if (containerToken == null) {
			throw new SaturnJobConsoleException("Please input userName and password");
		}
		if (containerToken.getUserName() == null) {
			throw new SaturnJobConsoleException("Please input userName");
		}
		if (containerToken.getPassword() == null) {
			throw new SaturnJobConsoleException("Please input password");
		}
	}

	@Override
	public void saveOrUpdateContainerTokenIfNecessary(String namespace, ContainerToken containerToken)
			throws SaturnJobConsoleException {
		ContainerToken containerTokenOld = getContainerToken(namespace);
		boolean same = containerToken.equals(containerTokenOld);
		if (!same) {
			saveContainerToken(namespace, containerToken);
		}
	}

	@Override
	public void addContainer(String namespace, ContainerConfig containerConfig) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String taskId = containerConfig.getTaskId();
		if (curatorFrameworkOp.checkExists(ContainerNodePath.getDcosTaskIdNodePath(taskId))) {
			throw new SaturnJobConsoleException("The taskId already exists");
		}
		if (containerConfig.getEnv() == null) {
			containerConfig.setEnv(new HashMap<String, String>());
		}
		containerConfig.getEnv().put(SaturnEnvProperties.NAME_VIP_SATURN_DCOS_TASK, taskId);
		if (!containerConfig.getEnv().containsKey(SaturnEnvProperties.NAME_VIP_SATURN_EXECUTOR_CLEAN)) {
			containerConfig.getEnv().put(SaturnEnvProperties.NAME_VIP_SATURN_EXECUTOR_CLEAN, "true");
		}
		ContainerToken containerToken = getContainerToken(namespace);
		MarathonRestClient.deploy(containerToken.getUserName(), containerToken.getPassword(), containerConfig);
		containerConfig.setCreateTime(System.currentTimeMillis());
		replaceEnvSensitiveParams(containerConfig);
		curatorFrameworkOp.update(ContainerNodePath.getDcosTaskConfigNodePath(taskId),
				JSON.toJSONString(containerConfig));
	}

	private void replaceEnvSensitiveParams(ContainerConfig containerConfig) {
		String sensitiveParams = systemConfigService.getValue(SystemConfigProperties.CONTAINER_SENSITIVE_PARAMS);
		List<String> sensitiveWords = extractSensitiveWords(sensitiveParams);
		if (containerConfig.getEnv() != null) {
			Map<String, String> env = containerConfig.getEnv();
			for (String sensitiveWord : sensitiveWords) {
				if (env.containsKey(sensitiveWord)) {
					env.put(sensitiveWord, "******");
				}
			}
		}
	}

	private List<String> extractSensitiveWords(String sensitiveParams) {
		List<String> words = new ArrayList<>();
		if (sensitiveParams != null && sensitiveParams.trim().length() > 0) {
			String[] split = sensitiveParams.split(",");
			if (split != null) {
				for (String s : split) {
					String tmp = s.trim();
					if (tmp.length() > 0) {
						words.add(tmp);
					}
				}
			}
		}
		return words;
	}

	@Override
	public void updateContainerInstances(String namespace, String taskId, int instances)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String dcosTaskIdNodePath = ContainerNodePath.getDcosTaskIdNodePath(taskId);
		String dcosTaskConfigNodePath = ContainerNodePath.getDcosTaskConfigNodePath(taskId);
		if (!curatorFrameworkOp.checkExists(dcosTaskIdNodePath)) {
			throw new SaturnJobConsoleException("The taskId already exists");
		}
		String taskConfigJson = curatorFrameworkOp.getData(dcosTaskConfigNodePath);
		ContainerConfig containerConfig = JSON.parseObject(taskConfigJson, ContainerConfig.class);
		ContainerToken containerToken = getContainerToken(namespace);
		MarathonRestClient.scale(containerToken.getUserName(), containerToken.getPassword(), taskId, instances);
		containerConfig.setInstances(instances);
		curatorFrameworkOp.update(dcosTaskConfigNodePath, JSON.toJSONString(containerConfig));
	}

	@Override
	public void removeContainer(String namespace, String taskId) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String dcosTaskIdNodePath = ContainerNodePath.getDcosTaskIdNodePath(taskId);
		if (!curatorFrameworkOp.checkExists(dcosTaskIdNodePath)) {
			throw new SaturnJobConsoleException("The taskId already exists");
		}
		List<ContainerScaleJobVo> containerScaleJobVos = getContainerScaleJobVos(namespace, taskId, curatorFrameworkOp);
		List<String> allUnSystemJobs = jobService.getUnSystemJobNames(namespace);
		for (String job : allUnSystemJobs) {
			String preferListNodePath = JobNodePath.getConfigNodePath(job, "preferList");
			if (curatorFrameworkOp.checkExists(preferListNodePath)) {
				String preferList = curatorFrameworkOp.getData(preferListNodePath);
				if (null == preferList) {
					continue;
				}
				String[] split = preferList.trim().split(",");
				for (String tmp : split) {
					if (tmp.trim().equals("@" + taskId)) {
						throw new SaturnJobConsoleException("Cannot destroy the container, because it's binding a job");
					}
				}
			}
		}
		for (ContainerScaleJobVo containerScaleJobVo : containerScaleJobVos) {
			deleteContainerScaleJob(namespace, taskId, containerScaleJobVo.getJobName());
		}
		ContainerToken containerToken = getContainerToken(namespace);
		MarathonRestClient.destroy(containerToken.getUserName(), containerToken.getPassword(), taskId);
		curatorFrameworkOp.deleteRecursive(dcosTaskIdNodePath);
	}

	@Override
	public String getContainerDetail(String namespace, String taskId) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String dcosTaskIdNodePath = ContainerNodePath.getDcosTaskIdNodePath(taskId);
		if (!curatorFrameworkOp.checkExists(dcosTaskIdNodePath)) {
			throw new SaturnJobConsoleException("The taskId already exists");
		}
		ContainerToken containerToken = getContainerToken(namespace);
		return MarathonRestClient.info(containerToken.getUserName(), containerToken.getPassword(), taskId);
	}

	@Override
	public String getRegistryCatalog(String namespace) throws SaturnJobConsoleException {
		return MarathonRestClient.getRegistryCatalog();
	}

	@Override
	public String getRegistryRepositoryTags(String namespace, String repository) throws SaturnJobConsoleException {
		return MarathonRestClient.getRegistryRepositoriesTagsList(repository);
	}

	@Override
	public void addContainerScaleJob(String namespace, String taskId, String jobDesc, int instances, String timeZone,
			String cron) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String jobName = SaturnConstants.SYSTEM_SCALE_JOB_PREFEX + System.currentTimeMillis();
		ContainerToken containerToken = getContainerToken(namespace);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setDescription(jobDesc);
		jobConfig.setTimeZone(timeZone);
		jobConfig.setCron(cron);
		jobConfig.setJobMode(JobMode.system_scale);
		jobConfig.setJobType(JobType.SHELL_JOB.name());
		jobConfig.setPreferList("@" + taskId);
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters(
				getContainerScaleJobShardingItemParameters(containerToken, taskId, instances));
		jobConfig.setUseDispreferList(false);
		jobConfig.setTimeout4AlarmSeconds(30);
		jobConfig.setTimeoutSeconds(30);
		jobConfig.setJobParameter("");
		jobConfig.setQueueName("");
		jobConfig.setChannelName("");
		jobConfig.setPausePeriodDate("");
		jobConfig.setPausePeriodTime("");

		jobService.addJob(namespace, jobConfig, "");

		ContainerScaleJobConfig containerScaleJobConfig = new ContainerScaleJobConfig();
		containerScaleJobConfig.setJobName(jobName);
		containerScaleJobConfig.setJobDesc(jobDesc);
		containerScaleJobConfig.setInstances(instances);
		containerScaleJobConfig.setTimeZone(timeZone);
		containerScaleJobConfig.setCron(cron);
		try {
			String containerScaleJobStr = JSON.toJSONString(containerScaleJobConfig);
			String dcosTaskScaleJobNodePath = ContainerNodePath.getDcosTaskScaleJobNodePath(taskId, jobName);
			curatorFrameworkOp.fillJobNodeIfNotExist(dcosTaskScaleJobNodePath, containerScaleJobStr);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e.getMessage(), e);
		}
	}

	@Override
	public ContainerScaleJobVo getContainerScaleJobVo(String namespace, String taskId, String jobName)
			throws SaturnJobConsoleException {
		ContainerScaleJobVo containerScaleJobVo = null;
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		try {
			ContainerScaleJob containerScaleJob = getContainerScaleJob(curatorFrameworkOp, taskId, jobName);
			ContainerScaleJobConfig containerScaleJobConfig = containerScaleJob.getContainerScaleJobConfig();
			containerScaleJobVo = new ContainerScaleJobVo();
			containerScaleJobVo.setJobName(containerScaleJobConfig.getJobName());
			containerScaleJobVo.setJobDesc(containerScaleJobConfig.getJobDesc());
			if (containerScaleJobConfig.getInstances() != null) {
				containerScaleJobVo.setInstances(containerScaleJobConfig.getInstances().toString());
			}
			if (containerScaleJobConfig.getTimeZone() == null) {
				containerScaleJobVo.setTimeZone(SaturnConstants.TIME_ZONE_ID_DEFAULT);
			} else {
				containerScaleJobVo.setTimeZone(containerScaleJobConfig.getTimeZone());
			}
			containerScaleJobVo.setCron(containerScaleJobConfig.getCron());
			containerScaleJobVo.setEnabled(containerScaleJob.getEnabled().toString());
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
		return containerScaleJobVo;
	}

	@Override
	public void enableContainerScaleJob(String namespace, String jobName, boolean flag)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		String enabledNodePath = JobNodePath.getConfigNodePath(jobName, "enabled");
		String enabledStr = curatorFrameworkOp.getData(enabledNodePath);
		Boolean enabled = Boolean.valueOf(enabledStr);
		if (enabled == flag) {
			throw new SaturnJobConsoleException("The job is already " + (flag ? "enabled" : "disabled"));
		}
		curatorFrameworkOp.update(enabledNodePath, flag);
	}

	@Override
	public void deleteContainerScaleJob(String namespace, String taskId, String jobName)
			throws SaturnJobConsoleException {
		// disable job, and delete it
		// wait 5s to disable job at most
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService
				.getCuratorFrameworkOp(namespace);
		try {
			String jobNodePath = JobNodePath.getJobNodePath(jobName);
			if (curatorFrameworkOp.checkExists(jobNodePath)) {
				String enabledNodePath = JobNodePath.getConfigNodePath(jobName, "enabled");
				String enabledStr = curatorFrameworkOp.getData(enabledNodePath);
				Boolean enabled = Boolean.valueOf(enabledStr);
				if (enabled) {
					curatorFrameworkOp.update(enabledNodePath, false);
				}
				long waitStopTime = 5000L;
				while (waitStopTime > 0L) {
					Thread.sleep(100);
					waitStopTime -= 100;
					JobStatus jobStatus = jobService.getJobStatus(namespace, jobName);
					if (JobStatus.STOPPED.equals(jobStatus)) {
						jobService.removeJob(namespace, jobName);
						deleteScaleJobNodePath(curatorFrameworkOp, taskId, jobName);
						return;
					}
				}
				throw new SaturnJobConsoleException("The job is not stopped, cannot be deleted, please retry later");
			} else {
				deleteScaleJobNodePath(curatorFrameworkOp, taskId, jobName);
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e.getMessage(), e);
		}
	}

	private void deleteScaleJobNodePath(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String taskId,
			String jobName) {
		String dcosTaskScaleJobNodePath = ContainerNodePath.getDcosTaskScaleJobNodePath(taskId, jobName);
		curatorFrameworkOp.deleteRecursive(dcosTaskScaleJobNodePath);
	}

	@Override
	public int getContainerRunningInstances(String namespace, String taskId) throws SaturnJobConsoleException {
		ContainerToken containerToken = getContainerToken(namespace);
		return MarathonRestClient.count(containerToken.getUserName(), containerToken.getPassword(), taskId);
	}
}
