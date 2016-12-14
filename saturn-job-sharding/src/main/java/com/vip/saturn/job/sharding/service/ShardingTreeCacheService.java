package com.vip.saturn.job.sharding.service;

import com.vip.saturn.job.sharding.TreeCacheThreadFactory;
import com.vip.saturn.job.sharding.entity.ShardingTreeCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public class ShardingTreeCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ShardingTreeCacheService.class);

    private String namespace;
    private CuratorFramework curatorFramework;
    private ShardingTreeCache shardingTreeCache = new ShardingTreeCache();

    public ShardingTreeCacheService(String namespace, CuratorFramework curatorFramework) {
        this.namespace = namespace;
        this.curatorFramework = curatorFramework;
    }

    public void addTreeCache(String path, int depth) {
        try {
            String fullPath = namespace + path;
            if (!shardingTreeCache.containsTreeCache(path, depth)) {
                TreeCache treeCache = TreeCache.newBuilder(curatorFramework, path)
                        .setExecutor(new TreeCacheThreadFactory(fullPath)).setMaxDepth(depth).build();
                treeCache.start();
                TreeCache treeCacheOld = shardingTreeCache.putTreeCacheIfAbsent(path, depth, treeCache);
                if (treeCacheOld != null) {
                    treeCache.close();
                } else {
                    logger.info("create TreeCache, full path is {}, depth is {}", fullPath, depth);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void addTreeCacheListener(String path, int depth, TreeCacheListener treeCacheListener) {
        String fullPath = namespace + path;
        TreeCacheListener treeCacheListenerOld = shardingTreeCache.addTreeCacheListenerIfAbsent(path, depth, treeCacheListener);
        if (treeCacheListenerOld == null) {
            logger.info("add {}, full path is {}, depth is {}", treeCacheListener.getClass().getSimpleName(), fullPath, depth);
        }
    }

    public void removeTreeCache(String path, int depth) {
        shardingTreeCache.removeTreeCache(path, depth);
    }

    public void shutdown() {
        shardingTreeCache.shutdown();
    }

}
