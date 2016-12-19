package com.vip.saturn.job.sharding.service;

import com.vip.saturn.job.sharding.TreeCacheThreadFactory;
import com.vip.saturn.job.sharding.entity.ShardingTreeCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.CloseableExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author hebelala
 */
public class ShardingTreeCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ShardingTreeCacheService.class);

    private String namespace;
    private CuratorFramework curatorFramework;
    private ShardingTreeCache shardingTreeCache;
    private ExecutorService executorService;

    public ShardingTreeCacheService(String namespace, CuratorFramework curatorFramework) {
        this.namespace = namespace;
        this.curatorFramework = curatorFramework;
        this.shardingTreeCache = new ShardingTreeCache();
    }

    public void addTreeCacheIfAbsent(String path, int depth) {
        try {
            String fullPath = namespace + path;
            if (!shardingTreeCache.containsTreeCache(path, depth)) {
                TreeCache treeCache = TreeCache.newBuilder(curatorFramework, path)
                        .setExecutor(new CloseableExecutorService(executorService, false))
                        .setMaxDepth(depth).build();
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

    public void addTreeCacheListenerIfAbsent(String path, int depth, TreeCacheListener treeCacheListener) {
        String fullPath = namespace + path;
        TreeCacheListener treeCacheListenerOld = shardingTreeCache.addTreeCacheListenerIfAbsent(path, depth, treeCacheListener);
        if (treeCacheListenerOld == null) {
            logger.info("add {}, full path is {}, depth is {}", treeCacheListener.getClass().getSimpleName(), fullPath, depth);
        }
    }

    public void removeTreeCache(String path, int depth) {
        shardingTreeCache.removeTreeCache(path, depth);
    }

    public ExecutorService newSingleThreadExecutor() {
        return Executors.newSingleThreadExecutor(new TreeCacheThreadFactory(namespace));
    }

    public void start() {
        executorService = newSingleThreadExecutor();
    }

    public void shutdown() {
        shardingTreeCache.shutdown();
        if(executorService != null) {
            executorService.shutdownNow();
        }
    }

}
