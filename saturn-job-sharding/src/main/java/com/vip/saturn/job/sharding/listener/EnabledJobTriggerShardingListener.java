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
public class EnabledJobTriggerShardingListener extends AbstractTreeCacheListener {

	private NamespaceShardingService namespaceShardingService;
	
	public EnabledJobTriggerShardingListener(NamespaceShardingService namespaceShardingService) {
		this.namespaceShardingService = namespaceShardingService;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		if(isJobEnabledPath(type, path)) {
			String jobName = SaturnExecutorsNode.getJobNameByConfigEnabledPath(path);
			if(Boolean.valueOf(nodeData)) {
				namespaceShardingService.asyncShardingWhenJobEnable(jobName);
			} else {
				namespaceShardingService.asyncShardingWhenJobDisable(jobName);
			}
		}
	}

	private boolean isJobEnabledPath(TreeCacheEvent.Type type, String path) {
		return type == TreeCacheEvent.Type.NODE_UPDATED && path.matches(SaturnExecutorsNode.JOBCONFIG_ENABLE_NODE_PATH_REGEX);
	}
}
