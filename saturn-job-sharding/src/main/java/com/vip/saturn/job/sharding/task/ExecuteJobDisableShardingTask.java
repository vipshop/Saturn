package com.vip.saturn.job.sharding.task;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 作业禁用，摘取所有executor运行的该作业的shard，注意要相应地减loadLevel，不需要放回
 */
public class ExecuteJobDisableShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteJobDisableShardingTask.class);

	private String jobName;

	public ExecuteJobDisableShardingTask(NamespaceShardingService namespaceShardingService, String jobName) {
		super(namespaceShardingService);
		this.jobName = jobName;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {} with {} disable", this.getClass().getSimpleName(), jobName);
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) {
		// 摘取所有该作业的Shard
		shardList.addAll(namespaceShardingService.removeAllShardsOnExecutors(lastOnlineTrafficExecutorList, jobName));

		// 如果shardList为空，则没必要进行放回等操作，摘取失败
		if (shardList.isEmpty()) {
			return false;
		}

		return true;
	}

	@Override
	protected void putBackBalancing(List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) {
		// 不做操作
	}

}
