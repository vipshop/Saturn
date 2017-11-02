package com.vip.saturn.job.sharding.listener;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;

/**
 * @author hebelala
 */
public class LeadershipElectionListener extends AbstractTreeCacheListener {

	private NamespaceShardingService namespaceShardingService;

	public LeadershipElectionListener(NamespaceShardingService namespaceShardingService) {
		this.namespaceShardingService = namespaceShardingService;
	}

	@Override
	public void childEvent(TreeCacheEvent.Type type, String path, String nodeData) throws Exception {
		if (isLeaderRemove(type, path)) {
			namespaceShardingService.leaderElection();
		}
	}

	private boolean isLeaderRemove(TreeCacheEvent.Type type, String path) {
		return type == TreeCacheEvent.Type.NODE_REMOVED && SaturnExecutorsNode.LEADER_HOSTNODE_PATH.equals(path);
	}
}
