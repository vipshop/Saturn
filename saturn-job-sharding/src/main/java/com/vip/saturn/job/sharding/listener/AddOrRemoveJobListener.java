package com.vip.saturn.job.sharding.listener;

import com.vip.saturn.job.sharding.NamespaceShardingManager;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.AddJobListenersService;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author chembo.huang
 *
 */
public class AddOrRemoveJobListener extends AbstractTreeCacheListener {
	static Logger log = LoggerFactory.getLogger(AddOrRemoveJobListener.class);

	private AddJobListenersService addJobListenersService;
	private String namespace;
	private NamespaceShardingManager namespaceShardingManager;
	
	public AddOrRemoveJobListener(String namespace, AddJobListenersService addJobListenersService, NamespaceShardingManager namespaceShardingManager) {
		this.addJobListenersService = addJobListenersService;
		this.namespace = namespace;
		this.namespaceShardingManager = namespaceShardingManager;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		try {
			String job = StringUtils.substringAfterLast(path, "/");
			if (!SaturnExecutorsNode.$JOBS.equals(job)) {
				if (isAddJob(type)) {
					log.info("job: {} created, now try to add listeners to its config and servers path.", job);
					addJobListenersService.addJobConfigPathListener(job);
					addJobListenersService.addJobServersPathListener(job);
				} else if (isRemoveJob(type)) {
					namespaceShardingManager.cleanTreeCache(namespace + path + "/config");
					namespaceShardingManager.cleanTreeCache(namespace + path + "/servers");
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw e;
		}
	}

	private boolean isAddJob(TreeCacheEvent.Type type) {
		return type == TreeCacheEvent.Type.NODE_ADDED ;
	}

	private boolean isRemoveJob(TreeCacheEvent.Type type) {
		return type == TreeCacheEvent.Type.NODE_REMOVED ;
	}
}
