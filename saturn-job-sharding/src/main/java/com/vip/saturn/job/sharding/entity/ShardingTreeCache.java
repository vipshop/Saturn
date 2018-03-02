package com.vip.saturn.job.sharding.entity;

import com.google.common.collect.Lists;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author hebelala
 */
public class ShardingTreeCache {

	private static final Logger logger = LoggerFactory.getLogger(ShardingTreeCache.class);

	private Map<String, TreeCache> treeCacheMap = new HashMap<String, TreeCache>();
	private Map<TreeCache, List<TreeCacheListener>> treeCacheListenerMap = new HashMap<TreeCache, List<TreeCacheListener>>();

	private String getKey(String path, int depth) {
		return path + depth;
	}

	public boolean containsTreeCache(String path, int depth) {
		synchronized (this) {
			return treeCacheMap.containsKey(getKey(path, depth));
		}
	}

	public List<String> getTreeCachePaths() {
		return Lists.newArrayList(treeCacheMap.keySet());
	}

	public void putTreeCache(String path, int depth, TreeCache treeCache) {
		synchronized (this) {
			treeCacheMap.put(getKey(path, depth), treeCache);
			treeCacheListenerMap.put(treeCache, new ArrayList<TreeCacheListener>());
		}
	}

	public TreeCache putTreeCacheIfAbsent(String path, int depth, TreeCache treeCache) {
		synchronized (this) {
			String key = getKey(path, depth);
			if (!treeCacheMap.containsKey(key)) {
				treeCacheMap.put(key, treeCache);
				treeCacheListenerMap.put(treeCache, new ArrayList<TreeCacheListener>());
				return null;
			} else {
				return treeCacheMap.get(key);
			}
		}
	}

	public TreeCacheListener addTreeCacheListenerIfAbsent(String path, int depth, TreeCacheListener treeCacheListener) {
		synchronized (this) {
			TreeCacheListener treeCacheListenerOld = null;
			String key = getKey(path, depth);
			TreeCache treeCache = treeCacheMap.get(key);
			if (treeCache == null) {
				logger.error("The TreeCache is not exists, cannot add TreeCacheListener, path is {}, depth is {}", path,
						depth);
			} else {
				List<TreeCacheListener> treeCacheListeners = treeCacheListenerMap.get(treeCache);
				boolean included = false;
				for (TreeCacheListener tmp : treeCacheListeners) {
					Class<? extends TreeCacheListener> tmpClass = tmp.getClass();
					Class<? extends TreeCacheListener> treeCacheListenerClass = treeCacheListener.getClass();
					if (tmpClass.equals(treeCacheListenerClass)) {
						treeCacheListenerOld = tmp;
						included = true;
						break;
					}
				}
				if (included) {
					logger.info(
							"The TreeCache has already included the instance of listener, will not be added, path is {}, depth is {}, listener is {}",
							path, depth, treeCacheListener.getClass());
				} else {
					treeCacheListeners.add(treeCacheListener);
					treeCache.getListenable().addListener(treeCacheListener);
				}
			}
			return treeCacheListenerOld;
		}
	}

	public void removeTreeCache(String path, int depth) {
		String key = getKey(path, depth);
		removeTreeCacheByKey(key);
	}

	public void removeTreeCacheByKey(String key) {
		synchronized (this) {
			TreeCache treeCache = treeCacheMap.get(key);
			if (treeCache != null) {
				treeCacheListenerMap.remove(treeCache);
				treeCacheMap.remove(key);
				treeCache.close();
				logger.info("remove TreeCache success, path+depth is {}", key);
			}
		}
	}

	public void shutdown() {
		synchronized (this) {
			Iterator<Map.Entry<String, TreeCache>> iterator = treeCacheMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, TreeCache> next = iterator.next();
				try {
					next.getValue().close();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				iterator.remove();
			}
			treeCacheListenerMap.clear();
		}
	}

}
