/**
 * 
 */
package com.vip.saturn.job.sharding.service;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.sharding.NamespaceShardingManager;
import com.vip.saturn.job.sharding.TreeCacheThreadFactory;
import com.vip.saturn.job.sharding.listener.EnabledJobTriggerShardingListener;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;

/**
 * @author chembo.huang
 *
 */
public class EnabledJobService {
	static Logger log = LoggerFactory.getLogger(EnabledJobService.class);

	private CuratorFramework curatorFramework;
	private EnabledJobTriggerShardingListener enabledJobTriggerShardingListener;
	private String namespace;
	private NamespaceShardingManager namespaceShardingManager;

	public EnabledJobService(String namespace, CuratorFramework curatorFramework, NamespaceShardingService namespaceShardingService, NamespaceShardingManager namespaceShardingManager) {
		this.curatorFramework = curatorFramework;
		this.namespace = namespace;
		enabledJobTriggerShardingListener = new EnabledJobTriggerShardingListener(namespaceShardingService);
		this.namespaceShardingManager = namespaceShardingManager;
	}
	
	public void addExistJobEnabledPathListener() throws Exception {
		if (null != curatorFramework.checkExists().forPath(SaturnExecutorsNode.$JOBSNODE_PATH)) {
			List<String> jobs = curatorFramework.getChildren().forPath(SaturnExecutorsNode.$JOBSNODE_PATH);
			log.info("namespaceSharding: addExistJobEnabledPathListener, jobs = {}", jobs);
			if (jobs != null && jobs.isEmpty()) {
				for (String job:jobs) {
					addOneJobEnabledPathListener(job);
				}
			}
		}
	}

	public void addOneJobEnabledPathListener(String job) throws Exception {
		String configPath = SaturnExecutorsNode.$JOBSNODE_PATH + "/" + job + "/config";
		String fullConfigPath = namespace + configPath;
		TreeCache treeCache;
		if (!namespaceShardingManager.TREE_CACHE_MAP.containsKey(fullConfigPath)) {
			int waitConfigPathCreatedCounts = 50;
			while (waitConfigPathCreatedCounts-- !=0 ) {
				if (null != curatorFramework.checkExists().forPath(configPath)) {
					break;
				}
				if (waitConfigPathCreatedCounts == 0) {
					log.error("namespaceSharding: add listener to {} failed!!!!" , fullConfigPath);
					throw new IllegalStateException("path = " + fullConfigPath + " not exists.");
				}
				Thread.sleep(100);
			}
			treeCache = TreeCache.newBuilder(curatorFramework, configPath).setMaxDepth(1)
					.setExecutor(new TreeCacheThreadFactory(namespace + "-" + job + "-config")).build();
			namespaceShardingManager.TREE_CACHE_MAP.putIfAbsent(fullConfigPath, treeCache);
			treeCache.getListenable().addListener(enabledJobTriggerShardingListener);
			treeCache.start();
			log.info("namespaceSharding: listen to {}, depth = {} started.", configPath, 1);
		} else {
			treeCache = namespaceShardingManager.TREE_CACHE_MAP.get(fullConfigPath);
			treeCache.getListenable().addListener(enabledJobTriggerShardingListener);
		}
	}
}
