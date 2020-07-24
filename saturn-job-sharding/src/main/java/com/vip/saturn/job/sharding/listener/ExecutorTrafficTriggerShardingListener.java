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

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;

/**
 * @author hebelala
 */
public class ExecutorTrafficTriggerShardingListener extends AbstractTreeCacheListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorTrafficTriggerShardingListener.class);

	private NamespaceShardingService namespaceShardingService;

	public ExecutorTrafficTriggerShardingListener(NamespaceShardingService namespaceShardingService) {
		this.namespaceShardingService = namespaceShardingService;
	}

	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		try {
			if (isExecutorNoTraffic(type, path)) {
				String executorName = SaturnExecutorsNode.getExecutorNameByNoTrafficPath(path);
				namespaceShardingService.asyncShardingWhenExtractExecutorTraffic(executorName);
			} else if (isExecutorTraffic(type, path)) {
				String executorName = SaturnExecutorsNode.getExecutorNameByNoTrafficPath(path);
				namespaceShardingService.asyncShardingWhenRecoverExecutorTraffic(executorName);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private boolean isExecutorNoTraffic(Type type, String path) {
		return type == Type.NODE_ADDED && path.matches(SaturnExecutorsNode.EXECUTOR_NO_TRAFFIC_NODE_PATH_REGEX);
	}

	public boolean isExecutorTraffic(Type type, String path) {
		return type == Type.NODE_REMOVED && path.matches(SaturnExecutorsNode.EXECUTOR_NO_TRAFFIC_NODE_PATH_REGEX);
	}

}