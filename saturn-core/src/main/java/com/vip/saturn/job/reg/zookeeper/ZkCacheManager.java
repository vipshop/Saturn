package com.vip.saturn.job.reg.zookeeper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.CloseableExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.threads.SaturnThreadFactory;

/**
 * @author chembo.huang
 */
public class ZkCacheManager {

	static Logger log = LoggerFactory.getLogger(ZkCacheManager.class);
	private Map<String/* path-depth */, TreeCache> treeCacheMap = new HashMap<>();
	private Map<String/* path */, NodeCache> nodeCacheMap = new HashMap<>();
	private CuratorFramework client;
	private String jobName;
	private String executorName;
	private ExecutorService executorService;

	public ZkCacheManager(CuratorFramework client, String jobName, String executorName) {
		this.client = client;
		this.jobName = jobName;
		this.executorName = executorName;
		executorService = Executors
				.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-" + jobName + "-watcher", false));
		log.info("ZkCacheManager for executor:{} - job:{} created.", executorName, jobName);
	}

	public NodeCache buildAndStartNodeCache(String path) {
		try {
			NodeCache nc = nodeCacheMap.get(path);
			if (nc == null) {
				nc = new NodeCache(client, path);
				nodeCacheMap.put(path, nc);
				nc.start();
				log.info("{} - {} builds nodeCache for path = {}", executorName, jobName, path);
			}
			return nc;
		} catch (Exception e) {
			log.error("{} - {}  fails in building nodeCache for path = {}, saturn will not work correctly.",
					executorName, jobName, path);
			log.error(e.getMessage(), e);
		}
		return null;
	}

	private TreeCache buildAndStartTreeCache(String path, int depth) {
		try {
			String key = buildMapKey(path, depth);
			TreeCache tc = treeCacheMap.get(key);
			if (tc == null) {
				tc = TreeCache.newBuilder(client, path).setMaxDepth(depth)
						.setExecutor(new CloseableExecutorService(executorService, false)).build();
				treeCacheMap.put(key, tc);
				tc.start();
				log.info("{} - {}  builds treeCache for path = {}, depth = {}", executorName, jobName, path, depth);
			}
			return tc;
		} catch (Exception e) {
			log.error("{} - {} fails in building treeCache for path = {}, depth = {}, saturn will not work correctly.",
					executorName, jobName, path, depth);
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public void addTreeCacheListener(final TreeCacheListener listener, final String path, final int depth) {
		TreeCache tc = buildAndStartTreeCache(path, depth);
		if (tc != null) {
			tc.getListenable().addListener(listener);
		}
	}

	public void closeTreeCache(String path, int depth) {
		String key = buildMapKey(path, depth);
		TreeCache tc = treeCacheMap.get(key);
		if (tc != null) {
			try {
				tc.close();
				treeCacheMap.remove(key);
				log.info("{} - {} closed treeCache, path and depth is {}", executorName, jobName, key);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public void closeAllTreeCache() {
		Iterator<Entry<String, TreeCache>> iterator = treeCacheMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, TreeCache> next = iterator.next();
			TreeCache tc = next.getValue();
			String path = next.getKey();
			try {
				tc.close();
				iterator.remove();
				log.info("{} - {} closed treeCache, path and depth is {}", executorName, jobName, path);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public void closeNodeCache(String path) {
		NodeCache nc = nodeCacheMap.get(path);
		if (nc != null) {
			try {
				nc.close();
				nodeCacheMap.remove(path);
				log.info("{} - {} closed nodeCache, path is {}", executorName, jobName, path);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public void closeAllNodeCache() {
		Iterator<Entry<String, NodeCache>> iterator = nodeCacheMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, NodeCache> next = iterator.next();
			NodeCache nc = next.getValue();
			String path = next.getKey();
			try {
				nc.close();
				iterator.remove();
				log.info("{} - {} closed nodeCache, path is {}", executorName, jobName, path);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private static String buildMapKey(String path, int depth) {
		return path + "-" + depth;
	}

	public void addNodeCacheListener(final NodeCacheListener listener, final String path) {
		NodeCache nc = buildAndStartNodeCache(path);
		if (nc != null) {
			nc.getListenable().addListener(listener);
		}
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void shutdown() {
		closeAllTreeCache();
		closeAllNodeCache();
		if (executorService != null && !executorService.isShutdown()) {
			// Cannot use shutdownNow, because the current thread maybe is running in the pool, and there are codes
			// after this.
			// Such as JobDeleteListener
			executorService.shutdown();
		}
	}

}
