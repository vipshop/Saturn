package com.vip.saturn.job.sharding.task;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * executor上线，仅仅添加executor空壳，如果其不存在；如果已经存在，重新设置下ip，防止ExecuteJobServerOnlineShardingTask先于执行而没设ip
 *
 * 特别的，如果当前没有executor，也就是这是第一台executor上线，则需要域全量分片，因为可能已经有作业处理启用状态了。
 */
public class ExecuteOnlineShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteOnlineShardingTask.class);

	private String executorName;

	private String ip;

	public ExecuteOnlineShardingTask(NamespaceShardingService namespaceShardingService, String executorName,
			String ip) {
		super(namespaceShardingService);
		this.namespaceShardingService = namespaceShardingService;
		this.executorName = executorName;
		this.ip = ip;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {} with {} online", this.getClass().getSimpleName(), executorName);
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
		// 如果没有Executor在运行，则需要进行全量分片
		if (lastOnlineExecutorList.isEmpty()) {
			log.warn("There are no running executors, need all sharding");
			namespaceShardingService.setNeedAllSharding(true);
			namespaceShardingService.shardingCountIncrementAndGet();
			executorService.submit(new ExecuteAllShardingTask(namespaceShardingService));
			return false;
		}

		Executor targetExecutor = null;
		for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
			Executor tmp = lastOnlineExecutorList.get(i);
			if (tmp.getExecutorName().equals(executorName)) {
				targetExecutor = tmp;
				break;
			}
		}
		if (targetExecutor == null) {
			targetExecutor = new Executor();
			targetExecutor.setExecutorName(executorName);
			targetExecutor.setIp(ip);
			targetExecutor.setNoTraffic(getExecutorNoTraffic(executorName));
			targetExecutor.setShardList(new ArrayList<Shard>());
			targetExecutor.setJobNameList(new ArrayList<String>());
			lastOnlineExecutorList.add(targetExecutor);
			if (!targetExecutor.isNoTraffic()) {
				lastOnlineTrafficExecutorList.add(targetExecutor);
			}
		} else { // 重新设置下ip
			targetExecutor.setIp(ip);
		}

		return true;
	}

}
