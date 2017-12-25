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

import java.text.DateFormat;
import java.util.*;

import javax.annotation.Resource;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.ServerBriefInfo;
import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.ServerDimensionService;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;

@Service
public class ServerDimensionServiceImpl implements ServerDimensionService {

	private static final Logger log = LoggerFactory.getLogger(ServerDimensionServiceImpl.class);

	private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM,
			Locale.SIMPLIFIED_CHINESE);
	@Resource
	private CuratorRepository curatorRepository;
	@Resource
	private JobDimensionService jobDimensionService;

	@Override
	public Map<String, Object> getAllServersBriefInfo() {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		HashMap<String, Object> model = new HashMap<String, Object>();
		Map<String, ServerBriefInfo> sbfMap = new LinkedHashMap<String, ServerBriefInfo>();
		List<String> jobs = new ArrayList<>();
		try {
			jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
		} catch (SaturnJobConsoleException e) {
			log.error(e.getMessage(), e);
		}

		Map<String, Map<String, Integer>> jobNameExecutorNameTotalLevel = new HashMap<>();
		String executorNodePath = ExecutorNodePath.getExecutorNodePath();
		if (curatorFrameworkOp.checkExists(executorNodePath)) {
			List<String> executors = curatorFrameworkOp.getChildren(executorNodePath);
			if (!CollectionUtils.isEmpty(executors)) {
				for (String executor : executors) {
					ServerBriefInfo sbf = new ServerBriefInfo(executor);
					String ip = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorNodePath(executor, "ip"));
					sbf.setServerIp(ip);
					sbf.setNoTraffic(curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executor, "noTraffic")));
					String lastBeginTime = curatorFrameworkOp
							.getData(ExecutorNodePath.getExecutorNodePath(sbf.getExecutorName(), "lastBeginTime"));
					sbf.setLastBeginTime(
							null == lastBeginTime ? null : dateFormat.format(new Date(Long.parseLong(lastBeginTime))));
					if (!Strings.isNullOrEmpty(ip)) {
						sbf.setStatus(ServerStatus.ONLINE);
					} else {
						sbf.setStatus(ServerStatus.OFFLINE);
					}
					sbf.setVersion(
							curatorFrameworkOp.getData(ExecutorNodePath.getExecutorNodePath(executor, "version")));
					if (!CollectionUtils.isEmpty(jobs)) {
						for (String jobName : jobs) {
							String serverNodePath = JobNodePath.getServerNodePath(jobName);
							if (!curatorFrameworkOp.checkExists(serverNodePath)) {
								continue;
							}
							if (Strings.isNullOrEmpty(sbf.getServerIp())) {
								String serverIp = curatorFrameworkOp
										.getData(JobNodePath.getServerNodePath(jobName, executor, "ip"));
								sbf.setServerIp(serverIp);
							}
							Map<String, Integer> executorNameWithTotalLevel = null;
							if (jobNameExecutorNameTotalLevel.containsKey(jobName)) {
								executorNameWithTotalLevel = jobNameExecutorNameTotalLevel.get(jobName);
							} else {
								executorNameWithTotalLevel = new LinkedHashMap<>();
								jobNameExecutorNameTotalLevel.put(jobName, executorNameWithTotalLevel);
							}
							if (ServerStatus.ONLINE.equals(sbf.getStatus())) {// 负荷分布图只显示online的Executor
								executorNameWithTotalLevel.put(executor, 0);
							}
							String sharding = curatorFrameworkOp
									.getData(JobNodePath.getServerNodePath(jobName, executor, "sharding"));
							if (!Strings.isNullOrEmpty(sharding)) {
								sbf.setHasSharding(true);// 如果有分片信息则前端需要屏蔽删除按钮
								if (JobStatus.STOPPED.equals(jobDimensionService.getJobStatus(jobName))) {
									continue;// 作业状态为STOPPED的即使有残留分片也不显示该分片
								}
								if (ServerStatus.OFFLINE.equals(sbf.getStatus())) {// offline的executor即使有残留分片也不显示该分片
									continue;
								}
								// concat executorSharding
								String executorSharding = jobName + ":" + sharding;
								if (Strings.isNullOrEmpty(sbf.getSharding())) {// 如果有分片信息则前端需要屏蔽删除按钮
									sbf.setSharding(executorSharding);
								} else {
									sbf.setSharding(sbf.getSharding() + "<br/>" + executorSharding);
								}
								// calculate totalLoadLevel
								String loadLevelNode = curatorFrameworkOp
										.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"));
								Integer loadLevel = 1;
								if (!Strings.isNullOrEmpty(loadLevelNode)) {
									loadLevel = Integer.parseInt(loadLevelNode);
								}
								Integer totalLoadLevel = sbf.getTotalLoadLevel();
								int thisJobsLoad = (sharding.split(",").length * loadLevel);
								sbf.setTotalLoadLevel(
										(sbf.getTotalLoadLevel() == null ? 0 : totalLoadLevel) + thisJobsLoad);
								executorNameWithTotalLevel.put(executor, thisJobsLoad);
							}
						}
					}
					sbfMap.put(executor, sbf);
				}
			}
			model.put("serverInfos", sbfMap.values());
			model.put("jobShardLoadLevels", jobNameExecutorNameTotalLevel);
		}
		return model;
	}

	@Override
	public ServerStatus getExecutorStatus(String executor) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		if (curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executor, "ip"))) {
			return ServerStatus.ONLINE;
		}
		return ServerStatus.OFFLINE;
	}

	@Override
	public void removeOffLineExecutor(String executor) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		curatorFrameworkOp.deleteRecursive(ExecutorNodePath.getExecutorNodePath(executor));
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
			String executorNode = JobNodePath.getServerNodePath(jobName, executor);
			if (!curatorFrameworkOp.checkExists(executorNode)) {
				continue;
			}
			curatorFrameworkOp.deleteRecursive(executorNode);
		}
	}

	@Override
	public boolean isRunning(String jobName, String executor) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String status = curatorFrameworkOp.getData(JobNodePath.getServerStatus(jobName, executor));
		if ("RUNNING".equals(status)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isReady(String jobName, String executor) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String status = curatorFrameworkOp.getData(JobNodePath.getServerStatus(jobName, executor));
		if ("READY".equals(status)) {
			return true;
		}
		return false;
	}

	@Override
	public void trafficExtraction(String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		validateExecutorNameExisted(executorName, curatorFrameworkOp);
		curatorFrameworkOp.create(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName));
	}

	@Override
	public void traficRecovery(String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		validateExecutorNameExisted(executorName, curatorFrameworkOp);
		curatorFrameworkOp.deleteRecursive(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName));
	}

	private void validateExecutorNameExisted(String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		if (!curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executorName))) {
			throw new SaturnJobConsoleException("The executorName(" + executorName + ") is not existing");
		}
	}

}
