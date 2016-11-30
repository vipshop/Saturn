package com.vip.saturn.job.sharding.listener;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;

/**
 * 
 * @author chembo.huang
 *
 */
public class ExecutorOnlineOfflineTriggerShardingListener extends AbstractTreeCacheListener {

	private NamespaceShardingService namespaceShardingService;
	
	public ExecutorOnlineOfflineTriggerShardingListener(NamespaceShardingService namespaceShardingService) {
		this.namespaceShardingService = namespaceShardingService;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		if(isExecutorOnline(type, path)) {
			String executorName = SaturnExecutorsNode.getExecutorNameByIpPath(path);
			namespaceShardingService.asyncShardingWhenExecutorOnline(executorName);
		} else if(isExecutorOffline(type, path)) {
			String executorName = SaturnExecutorsNode.getExecutorNameByIpPath(path);
			namespaceShardingService.asyncShardingWhenExecutorOffline(executorName);
		}
	}

	private boolean isExecutorOnline(TreeCacheEvent.Type type, String path) {
		return type == TreeCacheEvent.Type.NODE_ADDED && path.matches(SaturnExecutorsNode.EXECUTOR_IPNODE_PATH_REGEX);
	}

}
