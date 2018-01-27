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
public class JobConfigTriggerShardingListener extends AbstractTreeCacheListener {

	private String jobName;
	private NamespaceShardingService namespaceShardingService;
	private String enabledPath;
	private String forceShardPath;

	public JobConfigTriggerShardingListener(String jobName, NamespaceShardingService namespaceShardingService) {
		this.jobName = jobName;
		this.namespaceShardingService = namespaceShardingService;
		this.enabledPath = SaturnExecutorsNode.getJobConfigEnableNodePath(jobName);
		this.forceShardPath = SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName);
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		if (isJobEnabledPath(type, path)) {
			if (Boolean.parseBoolean(nodeData)) {
				namespaceShardingService.asyncShardingWhenJobEnable(jobName);
			} else {
				namespaceShardingService.asyncShardingWhenJobDisable(jobName);
			}
		} else if (isForceShardJob(type, path)) {
			namespaceShardingService.asyncShardingWhenJobForceShard(jobName);
		}
	}

	private boolean isJobEnabledPath(TreeCacheEvent.Type type, String path) {
		return type == TreeCacheEvent.Type.NODE_UPDATED && path.equals(enabledPath);
	}

	private boolean isForceShardJob(TreeCacheEvent.Type type, String path) {
		return type == TreeCacheEvent.Type.NODE_ADDED && path.equals(forceShardPath);
	}
}
