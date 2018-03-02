package com.vip.saturn.job.sharding.listener;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

/**
 * @author hebelala
 */
public class JobServersTriggerShardingListener extends AbstractTreeCacheListener {

	private String jobName;
	private NamespaceShardingService namespaceShardingService;

	public JobServersTriggerShardingListener(String jobName, NamespaceShardingService namespaceShardingService) {
		this.jobName = jobName;
		this.namespaceShardingService = namespaceShardingService;
	}

	@Override
	public void childEvent(TreeCacheEvent.Type type, String path, String nodeData) throws Exception {
		if (isJobServerStatusAddedOrRemoved(type, path)) {
			String executorName = SaturnExecutorsNode.getJobServersExecutorNameByStatusPath(path);
			if (type == Type.NODE_ADDED) {
				namespaceShardingService.asyncShardingWhenJobServerOnline(jobName, executorName);
			} else {
				namespaceShardingService.asyncShardingWhenJobServerOffline(jobName, executorName);
			}
		}
	}

	private boolean isJobServerStatusAddedOrRemoved(TreeCacheEvent.Type type, String path) {
		return (type == Type.NODE_ADDED || type == Type.NODE_REMOVED) && path
				.matches(SaturnExecutorsNode.getJobServersExecutorStatusNodePathRegex(jobName));
	}

}
