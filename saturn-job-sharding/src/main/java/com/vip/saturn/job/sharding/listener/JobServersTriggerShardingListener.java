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
            String executorName = SaturnExecutorsNode.getJobServersExecutorNameByStatusPath(path);
            switch (type) {
                case NODE_ADDED:
                    namespaceShardingService.asyncShardingWhenJobServerOnline(jobName, executorName);
                    break;
                case NODE_REMOVED:
                    namespaceShardingService.asyncShardingWhenJobServerOffline(jobName, executorName);
                    break;
            }
        }
    }

    private boolean isJobServerStatus(String path) {
        return path.matches(SaturnExecutorsNode.getJobServersExecutorStatusNodePathRegex(jobName));
    }

}
