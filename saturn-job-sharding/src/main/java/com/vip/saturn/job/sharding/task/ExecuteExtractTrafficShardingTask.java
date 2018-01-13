package com.vip.saturn.job.sharding.task;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 摘取executor流量，标记该executor的noTraffic为true，并移除其所有作业分片，只摘取所有非本地作业分片，设置totalLoadLevel为0
 */
public class ExecuteExtractTrafficShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteExtractTrafficShardingTask.class);

	private String executorName;

	public ExecuteExtractTrafficShardingTask(NamespaceShardingService namespaceShardingService, String executorName) {
		super(namespaceShardingService);
		this.namespaceShardingService = namespaceShardingService;
		this.executorName = executorName;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {} with {} extract traffic", this.getClass().getSimpleName(), executorName);
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
		// 摘取该executor的所有作业分片
		Executor targetExecutor = null;
		Iterator<Executor> iterator = lastOnlineTrafficExecutorList.iterator();
		while (iterator.hasNext()) {
			Executor executor = iterator.next();
			if (executor.getExecutorName().equals(executorName)) {
				shardList.addAll(executor.getShardList());
				clearTargetExecutorShards(executor);
				targetExecutor = executor;
				iterator.remove();
				break;
			}
		}

		if (targetExecutor == null) {
			log.warn("The executor {} maybe offline, unnecessary to extract traffic", executorName);
			return false;
		}

		// 移除本地模式的作业分片
		Iterator<Shard> shardIterator = shardList.iterator();
		while (shardIterator.hasNext()) {
			Shard shard = shardIterator.next();
			if (isLocalMode(shard.getJobName())) {
				shardIterator.remove();
			}
		}

		return true;
	}

	private void clearTargetExecutorShards(Executor executor) {
		executor.getShardList().clear();
		executor.setNoTraffic(true);
		executor.setTotalLoadLevel(0);
	}
}
