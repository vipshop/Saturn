package com.vip.saturn.job.sharding.listener;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;

/**
 * @author hebelala
 */
public class ForceShardJobTriggerShardingListener extends AbstractTreeCacheListener {

    private NamespaceShardingService namespaceShardingService;

    public ForceShardJobTriggerShardingListener(NamespaceShardingService namespaceShardingService) {
        this.namespaceShardingService = namespaceShardingService;
    }

    @Override
    public void childEvent(TreeCacheEvent.Type type, String path, String nodeData) throws Exception {
        if (isForceShardJob(type, path)) {
            String jobName = SaturnExecutorsNode.getJobNameByConfigForceShardPath(path);
            namespaceShardingService.asyncShardingWhenJobForceShard(jobName);
        }
    }

    private boolean isForceShardJob(TreeCacheEvent.Type type, String path) {
        return type == TreeCacheEvent.Type.NODE_ADDED && path.matches(SaturnExecutorsNode.JOBCONFIG_FORCESHARD_NODE_PATH_REGEX);
    }

}
