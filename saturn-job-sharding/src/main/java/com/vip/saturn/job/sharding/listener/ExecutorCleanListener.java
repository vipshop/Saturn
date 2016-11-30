package com.vip.saturn.job.sharding.listener;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.ExecutorCleanService;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class ExecutorCleanListener extends AbstractTreeCacheListener {

	private ExecutorCleanService executorCleanService;

	public ExecutorCleanListener(ExecutorCleanService executorCleanService) {
		this.executorCleanService = executorCleanService;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		if(isExecutorOffline(type, path)) {
			String executorName = SaturnExecutorsNode.getExecutorNameByIpPath(path);
			executorCleanService.clean(executorName);
		}
	}
	
}
