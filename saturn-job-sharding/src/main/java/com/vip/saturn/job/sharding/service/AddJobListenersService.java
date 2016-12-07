/**
 *
 */
package com.vip.saturn.job.sharding.service;

import com.vip.saturn.job.sharding.NamespaceShardingManager;
import com.vip.saturn.job.sharding.TreeCacheThreadFactory;
import com.vip.saturn.job.sharding.listener.EnabledJobTriggerShardingListener;
import com.vip.saturn.job.sharding.listener.ForceShardJobTriggerShardingListener;
import com.vip.saturn.job.sharding.listener.JobServersTriggerShardingListener;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author chembo.huang
 */
public class AddJobListenersService {
	static Logger log = LoggerFactory.getLogger(AddJobListenersService.class);

	private CuratorFramework curatorFramework;
	private NamespaceShardingService namespaceShardingService;
	private EnabledJobTriggerShardingListener enabledJobTriggerShardingListener;
	private ForceShardJobTriggerShardingListener forceShardJobTriggerShardingListener;
	private String namespace;
	private NamespaceShardingManager namespaceShardingManager;

	public AddJobListenersService(String namespace, CuratorFramework curatorFramework, NamespaceShardingService namespaceShardingService, NamespaceShardingManager namespaceShardingManager) {
		this.curatorFramework = curatorFramework;
		this.namespaceShardingService = namespaceShardingService;
		this.namespace = namespace;
		enabledJobTriggerShardingListener = new EnabledJobTriggerShardingListener(namespaceShardingService);
		forceShardJobTriggerShardingListener = new ForceShardJobTriggerShardingListener(namespaceShardingService);
		this.namespaceShardingManager = namespaceShardingManager;
	}

	public void addExistJobConfigPathListener() throws Exception {
		if (null != curatorFramework.checkExists().forPath(SaturnExecutorsNode.$JOBSNODE_PATH)) {
			List<String> jobs = curatorFramework.getChildren().forPath(SaturnExecutorsNode.$JOBSNODE_PATH);
			log.info("namespaceSharding: addExistJobConfigPathListener, jobs = {}", jobs);
			if (jobs != null) {
				for (String job : jobs) {
					addJobConfigPathListener(job);
					addJobServersPathListener(job);
				}
			}
		}
	}

	public void addJobConfigPathListener(String jobName) throws Exception {
		String configPath = SaturnExecutorsNode.$JOBSNODE_PATH + "/" + jobName + "/config";
		String fullConfigPath = namespace + configPath;
		TreeCache treeCache;
		if (!namespaceShardingManager.TREE_CACHE_MAP.containsKey(fullConfigPath)) {
			int waitConfigPathCreatedCounts = 50;
			while (waitConfigPathCreatedCounts-- != 0) {
				if (null != curatorFramework.checkExists().forPath(configPath)) {
					break;
				}
				if (waitConfigPathCreatedCounts == 0) {
					log.error("namespaceSharding: add listener to {} failed!!!!", fullConfigPath);
					throw new IllegalStateException("path = " + fullConfigPath + " not exists.");
				}
				Thread.sleep(100);
			}
			treeCache = TreeCache.newBuilder(curatorFramework, configPath).setMaxDepth(1)
					.setExecutor(new TreeCacheThreadFactory(namespace + "-" + jobName + "-config")).build();
			namespaceShardingManager.TREE_CACHE_MAP.putIfAbsent(fullConfigPath, treeCache);
			treeCache.start();
			log.info("namespaceSharding: listen to {}, depth = {} started.", configPath, 1);
		} else {
			treeCache = namespaceShardingManager.TREE_CACHE_MAP.get(fullConfigPath);
		}
		treeCache.getListenable().addListener(enabledJobTriggerShardingListener);
		treeCache.getListenable().addListener(forceShardJobTriggerShardingListener);
	}

	public void addJobServersPathListener(String jobName) throws Exception {
		String path = SaturnExecutorsNode.$JOBSNODE_PATH + "/" + jobName + "/servers";
		try {
			// create servers if not exists
			if (curatorFramework.checkExists().forPath(path) == null) {
				curatorFramework.create().forPath(path);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		String fullPath = namespace + path;
		int depth = 2;
		TreeCache treeCache = null;
		if (!namespaceShardingManager.TREE_CACHE_MAP.containsKey(fullPath)) {
			treeCache = TreeCache.newBuilder(curatorFramework, path).setMaxDepth(depth)
					.setExecutor(new TreeCacheThreadFactory(namespace + "-" + jobName + "-servers")).build();
			namespaceShardingManager.TREE_CACHE_MAP.putIfAbsent(fullPath, treeCache);
			treeCache.start();
			log.info("namespaceSharding: listen to {}, depth = {} started.", path, depth);
		} else {
			treeCache = namespaceShardingManager.TREE_CACHE_MAP.get(fullPath);
		}
		treeCache.getListenable().addListener(new JobServersTriggerShardingListener(jobName, namespaceShardingService));
	}
}
