package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.ServerAllocationInfo;
import com.vip.saturn.job.console.domain.ServerBriefInfo;
import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Default implementation of ExecutorService.
 *
 * @author xiaopeng.he
 * @author kfchu
 */
@Service
public class ExecutorServiceImpl implements ExecutorService {

	private static final Logger log = LoggerFactory.getLogger(ExecutorServiceImpl.class);

	private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM,
			Locale.SIMPLIFIED_CHINESE);

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private RegistryCenterService registryCenterService;

	@Override
	public List<ServerBriefInfo> getExecutors(String namespace) throws SaturnJobConsoleException {
		return getExecutors(namespace, null);
	}

	@Override
	public List<ServerBriefInfo> getExecutors(String namespace, ServerStatus expectedServerStatus)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);

		List<String> executors = curatorFrameworkOp.getChildren(ExecutorNodePath.getExecutorNodePath());
		if (executors == null || executors.size() == 0) {
			return Lists.newArrayList();
		}

		List<ServerBriefInfo> executorInfoList = Lists.newArrayList();
		for (String executor : executors) {
			ServerBriefInfo executorInfo = getServerBriefInfo(executor, curatorFrameworkOp);
			if (expectedServerStatus == null || executorInfo.getStatus().equals(expectedServerStatus)) {
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

	@Override
	public ServerStatus getExecutorStatus(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		if (curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorIpNodePath(executorName))) {
			return ServerStatus.ONLINE;
		}
		return ServerStatus.OFFLINE;
	}

	private ServerBriefInfo getServerBriefInfo(String executorName, CuratorFrameworkOp curatorFrameworkOp) {
		ServerBriefInfo executorInfo = new ServerBriefInfo(executorName);
		String ip = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorIpNodePath(executorName));
		executorInfo.setServerIp(ip);
		if (!Strings.isNullOrEmpty(ip)) {
			executorInfo.setStatus(ServerStatus.ONLINE);
		} else {
			executorInfo.setStatus(ServerStatus.OFFLINE);
		}

		executorInfo.setNoTraffic(curatorFrameworkOp
				.checkExists(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName)));
		String lastBeginTime = curatorFrameworkOp
				.getData(ExecutorNodePath.getExecutorNodePath(executorInfo.getExecutorName(), "lastBeginTime"));
		executorInfo.setLastBeginTime(
				null == lastBeginTime ? null : dateFormat.format(new Date(Long.parseLong(lastBeginTime))));
		executorInfo.setVersion(
				curatorFrameworkOp.getData(ExecutorNodePath.getExecutorVersionNodePath(executorName)));
		String task = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorTaskNodePath(executorName));
		if (StringUtils.isNotBlank(task)) {
			executorInfo.setGroupName(task);
			executorInfo.setContainer(true);
		}
		return executorInfo;
	}

	@Override
	public ServerAllocationInfo getExecutorAllocation(String namespace, String executorName)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);

		List<String> jobs = null;
		try {
			jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
		} catch (SaturnJobConsoleException e) {
			log.error(e.getMessage(), e);
		}

		ServerAllocationInfo serverAllocationInfo = new ServerAllocationInfo(executorName);

		if (jobs == null || jobs.size() == 0) {
			return serverAllocationInfo;
		}

		for (String jobName : jobs) {
			String serverNodePath = JobNodePath.getServerNodePath(jobName);
			if (!curatorFrameworkOp.checkExists(serverNodePath)) {
				continue;
			}

			String sharding = curatorFrameworkOp
					.getData(JobNodePath.getServerNodePath(jobName, executorName, "sharding"));
			if (StringUtils.isNotBlank(sharding)) {
				// 作业状态为STOPPED的即使有残留分片也不显示该分片
				if (JobStatus.STOPPED.equals(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp))) {
					continue;
				}
				// concat executorSharding
				serverAllocationInfo.getAllocationMap().put(jobName, sharding);
				// calculate totalLoad
				String loadLevelNode = curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"));
				Integer loadLevel = 1;
				if (StringUtils.isNotBlank(loadLevelNode)) {
					loadLevel = Integer.parseInt(loadLevelNode);
				}

				int shardingItemNum = sharding.split(",").length;
				int curJobLoad = shardingItemNum * loadLevel;
				int totalLoad = serverAllocationInfo.getTotalLoadLevel();
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
		List<String> jobNames = new ArrayList<>();
		try {
			jobNames = jobDimensionService.getAllJobs(curatorFrameworkOp);
		} catch (SaturnJobConsoleException e) {
			log.error(e.getMessage(), e);
		}
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

	private void validateIfExecutorNameExisted(String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		if (!curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executorName))) {
			throw new SaturnJobConsoleException("The executorName(" + executorName + ") is not existed.");
		}
	}

	private CuratorFrameworkOp getCuratorFrameworkOp(String namespace) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp;
		try {
			curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		} catch (SaturnJobConsoleException e) {
			throw new SaturnJobConsoleException("no CuratorFramework found for namespace:" + namespace);
		}
		return curatorFrameworkOp;
	}
}
