package com.vip.saturn.job.console.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConsoleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of ExecutorService.
 *
 * @author xiaopeng.he
 * @author kfchu
 */
public class ExecutorServiceImpl implements ExecutorService {

	private static final int DEFAULT_MAX_SECONDS_FORCE_KILL_EXECUTOR = 300;
	private static final int SMALLEST_VERSION_SUPPORTED_DUMP = 3;
	private static final Set<String> SUPPORT_DUMP_VERSION_WHITE_LIST = Sets.newHashSet("saturn-dev", "master-SNAPSHOT");

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private JobService jobService;

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private SystemConfigService systemConfigService;

	@Override
	public List<ServerBriefInfo> getExecutors(String namespace) throws SaturnJobConsoleException {
		return getExecutors(namespace, null);
	}

	@Override
	public List<ServerBriefInfo> getExecutors(String namespace, ServerStatus expectedServerStatus)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);

		List<String> executors = curatorFrameworkOp.getChildren(ExecutorNodePath.getExecutorNodePath());
		if (executors == null || executors.isEmpty()) {
			return Lists.newArrayList();
		}

		List<ServerBriefInfo> executorInfoList = Lists.newArrayList();
		for (String executor : executors) {
			ServerBriefInfo executorInfo = getServerBriefInfo(executor, curatorFrameworkOp);
			if (expectedServerStatus == null || executorInfo.getStatus() == expectedServerStatus) {
				executorInfoList.add(executorInfo);
			}
		}

		return executorInfoList;
	}

	@Override
	public ServerBriefInfo getExecutor(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);

		if (!curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executorName))) {
			return null;
		}

		return getServerBriefInfo(executorName, curatorFrameworkOp);
	}

	private ServerBriefInfo getServerBriefInfo(String executorName, CuratorFrameworkOp curatorFrameworkOp) {
		ServerBriefInfo executorInfo = new ServerBriefInfo(executorName);
		String ip = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorIpNodePath(executorName));
		executorInfo.setServerIp(ip);
		if (StringUtils.isNotBlank(ip)) {
			executorInfo.setStatus(ServerStatus.ONLINE);
		} else {
			executorInfo.setStatus(ServerStatus.OFFLINE);
		}

		String restartNodePath = ExecutorNodePath.getExecutorRestartNodePath(executorName);
		long restartTriggerTime = curatorFrameworkOp.getCtime(restartNodePath);
		long now = System.currentTimeMillis();

		long maxRestartInv = systemConfigService.getIntegerValue(SystemConfigProperties.MAX_SECONDS_FORCE_KILL_EXECUTOR,
				DEFAULT_MAX_SECONDS_FORCE_KILL_EXECUTOR) * 1000L;

		// 如果restart结点存在，restart结点存在的时间<300s，executor状态列显示RESTARTING；
		if (0 != restartTriggerTime && now - restartTriggerTime < maxRestartInv) {
			executorInfo.setRestarting(true);
		} else {
			executorInfo.setRestarting(false);
		}

		// 是否已被摘流量
		executorInfo.setNoTraffic(
				curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName)));
		// lastBeginTime
		String lastBeginTime = curatorFrameworkOp
				.getData(ExecutorNodePath.getExecutorNodePath(executorInfo.getExecutorName(), "lastBeginTime"));
		executorInfo.setLastBeginTime(SaturnConsoleUtils.parseMillisecond2DisplayTime(lastBeginTime));
		// version
		executorInfo.setVersion(curatorFrameworkOp.getData(ExecutorNodePath.getExecutorVersionNodePath(executorName)));

		String task = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorTaskNodePath(executorName));
		if (StringUtils.isNotBlank(task)) {
			// 容器组
			executorInfo.setGroupName(task);
			// 是否容器
			executorInfo.setContainer(true);
		}
		return executorInfo;
	}

	@Override
	public ServerAllocationInfo getExecutorAllocation(String namespace, String executorName)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);

		List<JobConfig> unSystemJobs = jobService.getUnSystemJobs(namespace);

		ServerAllocationInfo serverAllocationInfo = new ServerAllocationInfo(executorName);

		for (JobConfig jobConfig : unSystemJobs) {
			String jobName = jobConfig.getJobName();
			String serverNodePath = JobNodePath.getServerNodePath(jobName);
			if (!curatorFrameworkOp.checkExists(serverNodePath)) {
				continue;
			}

			String sharding = curatorFrameworkOp
					.getData(JobNodePath.getServerNodePath(jobName, executorName, "sharding"));
			if (StringUtils.isNotBlank(sharding)) {
				JobStatus jobStatus = jobService.getJobStatus(namespace, jobName);

				// 作业状态为STOPPED的即使有残留分片也不显示该分片
				if (JobStatus.STOPPED.equals(jobStatus)) {
					continue;
				}

				String loadLevelNode = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"));
				Integer loadLevel = 1;
				if (StringUtils.isNotBlank(loadLevelNode)) {
					loadLevel = Integer.valueOf(loadLevelNode);
				}

				int shardingItemNum = sharding.split(",").length;
				int curJobLoad = shardingItemNum * loadLevel;
				int totalLoad = serverAllocationInfo.getTotalLoadLevel();

				Map<String, String> jobNameAndStatus = new HashMap<>();
				jobNameAndStatus.put("jobName", jobName);
				jobNameAndStatus.put("status", jobStatus.name());
				jobNameAndStatus.put("sharding", sharding);
				serverAllocationInfo.getJobStatus().add(jobNameAndStatus);
				serverAllocationInfo.setTotalLoadLevel(totalLoad + curJobLoad);
			}
		}

		return serverAllocationInfo;
	}

	@Override
	public void extractTraffic(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		validateIfExecutorNameExisted(executorName, curatorFrameworkOp);
		curatorFrameworkOp.create(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName));
	}

	@Override
	public void recoverTraffic(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		validateIfExecutorNameExisted(executorName, curatorFrameworkOp);
		curatorFrameworkOp.deleteRecursive(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName));
	}

	@Override
	public void removeExecutor(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		curatorFrameworkOp.deleteRecursive(ExecutorNodePath.getExecutorNodePath(executorName));
		List<String> jobNames = jobService.getAllJobNamesFromZK(namespace);

		if (CollectionUtils.isEmpty(jobNames)) {
			return;
		}
		for (String jobName : jobNames) {
			String executorNode = JobNodePath.getServerNodePath(jobName, executorName);
			curatorFrameworkOp.deleteRecursive(executorNode);
		}
	}

	@Override
	public void shardAll(String namespace) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		String shardAllAtOnceNodePath = ExecutorNodePath.getExecutorShardingNodePath("shardAllAtOnce");
		curatorFrameworkOp.deleteRecursive(shardAllAtOnceNodePath);
		curatorFrameworkOp.create(shardAllAtOnceNodePath);
	}

	@Override
	public void dump(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		String version = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorVersionNodePath(executorName));
		if (!isVersionSupportedDump(version)) {
			throw new SaturnJobConsoleException(SaturnJobConsoleException.ERROR_CODE_BAD_REQUEST,
					"Saturn executor版本低于3.0.0无法进行一键dump");
		}
		String dumpNodePath = ExecutorNodePath.getExecutorDumpNodePath(executorName);
		curatorFrameworkOp.delete(dumpNodePath);
		curatorFrameworkOp.create(dumpNodePath);
	}

	private boolean isVersionSupportedDump(String version) {
		if (SUPPORT_DUMP_VERSION_WHITE_LIST.contains(version)) {
			return true;
		}

		String[] items = version.split("\\.");
		if (items.length < 3) {
			return false;
		}

		int majorVersion = Integer.parseInt(items[0]);
		return majorVersion >= SMALLEST_VERSION_SUPPORTED_DUMP;
	}

	@Override
	public File dumpAsFile(String namespace, String executorName) throws SaturnJobConsoleException {
		throw new UnsupportedOperationException("this method is not supported yet");
	}

	@Override
	public void restart(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		String restartNodePath = ExecutorNodePath.getExecutorRestartNodePath(executorName);
		curatorFrameworkOp.delete(restartNodePath);
		curatorFrameworkOp.create(restartNodePath);
	}

	private void validateIfExecutorNameExisted(String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		if (!curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executorName))) {
			throw new SaturnJobConsoleException("The executorName(" + executorName + ") is not existed.");
		}
	}

	protected CuratorFrameworkOp getCuratorFrameworkOp(String namespace) throws SaturnJobConsoleException {
		return registryCenterService.getCuratorFrameworkOp(namespace);
	}
}
