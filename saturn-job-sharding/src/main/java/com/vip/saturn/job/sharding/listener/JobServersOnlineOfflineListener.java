package com.vip.saturn.job.sharding.listener;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import com.vip.saturn.job.sharding.service.ShardingTreeCacheService;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_ADDED;
import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_REMOVED;


/**
 * Job Server上下线Listener，会检测/status节点的添加和删除行为。
 */
public class JobServersOnlineOfflineListener extends AbstractTreeCacheListener {

	private static final String NODE_STATUS = "/status";

	private static final int TREE_CACHE_DEPTH = 0;

	private String jobName;

	private String jobServersNodePath;

	private ShardingTreeCacheService shardingTreeCacheService;

	private NamespaceShardingService namespaceShardingService;

	public JobServersOnlineOfflineListener(String jobName, ShardingTreeCacheService shardingTreeCacheService,
			NamespaceShardingService namespaceShardingService) {
		this.jobName = jobName;
		this.jobServersNodePath = SaturnExecutorsNode.getJobServersNodePath(jobName);
		this.shardingTreeCacheService = shardingTreeCacheService;
		this.namespaceShardingService = namespaceShardingService;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		if (path.equals(jobServersNodePath)) {
			return;
		}

		String statusPath = path + NODE_STATUS;
		if (type == NODE_ADDED) {
			shardingTreeCacheService.addTreeCacheIfAbsent(statusPath, TREE_CACHE_DEPTH);
			shardingTreeCacheService.addTreeCacheListenerIfAbsent(statusPath, TREE_CACHE_DEPTH,
					new JobServersTriggerShardingListener(jobName, namespaceShardingService));
		} else if (type == NODE_REMOVED) { // 保证只watch新server clean事件
			shardingTreeCacheService.removeTreeCache(statusPath, TREE_CACHE_DEPTH);
		}
	}


}
