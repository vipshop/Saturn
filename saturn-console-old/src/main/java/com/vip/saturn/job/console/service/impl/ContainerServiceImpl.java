package com.vip.saturn.job.console.service.impl;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.domain.container.*;
import com.vip.saturn.job.console.domain.container.vo.ContainerExecutorVo;
import com.vip.saturn.job.console.domain.container.vo.ContainerScaleJobVo;
import com.vip.saturn.job.console.domain.container.vo.ContainerVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.*;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.ContainerNodePath;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author hebelala
 */
@Service
public class ContainerServiceImpl implements ContainerService {

	private static final Logger log = LoggerFactory.getLogger(ContainerServiceImpl.class);

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private ExecutorService executorService;

	@Resource
	private SystemConfigService systemConfigService;

	@Autowired
	@Qualifier("marathonRestAdapter")
	private ContainerRestService marathonRestAdapter;

	private ContainerRestService getContainerRestService() {
		String containerType = SaturnEnvProperties.CONTAINER_TYPE;
		if ("MARATHON".equals(containerType)) {
			return marathonRestAdapter;
		} else {
			return marathonRestAdapter;
		}
	}

	@Override
	public void checkContainerTokenNotNull(ContainerToken containerToken) throws SaturnJobConsoleException {
		getContainerRestService().checkContainerTokenNotNull(containerToken);
	}

	@Override
	public void saveOrUpdateContainerToken(ContainerToken containerToken) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String dcosConfigTokenNodePath = ContainerNodePath.getDcosConfigTokenNodePath();
		try {
			// Update scale job's shardingItemParameters. Disable job, sleep 1s, update shardingItemParameters, sleep
			// 1s, enable job
			Map<String, List<ContainerScaleJob>> allContainerScaleJobs = new HashMap<>();
			List<String> tasks = getTasks(curatorFrameworkOp);
			for (String task : tasks) {
				List<ContainerScaleJob> containerScaleJobs = getContainerScaleJobs(task);
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
			curatorFrameworkOp.update(dcosConfigTokenNodePath,
					getContainerRestService().serializeContainerToken(containerToken));
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
	}

	@Override
	public void saveOrUpdateContainerTokenIfNecessary(ContainerToken containerToken) throws SaturnJobConsoleException {
		ContainerToken containerTokenOld = getContainerToken();
		ContainerRestService containerRestService = getContainerRestService();
		boolean same = containerRestService.containerTokenEquals(containerToken, containerTokenOld);
		if (!same) {
			saveOrUpdateContainerToken(containerToken);
		}
	}

	@Override
	public ContainerToken getContainerToken() throws SaturnJobConsoleException {
		return getContainerToken(curatorRepository.inSessionClient());
	}

	@Override
	public ContainerToken getContainerToken(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		String dcosConfigTokenNodePath = ContainerNodePath.getDcosConfigTokenNodePath();
		String data = curatorFrameworkOp.getData(dcosConfigTokenNodePath);
		ContainerRestService containerRestService = getContainerRestService();
		return containerRestService.deserializeContainerToken(data);
	}

	@Override
	public List<ContainerVo> getContainerVos() throws SaturnJobConsoleException {
		List<ContainerVo> containerVos = new ArrayList<>();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		List<String> allUnSystemJobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
		Map<String, List<String>> taskBindJobNames = getTaskBindJobNames(curatorFrameworkOp, allUnSystemJobs);
		ContainerToken containerToken = getContainerToken();
		List<String> tasks = getTasks(curatorFrameworkOp);
		Map<String, List<ContainerExecutorVo>> containerExecutors = getContainerExecutors(curatorFrameworkOp,
				allUnSystemJobs);
		if (tasks != null && !tasks.isEmpty()) {
			for (String task : tasks) {
				ContainerVo containerVo = new ContainerVo();
				containerVo.setTaskId(task);
				containerVo.setContainerExecutorVos(containerExecutors.get(task));
				containerVo.setBindingJobNames(changeTypeOfBindingJobNames(taskBindJobNames.get(task)));
				containerVo.setContainerStatus(changeTypeOfContainerStatus(containerToken, task));
				ContainerConfig containerConfig = getContainerConfig(curatorFrameworkOp, task);
				containerVo.setContainerConfig(changeTypeOfContainerConfig(containerConfig));
				containerVo.setCreateTime(changeTypeOfCreateTime(containerConfig));
				containerVo.setContainerScaleJobVos(getContainerScaleJobVos(task));
				containerVo.setInstancesConfigured(String.valueOf(containerConfig.getInstances()));
				containerVos.add(containerVo);
			}
		}
		return containerVos;
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
			containerStatus = getContainerRestService().getContainerStatus(containerToken, task);
		} catch (SaturnJobConsoleException e) {
			errorMessage = "<font color='red'>get task status error, message is: "
					+ StringEscapeUtils.escapeHtml4(e.getMessage()) + "</font>";
		} finally {
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
	}

	private ContainerConfig getContainerConfig(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String task) {
		String taskConfigJson = curatorFrameworkOp.getData(ContainerNodePath.getDcosTaskConfigNodePath(task));
		return JSON.parseObject(taskConfigJson, ContainerConfig.class);
	}

	private String changeTypeOfCreateTime(ContainerConfig containerConfig) {
		Long createTime = containerConfig.getCreateTime();
		if (createTime != null) {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(createTime);
		} else {
			return null;
		}
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

	@Override
	public void addContainer(ContainerConfig containerConfig) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
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
		getContainerRestService().deploy(getContainerToken(), containerConfig);
		;
		containerConfig.setCreateTime(System.currentTimeMillis());
		replaceEnvSensitiveParams(containerConfig);
		curatorFrameworkOp.update(ContainerNodePath.getDcosTaskConfigNodePath(taskId),
				JSON.toJSONString(containerConfig));
	}

	@Override
	public void updateContainerInstances(String taskId, Integer instances) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		ContainerToken containerToken = getContainerToken();
		String dcosTaskIdNodePath = ContainerNodePath.getDcosTaskIdNodePath(taskId);
		String dcosTaskConfigNodePath = ContainerNodePath.getDcosTaskConfigNodePath(taskId);
		if (!curatorFrameworkOp.checkExists(dcosTaskIdNodePath)) {
			throw new SaturnJobConsoleException("The taskId already exists");
		}
		String taskConfigJson = curatorFrameworkOp.getData(dcosTaskConfigNodePath);
		ContainerConfig containerConfig = JSON.parseObject(taskConfigJson, ContainerConfig.class);
		getContainerRestService().scale(containerToken, taskId, instances);
		containerConfig.setInstances(instances);
		curatorFrameworkOp.update(dcosTaskConfigNodePath, JSON.toJSONString(containerConfig));
	}

	@Override
	public void removeContainer(String taskId) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		ContainerToken containerToken = getContainerToken();
		String dcosTaskIdNodePath = ContainerNodePath.getDcosTaskIdNodePath(taskId);
		if (!curatorFrameworkOp.checkExists(dcosTaskIdNodePath)) {
			throw new SaturnJobConsoleException("The taskId already exists");
		}
		List<ContainerScaleJobVo> containerScaleJobVos = getContainerScaleJobVos(taskId);
		List<String> allUnSystemJobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
		for (String job : allUnSystemJobs) {
			String preferListNodePath = JobNodePath.getConfigNodePath(job, "preferList");
			if (curatorFrameworkOp.checkExists(preferListNodePath)) {
				String preferList = curatorFrameworkOp.getData(preferListNodePath);
				if (preferList != null) {
					String[] split = preferList.trim().split(",");
					for (String tmp : split) {
						if (tmp.trim().equals("@" + taskId)) {
							throw new SaturnJobConsoleException(
									"Cannot destroy the container, because it's binding a job");
						}
					}
				}
			}
		}
		for (ContainerScaleJobVo containerScaleJobVo : containerScaleJobVos) {
			deleteContainerScaleJob(taskId, containerScaleJobVo.getJobName());
		}
		getContainerRestService().destroy(containerToken, taskId);
		curatorFrameworkOp.deleteRecursive(dcosTaskIdNodePath);
	}

	@Override
	public String getContainerDetail(String taskId) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		ContainerToken containerToken = getContainerToken();
		String dcosTaskIdNodePath = ContainerNodePath.getDcosTaskIdNodePath(taskId);
		if (!curatorFrameworkOp.checkExists(dcosTaskIdNodePath)) {
			throw new SaturnJobConsoleException("The taskId already exists");
		}
		return getContainerRestService().info(containerToken, taskId);
	}

	@Override
	public int getContainerRunningInstances(String taskId, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		return getContainerRestService().count(getContainerToken(curatorFrameworkOp), taskId);
	}

	@Override
	public String getRegistryCatalog() throws SaturnJobConsoleException {
		return getContainerRestService().getRegistryCatalog();
	}

	@Override
	public String getRegistryRepositoryTags(String repository) throws SaturnJobConsoleException {
		return getContainerRestService().getRegistryRepositoryTags(repository);
	}

	@Override
	public void addContainerScaleJob(String taskId, String jobDesc, Integer instances, String timeZone, String cron)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String jobName = SaturnConstants.SYSTEM_SCALE_JOB_PREFEX + System.currentTimeMillis();
		ContainerToken containerToken = getContainerToken();
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setDescription(jobDesc);
		jobConfig.setTimeZone(timeZone);
		jobConfig.setCron(cron);
		jobConfig.setJobMode(JobMode.system_scale);
		jobConfig.setJobType(JobBriefInfo.JobType.SHELL_JOB.name());
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
		RequestResult requestResult = executorService.addJobs(jobConfig);
		if (!requestResult.isSuccess()) {
			throw new SaturnJobConsoleException(requestResult.getMessage());
		}
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

	private ContainerScaleJob getContainerScaleJob(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String id,
			String jobName) throws SaturnJobConsoleException {
		ContainerScaleJob containerScaleJob = null;
		try {
			String data = curatorFrameworkOp.getData(ContainerNodePath.getDcosTaskScaleJobNodePath(id, jobName));
			if (data != null) {
				containerScaleJob = new ContainerScaleJob();
				ContainerScaleJobConfig containerScaleJobConfig = JSON.parseObject(data, ContainerScaleJobConfig.class);
				containerScaleJob.setContainerScaleJobConfig(containerScaleJobConfig);
				String enabledStr = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled"));
				containerScaleJob.setEnabled(Boolean.valueOf(enabledStr));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
		return containerScaleJob;
	}

	@Override
	public ContainerScaleJobVo getContainerScaleJobVo(String taskId, String jobName) throws SaturnJobConsoleException {
		ContainerScaleJobVo containerScaleJobVo = null;
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
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

	private List<ContainerScaleJob> getContainerScaleJobs(String id) throws SaturnJobConsoleException {
		List<ContainerScaleJob> containerScaleJobs = new ArrayList<>();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String dcosTaskScaleJobsNodePath = ContainerNodePath.getDcosTaskScaleJobsNodePath(id);
		if (curatorFrameworkOp.checkExists(dcosTaskScaleJobsNodePath)) {
			List<String> jobs = curatorFrameworkOp.getChildren(dcosTaskScaleJobsNodePath);
			if (jobs != null) {
				for (String job : jobs) {
					ContainerScaleJob containerScaleJob = getContainerScaleJob(curatorFrameworkOp, id, job);
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

	@Override
	public List<ContainerScaleJobVo> getContainerScaleJobVos(String taskId) throws SaturnJobConsoleException {
		List<ContainerScaleJobVo> containerScaleJobVos = new ArrayList<>();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String dcosTaskScaleJobsNodePath = ContainerNodePath.getDcosTaskScaleJobsNodePath(taskId);
		if (curatorFrameworkOp.checkExists(dcosTaskScaleJobsNodePath)) {
			List<String> jobs = curatorFrameworkOp.getChildren(dcosTaskScaleJobsNodePath);
			if (jobs != null) {
				for (String job : jobs) {
					ContainerScaleJobVo containerScaleJobVo = getContainerScaleJobVo(taskId, job);
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
	public void enableContainerScaleJob(String jobName, Boolean enable) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String enabledNodePath = JobNodePath.getConfigNodePath(jobName, "enabled");
		String enabledStr = curatorFrameworkOp.getData(enabledNodePath);
		Boolean enabled = Boolean.valueOf(enabledStr);
		if (enabled == enable) {
			throw new SaturnJobConsoleException("The job is already " + (enable ? "enabled" : "disabled"));
		}
		curatorFrameworkOp.update(enabledNodePath, enable);
	}

	@Override
	public void deleteContainerScaleJob(String taskId, String jobName) throws SaturnJobConsoleException {
		// disable job, and delete it
		// wait 5s to disable job at most
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
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
					JobStatus jobStatus = jobDimensionService.getJobStatus(jobName);
					if (JobStatus.STOPPED.equals(jobStatus)) {
						executorService.removeJob(jobName);
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

	private List<String> getTasks(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		List<String> tasks = new ArrayList<>();
		List<String> children = curatorFrameworkOp.getChildren(ContainerNodePath.getDcosTasksNodePath());
		if (children != null) {
			tasks.addAll(children);
		}
		return tasks;
	}

	private String getContainerScaleJobShardingItemParameters(ContainerToken containerToken, String taskId,
			Integer instances) throws SaturnJobConsoleException {
		return getContainerRestService().getContainerScaleJobShardingItemParameters(containerToken, taskId, instances);
	}

	private Map<String, List<String>> getTaskBindJobNames(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			List<String> allUnSystemJobs) throws SaturnJobConsoleException {
		Map<String, List<String>> map = new HashMap<>();
		for (String job : allUnSystemJobs) {
			String preferListNodePath = JobNodePath.getConfigNodePath(job, "preferList");
			if (curatorFrameworkOp.checkExists(preferListNodePath)) {
				String data = curatorFrameworkOp.getData(preferListNodePath);
				if (data != null) {
					String[] split = data.split(",");
					for (int i = 0; i < split.length; i++) {
						String tmp = split[i].trim();
						if (tmp.startsWith("@")) {
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
			}
		}
		return map;
	}

	private Map<String, List<ContainerExecutorVo>> getContainerExecutors(
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, List<String> allUnSystemJobs) {
		Map<String, List<ContainerExecutorVo>> containerExecutors = new HashMap<>();
		if (curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath())) {
			List<String> executors = curatorFrameworkOp.getChildren(ExecutorNodePath.getExecutorNodePath());
			if (executors != null) {
				for (String executor : executors) {
					String executorTaskNodePath = ExecutorNodePath.getExecutorTaskNodePath(executor);
					if (curatorFrameworkOp.checkExists(executorTaskNodePath)) {
						String task = curatorFrameworkOp.getData(executorTaskNodePath);
						if (task != null) {
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
		}
		for (String job : allUnSystemJobs) {
			Collection<List<ContainerExecutorVo>> values = containerExecutors.values();
			if (values != null) {
				for (List<ContainerExecutorVo> value : values) {
					if (value != null) {
						for (ContainerExecutorVo containerExecutorVo : value) {
							String jobServerShardingNodePath = JobNodePath.getServerNodePath(job,
									containerExecutorVo.getExecutorName(), "sharding");
							if (curatorFrameworkOp.checkExists(jobServerShardingNodePath)) {
								String sharding = curatorFrameworkOp.getData(jobServerShardingNodePath);
								if (sharding != null && sharding.trim().length() != 0) {
									boolean isRunning = false;
									String[] split = sharding.split(",");
									for (String tmp : split) {
										String runningNodePath = JobNodePath.getExecutionNodePath(job, tmp.trim(),
												"running");
										if (curatorFrameworkOp.checkExists(runningNodePath)) {
											isRunning = true;
											break;
										}
									}
									if (!isRunning) {
										String enabledNodePath = JobNodePath.getConfigNodePath(job, "enabled");
										if (Boolean.valueOf(curatorFrameworkOp.getData(enabledNodePath))) {
											isRunning = true;
										}
									}
									if (isRunning) {
										String runningJobNames = containerExecutorVo.getRunningJobNames();
										if (runningJobNames == null) {
											containerExecutorVo.setRunningJobNames(job);
										} else {
											containerExecutorVo.setRunningJobNames(runningJobNames + ",<br/>" + job);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return containerExecutors;
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

	private void deleteScaleJobNodePath(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String taskId,
			String jobName) {
		String dcosTaskScaleJobNodePath = ContainerNodePath.getDcosTaskScaleJobNodePath(taskId, jobName);
		curatorFrameworkOp.deleteRecursive(dcosTaskScaleJobNodePath);
	}
}
