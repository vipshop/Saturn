package com.vip.saturn.job.sharding.service;

import com.vip.saturn.job.sharding.TreeCacheThreadFactory;
import com.vip.saturn.job.sharding.entity.ShardingTreeCache;
import com.vip.saturn.job.sharding.exception.ShardingException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.CloseableExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hebelala
 */
public class ShardingTreeCacheService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ShardingTreeCacheService.class);

	private String namespace;
	private CuratorFramework curatorFramework;
	private ShardingTreeCache shardingTreeCache;
	private ExecutorService executorService;
	private AtomicBoolean isShutdownFlag = new AtomicBoolean(true);

	public ShardingTreeCacheService(String namespace, CuratorFramework curatorFramework) {
		this.namespace = namespace;
		this.curatorFramework = curatorFramework;
		this.shardingTreeCache = new ShardingTreeCache();
	}

	public void addTreeCacheIfAbsent(String path, int depth) throws Exception {
		synchronized (isShutdownFlag) {
			if (isShutdownFlag.get()) {
				throw new ShardingException("ShardingTreeCacheService has been shutdown");
			}

			String fullPath = namespace + path;
			if (!shardingTreeCache.containsTreeCache(path, depth)) {
				TreeCache treeCache = TreeCache.newBuilder(curatorFramework, path)
						.setExecutor(new CloseableExecutorService(executorService, false)).setMaxDepth(depth)
						.build();
				try {
					treeCache.start();
				} catch (Exception e) {
					treeCache.close();
					throw e;
				}
				TreeCache treeCacheOld = shardingTreeCache.putTreeCacheIfAbsent(path, depth, treeCache);
				if (treeCacheOld != null) {
					treeCache.close();
				} else {
					LOGGER.info("create TreeCache, full path is {}, depth is {}", fullPath, depth);
				}
			}
		}
	}

	public void addTreeCacheListenerIfAbsent(String path, int depth, TreeCacheListener treeCacheListener) throws ShardingException {
		synchronized (isShutdownFlag) {
			if (isShutdownFlag.get()) {
				throw new ShardingException("ShardingTreeCacheService has been shutdown");
			}

			String fullPath = namespace + path;
			TreeCacheListener treeCacheListenerOld = shardingTreeCache.addTreeCacheListenerIfAbsent(path, depth,
					treeCacheListener);
			if (treeCacheListenerOld == null) {
				LOGGER.info("add {}, full path is {}, depth is {}", treeCacheListener.getClass().getSimpleName(),
						fullPath, depth);
			}
		}
	}

	public void removeTreeCache(String path, int depth) throws ShardingException {
		synchronized (isShutdownFlag) {
			if (isShutdownFlag.get()) {
				throw new ShardingException("ShardingTreeCacheService has been shutdown");
			}

			shardingTreeCache.removeTreeCache(path, depth);
		}
	}

	public void start() {
		synchronized (isShutdownFlag) {
			if (isShutdownFlag.compareAndSet(true, false)) {
				shutdown0();
				executorService = Executors
						.newSingleThreadExecutor(new TreeCacheThreadFactory("NamespaceSharding-" + namespace));
			} else {
				LOGGER.warn("{}-ShardingTreeCacheService has already started, unnecessary to start", namespace);
			}
		}
	}

	private void shutdown0() {
		shardingTreeCache.shutdown();
		if (executorService != null) {
			executorService.shutdownNow();
		}
	}

	public void shutdown() {
		synchronized (isShutdownFlag) {
			if (isShutdownFlag.compareAndSet(false, true)) {
				shutdown0();
			} else {
				LOGGER.warn("{}-ShardingTreeCacheService has already shutdown, unnecessary to shutdown", namespace);
			}
		}
	}

}
