package com.vip.saturn.job.sharding.listener;

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
	
	public AddOrRemoveJobListener(AddJobListenersService addJobListenersService) {
		this.addJobListenersService = addJobListenersService;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		try {
			String job = StringUtils.substringAfterLast(path, "/");
			if (!SaturnExecutorsNode.$JOBS.equals(job)) {
				if (isAddJob(type)) {
					log.info("job: {} created", job);
					addJobListenersService.addJobPathListener(job);
				} else if (isRemoveJob(type)) {
					log.info("job: {} removed", job);
					addJobListenersService.removeJobPathTreeCache(job);
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
