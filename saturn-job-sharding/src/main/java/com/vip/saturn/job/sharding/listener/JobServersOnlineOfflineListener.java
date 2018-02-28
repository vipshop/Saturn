package com.vip.saturn.job.sharding.listener;

import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_ADDED;
import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_REMOVED;

import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import com.vip.saturn.job.sharding.service.ShardingTreeCacheService;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

/**
 * Job Server上下线Listener，会检测/status节点的添加和删除行为。
 */
public class JobServersOnlineOfflineListener extends AbstractTreeCacheListener {

	private static final String NODE_STATUS = "/status";

	private String jobName;

	private ShardingTreeCacheService shardingTreeCacheService;

	private NamespaceShardingService namespaceShardingService;

	public JobServersOnlineOfflineListener(String jobName,
			ShardingTreeCacheService shardingTreeCacheService,
			NamespaceShardingService namespaceShardingService) {
		this.jobName = jobName;
		this.shardingTreeCacheService = shardingTreeCacheService;
		this.namespaceShardingService = namespaceShardingService;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		int depth = 0;
		if (type == NODE_ADDED) {
			shardingTreeCacheService.addTreeCacheIfAbsent(path + NODE_STATUS, depth);
			shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth,
					new JobServersTriggerShardingListener(jobName, namespaceShardingService));
		} else if (type == NODE_REMOVED) {
			shardingTreeCacheService.removeTreeCache(path + NODE_STATUS, depth);
		}
	}
}
