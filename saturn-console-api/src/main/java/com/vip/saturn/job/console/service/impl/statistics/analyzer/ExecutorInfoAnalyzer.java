package com.vip.saturn.job.console.service.impl.statistics.analyzer;

import com.vip.saturn.job.console.domain.ExecutorStatistics;
import com.vip.saturn.job.console.domain.JobStatistics;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author timmy.hu
 */
public class ExecutorInfoAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(ExecutorInfoAnalyzer.class);

	private Map<String/** {executorName}-{domain} */
			, ExecutorStatistics> executorMap = new ConcurrentHashMap<>();

	private Map<String, Long> versionDomainNumber = new ConcurrentHashMap<>(); // 不同版本的域数量

	private Map<String, Long> versionExecutorNumber = new ConcurrentHashMap<>(); // 不同版本的executor数量

	private AtomicInteger exeInDocker = new AtomicInteger(0);

	private AtomicInteger exeNotInDocker = new AtomicInteger(0);

	public void analyzeExecutor(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
								RegistryCenterConfiguration config) {
		long executorNumber = 0L; // 该域的在线executor数量
		// 统计物理容器资源，统计版本数据
		if (!curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath())) {
			addVersionNumber("-1", executorNumber);
			return;
		}

		List<String> executors = curatorFrameworkOp.getChildren(ExecutorNodePath.getExecutorNodePath());
		if (executors == null) {
			addVersionNumber("-1", executorNumber);
			return;
		}

		String version = null; // 该域的版本号
		for (String exe : executors) {
			// 在线的才统计
			if (curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorIpNodePath(exe))) {
				// 统计是物理机还是容器
				String executorMapKey = exe + "-" + config.getNamespace();
				ExecutorStatistics executorStatistics = executorMap.get(executorMapKey);
				if (executorStatistics == null) {
					executorStatistics = new ExecutorStatistics(exe, config.getNamespace());
					executorStatistics.setNns(config.getNameAndNamespace());
					executorStatistics
							.setIp(curatorFrameworkOp.getData(ExecutorNodePath.getExecutorIpNodePath(exe)));
					executorMap.put(executorMapKey, executorStatistics);
				}
				// set runInDocker field
				if (isExecutorInDocker(curatorFrameworkOp, exe)) {
					executorStatistics.setRunInDocker(true);
					exeInDocker.incrementAndGet();
				} else {
					exeNotInDocker.incrementAndGet();
				}
			}
			// 获取版本号
			if (version == null) {
				version = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorVersionNodePath(exe));
			}
		}
		executorNumber = executors.size();
		// 统计版本数据
		if (version == null) { // 未知版本
			version = "-1";
		}
		addVersionNumber(version, executorNumber);
	}

	public boolean isExecutorInDocker(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
									  String executorName) {
		return curatorFrameworkOp.checkExists(ExecutorNodePath.get$ExecutorTaskNodePath(executorName));
	}

	private synchronized void addVersionNumber(String version, long executorNumber) {
		if (versionDomainNumber.containsKey(version)) {
			Long domainNumber = versionDomainNumber.get(version);
			versionDomainNumber.put(version, domainNumber + 1);
		} else {
			versionDomainNumber.put(version, 1L);
		}
		if (versionExecutorNumber.containsKey(version)) {
			Long executorNumber0 = versionExecutorNumber.get(version);
			versionExecutorNumber.put(version, executorNumber0 + executorNumber);
		} else {
			if (executorNumber != 0) {
				versionExecutorNumber.put(version, executorNumber);
			}
		}
	}

	public void analyzeServer(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, List<String> servers, String job,
							  String nns,
							  RegistryCenterConfiguration config, int loadLevel, JobStatistics jobStatistics) {
		for (String server : servers) {
			// 如果结点存活，算两样东西：1.遍历所有servers节点里面的processSuccessCount &
			// processFailureCount，用以统计作业每天的执行次数；2.统计executor的loadLevel;，
			if (!curatorFrameworkOp.checkExists(JobNodePath.getServerStatus(job, server))) {
				return;
			}
			// 1.遍历所有servers节点里面的processSuccessCount && processFailureCount，用以统计作业每天的执行次数；
			calcJobProcessCount(curatorFrameworkOp, nns, server, job, config.getNamespace(), jobStatistics);

			// 2.统计executor的loadLevel
			calcLoadLevelOfExecutors(curatorFrameworkOp, nns, server, job, config.getNamespace(), loadLevel, jobStatistics);
		}
	}

	private void calcJobProcessCount(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
									 String nns, String server, String job, String namespace,
									 JobStatistics jobStatistics)	{
		// 1.遍历所有servers节点里面的processSuccessCount &
		// processFailureCount，用以统计作业每天的执行次数；
		try {
			String processSuccessCountOfThisExeStr = curatorFrameworkOp
					.getData(JobNodePath.getProcessSucessCount(job, server));
			String processFailureCountOfThisExeStr = curatorFrameworkOp
					.getData(JobNodePath.getProcessFailureCount(job, server));
			int processSuccessCountOfThisExe = StringUtils.isBlank(processSuccessCountOfThisExeStr) ? 0
					: Integer.parseInt(processSuccessCountOfThisExeStr);
			int processFailureCountOfThisExe = StringUtils.isBlank(processFailureCountOfThisExeStr) ? 0
					: Integer.parseInt(processFailureCountOfThisExeStr);

			// executor当天运行成功失败数
			String executorMapKey = server + "-" + namespace;
			ExecutorStatistics executorStatistics = executorMap.get(executorMapKey);
			if (executorStatistics == null) {
				executorStatistics = new ExecutorStatistics(server, namespace);
				executorStatistics.setNns(nns);
				executorStatistics
						.setIp(curatorFrameworkOp.getData(ExecutorNodePath.getExecutorIpNodePath(server)));
				executorMap.put(executorMapKey, executorStatistics);
			}
			executorStatistics.setFailureCountOfTheDay(
					executorStatistics.getFailureCountOfTheDay() + processFailureCountOfThisExe);
			executorStatistics.setProcessCountOfTheDay(executorStatistics.getProcessCountOfTheDay()
					+ processSuccessCountOfThisExe + processFailureCountOfThisExe);
			jobStatistics.incrProcessCountOfTheDay(processSuccessCountOfThisExe + processFailureCountOfThisExe);
			jobStatistics.incrFailureCountOfTheDay(processFailureCountOfThisExe);
		} catch (Exception e) {
			log.info(e.getMessage(), e);
		}
	}

	private void calcLoadLevelOfExecutors(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
										  String nns,
										  String server, String job, String namespace,
										  int loadLevel, JobStatistics jobStatistics){
		try {
			// enabled 的作业才需要计算权重
			if (!Boolean.parseBoolean(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(job, "enabled")))) {
				return;
			}

			String sharding = curatorFrameworkOp.getData(JobNodePath.getServerSharding(job, server));
			if (StringUtils.isNotEmpty(sharding)) {
				// 更新job的executorsAndshards
				String exesAndShards = (jobStatistics.getExecutorsAndShards() == null ? ""
						: jobStatistics.getExecutorsAndShards()) + server + ":" + sharding + "; ";
				jobStatistics.setExecutorsAndShards(exesAndShards);
				// 2.统计是物理机还是容器
				String executorMapKey = server + "-" + namespace;
				ExecutorStatistics executorStatistics = executorMap.get(executorMapKey);
				if (executorStatistics == null) {
					executorStatistics = new ExecutorStatistics(server, namespace);
					executorStatistics.setNns(nns);
					executorStatistics.setIp(curatorFrameworkOp
							.getData(ExecutorNodePath.getExecutorIpNodePath(server)));
					executorMap.put(executorMapKey, executorStatistics);
					// set runInDocker field
					if (isExecutorInDocker(curatorFrameworkOp, server)) {
						executorStatistics.setRunInDocker(true);
						exeInDocker.incrementAndGet();
					} else {
						exeNotInDocker.incrementAndGet();
					}
				}
				if (executorStatistics.getJobAndShardings() != null) {
					executorStatistics.setJobAndShardings(
							executorStatistics.getJobAndShardings() + job + ":" + sharding + ";");
				} else {
					executorStatistics.setJobAndShardings(job + ":" + sharding + ";");
				}
				int newLoad = executorStatistics.getLoadLevel() + (loadLevel * sharding.split(",").length);
				executorStatistics.setLoadLevel(newLoad);
			}
		} catch (Exception e) {
			log.info(e.getMessage(), e);
		}
	}

	public Map<String, ExecutorStatistics> getExecutorMap() {
		return executorMap;
	}

	public List<ExecutorStatistics> getExecutorList() {
		return new ArrayList<ExecutorStatistics>(executorMap.values());
	}

	public Map<String, Long> getVersionDomainNumber() {
		return versionDomainNumber;
	}

	public Map<String, Long> getVersionExecutorNumber() {
		return versionExecutorNumber;
	}

	public int getExeInDocker() {
		return exeInDocker.get();
	}

	public int getExeNotInDocker() {
		return exeNotInDocker.get();
	}
}
