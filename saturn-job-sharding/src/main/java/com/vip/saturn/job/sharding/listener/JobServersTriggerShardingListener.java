package com.vip.saturn.job.sharding.listener;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public class JobServersTriggerShardingListener extends AbstractTreeCacheListener {

    private static final Logger logger = LoggerFactory.getLogger(JobServersTriggerShardingListener.class);

    private String jobName;
    private NamespaceShardingService namespaceShardingService;

    public JobServersTriggerShardingListener(String jobName, NamespaceShardingService namespaceShardingService) {
        this.jobName = jobName;
        this.namespaceShardingService = namespaceShardingService;
    }

    @Override
    public void childEvent(TreeCacheEvent.Type type, String path, String nodeData) throws Exception {
        if(isJobServerStatus(path)) {
            switch (type) {
                case NODE_ADDED:
                    logger.info("The node {} is added, trigger job force shard", path);
                    namespaceShardingService.asyncShardingWhenJobForceShard(jobName);
                    break;
                case NODE_REMOVED:
                    logger.info("The node {} is removed, trigger job force shard", path);
                    namespaceShardingService.asyncShardingWhenJobForceShard(jobName);
                    break;
            }
        }
    }

    private boolean isJobServerStatus(String path) {
        return path.matches(SaturnExecutorsNode.getJobServersExecutorStatusNodePathRegex(jobName));
    }

}
