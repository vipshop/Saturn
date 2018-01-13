package com.vip.saturn.job.sharding.task;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 恢复executor流量，标记该executor的noTraffic为false，平衡摘取分片
 */
public class ExecuteRecoverTrafficShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteRecoverTrafficShardingTask.class);

	private String executorName;

	public ExecuteRecoverTrafficShardingTask(NamespaceShardingService namespaceShardingService, String executorName) {
		super(namespaceShardingService);
		this.executorName = executorName;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {} with {} recover traffic", this.getClass().getSimpleName(), executorName);
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
		// 设置该executor的noTraffic为false
		Executor targetExecutor = null;
		Iterator<Executor> iterator = lastOnlineExecutorList.iterator();
		while (iterator.hasNext()) {
			Executor executor = iterator.next();
			if (executor.getExecutorName().equals(executorName)) {
				executor.setNoTraffic(false);
				lastOnlineTrafficExecutorList.add(executor);
				targetExecutor = executor;
				break;
			}
		}
		if (targetExecutor == null) {
			log.warn("The executor {} maybe offline, unnecessary to recover traffic", executorName);
			return false;
		}

		// 平衡摘取每个作业能够运行的分片，可以视为jobNameList中每个作业的jobServerOnline
		final List<String> jobNameList = targetExecutor.getJobNameList();
		for (String jobName : jobNameList) {
			new ExecuteJobServerOnlineShardingTask(namespaceShardingService, jobName, executorName)
					.pickIntelligent(allEnableJobs, shardList, lastOnlineTrafficExecutorList);
		}

		return true;
	}
}
