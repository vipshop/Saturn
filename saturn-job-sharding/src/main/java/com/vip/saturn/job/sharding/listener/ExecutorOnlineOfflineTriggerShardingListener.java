/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.sharding.listener;

import com.vip.saturn.job.sharding.service.ExecutorCleanService;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author chembo.huang
 *
 */
public class ExecutorOnlineOfflineTriggerShardingListener extends AbstractTreeCacheListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorOnlineOfflineTriggerShardingListener.class);

	private NamespaceShardingService namespaceShardingService;
	private ExecutorCleanService executorCleanService;

	public ExecutorOnlineOfflineTriggerShardingListener(NamespaceShardingService namespaceShardingService,
			ExecutorCleanService executorCleanService) {
		this.namespaceShardingService = namespaceShardingService;
		this.executorCleanService = executorCleanService;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		if (isExecutorOnline(type, path)) {
			String executorName = SaturnExecutorsNode.getExecutorNameByIpPath(path);
			namespaceShardingService.asyncShardingWhenExecutorOnline(executorName, nodeData);
		} else if (isExecutorOffline(type, path)) {
			String executorName = SaturnExecutorsNode.getExecutorNameByIpPath(path);
			try {
				executorCleanService.clean(executorName);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
			try {
				namespaceShardingService.asyncShardingWhenExecutorOffline(executorName);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	private boolean isExecutorOnline(TreeCacheEvent.Type type, String path) {
		return type == TreeCacheEvent.Type.NODE_ADDED && path.matches(SaturnExecutorsNode.EXECUTOR_IPNODE_PATH_REGEX);
	}

	public boolean isExecutorOffline(TreeCacheEvent.Type type, String path) {
		return type == TreeCacheEvent.Type.NODE_REMOVED && path.matches(SaturnExecutorsNode.EXECUTOR_IPNODE_PATH_REGEX);
	}

}
