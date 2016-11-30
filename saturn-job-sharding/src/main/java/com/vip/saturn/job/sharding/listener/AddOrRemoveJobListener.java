package com.vip.saturn.job.sharding.listener;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.sharding.NamespaceShardingManager;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.EnabledJobService;

/**
 * 
 * @author chembo.huang
 *
 */
public class AddOrRemoveJobListener extends AbstractTreeCacheListener {
	static Logger log = LoggerFactory.getLogger(AddOrRemoveJobListener.class);

	private EnabledJobService enabledJobService;
	private String namespace;
	private NamespaceShardingManager namespaceShardingManager;
	
	public AddOrRemoveJobListener(String namespace, EnabledJobService enabledJobService, NamespaceShardingManager namespaceShardingManager) {
		this.enabledJobService = enabledJobService;
		this.namespace = namespace;
		this.namespaceShardingManager = namespaceShardingManager;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		if(isAddJob(type)) {
			String job = StringUtils.substringAfterLast(path, "/");
			if (!SaturnExecutorsNode.$JOBS.equals(job)) {
				log.info("job: {} created, now try to add listener to its config path.", job);
				enabledJobService.addOneJobEnabledPathListener(job);
			}
		} else if (isRemoveJob(type)) {
			namespaceShardingManager.cleanTreeCache(namespace + path + "/config");
		}
	}

	private boolean isAddJob(TreeCacheEvent.Type type) {
		return type == TreeCacheEvent.Type.NODE_ADDED ;
	}

	private boolean isRemoveJob(TreeCacheEvent.Type type) {
		return type == TreeCacheEvent.Type.NODE_REMOVED ;
	}
}
