package com.vip.saturn.job.sharding.task;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * executor下线，摘取该executor运行的所有非本地模式作业，移除该executor
 */
public class ExecuteOfflineShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteOfflineShardingTask.class);

	private String executorName;

	public ExecuteOfflineShardingTask(NamespaceShardingService namespaceShardingService, String executorName) {
		super(namespaceShardingService);
		this.executorName = executorName;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {} with {} offline", this.getClass().getSimpleName(), executorName);
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
		/**
		 * 摘取下线的executor全部Shard
		 */
		Executor targetExecutor = null;
		Iterator<Executor> lastOnlineExecutorsIterator = lastOnlineExecutorList.iterator();
		while (lastOnlineExecutorsIterator.hasNext()) {
			Executor executor = lastOnlineExecutorsIterator.next();
			if (executor.getExecutorName().equals(executorName)) {
				targetExecutor = executor;
				lastOnlineExecutorsIterator.remove();
				shardList.addAll(executor.getShardList());
				break;
			}
		}

		if (targetExecutor != null) {
			Iterator<Executor> lastOnlineTrafficExecutorsIterator = lastOnlineTrafficExecutorList.iterator();
			while (lastOnlineTrafficExecutorsIterator.hasNext()) {
				Executor executor = lastOnlineTrafficExecutorsIterator.next();
				if (targetExecutor.equals(executor)) {
					lastOnlineTrafficExecutorsIterator.remove();
					break;
				}
			}
		}

		// 如果该executor实际上已经在此之前下线，则摘取失败
		if (targetExecutor == null) {
			return false;
		}

		// 移除本地模式的作业分片
		Iterator<Shard> iterator = shardList.iterator();
		while (iterator.hasNext()) {
			Shard shard = iterator.next();
			if (isLocalMode(shard.getJobName())) {
				iterator.remove();
			}
		}

		return true;
	}

}
