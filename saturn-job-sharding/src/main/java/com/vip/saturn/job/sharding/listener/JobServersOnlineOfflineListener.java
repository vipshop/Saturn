package com.vip.saturn.job.sharding.listener;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import com.vip.saturn.job.sharding.service.ShardingTreeCacheService;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_ADDED;


/**
 * Job Server上下线Listener，会检测/status节点的添加和删除行为。
 */
public class JobServersOnlineOfflineListener extends AbstractTreeCacheListener {

	private static final String NODE_STATUS = "/status";

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
		if (isJobServerAdded(type, path)) {
			String statusPath = path + NODE_STATUS;
			shardingTreeCacheService.addTreeCacheIfAbsent(statusPath, 0);
			shardingTreeCacheService.addTreeCacheListenerIfAbsent(statusPath, 0,
					new JobServersTriggerShardingListener(jobName, namespaceShardingService));
		}
	}

	private boolean isJobServerAdded(Type type, String path) {
		return type == NODE_ADDED && !path.equals(jobServersNodePath);
	}
}
