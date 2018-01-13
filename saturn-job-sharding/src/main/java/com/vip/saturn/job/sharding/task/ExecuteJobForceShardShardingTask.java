package com.vip.saturn.job.sharding.task;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 作业重排，移除所有executor的该作业shard，重新获取该作业的shards，finally删除forceShard结点
 */
public class ExecuteJobForceShardShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteJobForceShardShardingTask.class);

	private String jobName;

	public ExecuteJobForceShardShardingTask(NamespaceShardingService namespaceShardingService, String jobName) {
		super(namespaceShardingService);
		this.jobName = jobName;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {} with {} forceShard", this.getClass().getSimpleName(), jobName);
	}

	@Override
	public void run() {
		try {
			super.run();
		} finally {
			deleteForceShardNode();
		}
	}

	private void deleteForceShardNode() {
		try {
			String jobConfigForceShardNodePath = SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName);
			if (curatorFramework.checkExists().forPath(jobConfigForceShardNodePath) != null) {
				curatorFramework.delete().forPath(jobConfigForceShardNodePath);
			}
		} catch (Throwable t) {
			log.error("delete forceShard node error", t);
		}
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
		// 移除已经在Executor运行的该作业的所有Shard
		namespaceShardingService.removeAllShardsOnExecutors(lastOnlineTrafficExecutorList, jobName);
		// 修正所有executor对该作业的jobNameList
		fixJobNameList(lastOnlineExecutorList, jobName);
		// 如果该作业是启用状态，则创建该作业的Shard
		if (allEnableJobs.contains(jobName)) {
			shardList.addAll(createShards(jobName, lastOnlineTrafficExecutorList));
		}

		return true;
	}
}
