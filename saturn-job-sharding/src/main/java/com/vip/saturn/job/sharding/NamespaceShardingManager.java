package com.vip.saturn.job.sharding;

import com.vip.saturn.job.sharding.listener.AddOrRemoveJobListener;
import com.vip.saturn.job.sharding.listener.ExecutorCleanListener;
import com.vip.saturn.job.sharding.listener.ExecutorOnlineOfflineTriggerShardingListener;
import com.vip.saturn.job.sharding.listener.LeadershipElectionListener;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.AddJobListenersService;
import com.vip.saturn.job.sharding.service.ExecutorCleanService;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class NamespaceShardingManager {
	static Logger log = LoggerFactory.getLogger(NamespaceShardingManager.class);

	private NamespaceShardingService namespaceShardingService;
	private ExecutorCleanService executorCleanService;
	public ConcurrentMap<String/*path*/, TreeCache> TREE_CACHE_MAP = new ConcurrentHashMap<>();
	private CuratorFramework curatorFramework;
	private AddJobListenersService addJobListenersService;
	private String namespace;
	
	public NamespaceShardingManager(CuratorFramework curatorFramework, String namespace, String hostValue) {
		this.namespaceShardingService = new NamespaceShardingService(curatorFramework, hostValue);
		this.executorCleanService = new ExecutorCleanService(curatorFramework);
		this.addJobListenersService = new AddJobListenersService(namespace, curatorFramework, namespaceShardingService, this);
		this.curatorFramework = curatorFramework;
		this.namespace = namespace;
	}

	/**
	 * leadership election, add listeners
	 */
	public void start() throws Exception {
		// create ephemeral node $SaturnExecutors/leader/host & $Jobs.
		namespaceShardingService.leaderElection();
		addJobListenersService.addExistJobConfigPathListener();
		addOnlineOfflineListener();
		addExecutorCleannerListener();
		addLeaderElectionListener();
		addNewOrRemoveJobListener();
	}
	
	
	private void addNewOrRemoveJobListener() throws Exception {
		TreeCache treeCache;
		String path = namespace + SaturnExecutorsNode.$JOBSNODE_PATH;
		if (!TREE_CACHE_MAP.containsKey(path)) {
			treeCache = TreeCache.newBuilder(curatorFramework, SaturnExecutorsNode.$JOBSNODE_PATH)
					.setExecutor(new TreeCacheThreadFactory(namespace + "-$jobs")).setMaxDepth(1).build();
			TREE_CACHE_MAP.putIfAbsent(path, treeCache);
			treeCache.start();
			treeCache.getListenable().addListener(new AddOrRemoveJobListener(namespace, addJobListenersService, this));
			log.info("namespaceSharding: listen to {}, depth = {} started.", SaturnExecutorsNode.$JOBSNODE_PATH, 1);
		} else {
			treeCache = TREE_CACHE_MAP.get(path);
			treeCache.getListenable().addListener(new AddOrRemoveJobListener(namespace, addJobListenersService, this));
		}
	}


	/**
	 *  watch 2-level-depth of the children of /$SaturnExecutors/executors
	 */
	private void addOnlineOfflineListener() throws Exception {
		TreeCache treeCache;
		String path = namespace + SaturnExecutorsNode.EXECUTORSNODE_PATH;
		if (!TREE_CACHE_MAP.containsKey(path)) {
			treeCache = TreeCache.newBuilder(curatorFramework, SaturnExecutorsNode.EXECUTORSNODE_PATH)
					.setExecutor(new TreeCacheThreadFactory(namespace + "-$SaturnExecutors-executors")).setMaxDepth(2)
					.build();
			TREE_CACHE_MAP.putIfAbsent(path, treeCache);
			treeCache.getListenable().addListener(new ExecutorOnlineOfflineTriggerShardingListener(namespaceShardingService));
			treeCache.start();
			log.info("namespaceSharding: listen to {}, depth = {} started.", SaturnExecutorsNode.EXECUTORSNODE_PATH, 2);
		} else {
			treeCache = TREE_CACHE_MAP.get(path);
			treeCache.getListenable().addListener(new ExecutorOnlineOfflineTriggerShardingListener(namespaceShardingService));
		}
	}


	/**
	 *  watch 2-level-depth of the children of /$SaturnExecutors/executors
	 */
	private void addExecutorCleannerListener() throws Exception {
		TreeCache treeCache;
		String path = namespace + SaturnExecutorsNode.EXECUTORSNODE_PATH;
		if (!TREE_CACHE_MAP.containsKey(path)) {
			treeCache = TreeCache.newBuilder(curatorFramework, SaturnExecutorsNode.EXECUTORSNODE_PATH)
					.setExecutor(new TreeCacheThreadFactory(namespace + "-$SaturnExecutors-executors")).setMaxDepth(2)
					.build();
			TREE_CACHE_MAP.putIfAbsent(path, treeCache);
			treeCache.getListenable().addListener(new ExecutorCleanListener(executorCleanService));
			treeCache.start();
			log.info("namespaceSharding: listen to {}, depth = {} started.", SaturnExecutorsNode.EXECUTORSNODE_PATH, 2);
		} else {
			treeCache = TREE_CACHE_MAP.get(path);
			treeCache.getListenable().addListener(new ExecutorCleanListener(executorCleanService));
		}
	}

	/**
	 * watch 1-level-depth of the children of /$SaturnExecutors/leader  
	 */
	private void addLeaderElectionListener() throws Exception {
		TreeCache treeCache;
		String path = namespace + SaturnExecutorsNode.LEADERNODE_PATH;
		if (!TREE_CACHE_MAP.containsKey(path)) {
			treeCache = TreeCache.newBuilder(curatorFramework, SaturnExecutorsNode.LEADERNODE_PATH)
					.setExecutor(new TreeCacheThreadFactory(namespace + "-$SaturnExecutors-leader")).setMaxDepth(1)
					.build();
			TREE_CACHE_MAP.putIfAbsent(path, treeCache);
			treeCache.getListenable().addListener(new LeadershipElectionListener(namespaceShardingService));
			treeCache.start();
			log.info("namespaceSharding: listen to {}, depth = {} started.", SaturnExecutorsNode.LEADERNODE_PATH, 1);
		} else {
			treeCache = TREE_CACHE_MAP.get(path);
			treeCache.getListenable().addListener(new LeadershipElectionListener(namespaceShardingService));
		}
	}


	/**
	 * close listeners, delete leadership, close curatorFramework
	 */
	public void stop() {
		namespaceShardingService.shutdown();
		Collection<TreeCache> values = TREE_CACHE_MAP.values();
		for (TreeCache tc:values) {
			tc.close();
		}
		TREE_CACHE_MAP.clear();
		curatorFramework.close();
	}
	
	public void cleanTreeCache(String path) {
		TreeCache treeCache = TREE_CACHE_MAP.get(path);
		if (treeCache != null) {
			treeCache.close();
			TREE_CACHE_MAP.remove(path);
		}
	}
	
}
