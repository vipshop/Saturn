/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.internal.failover;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.execution.ExecutionService;
import com.vip.saturn.job.internal.listener.AbstractJobListener;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;
import com.vip.saturn.job.internal.storage.JobNodePath;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 失效转移监听管理器.
 */
public class FailoverListenerManager extends AbstractListenerManager {
	static Logger log = LoggerFactory.getLogger(FailoverListenerManager.class);

	private volatile boolean isShutdown = false;

	private final ConfigurationService configService;

	private final ExecutionService executionService;

	private final FailoverService failoverService;

	private final String executionPath;

	private final Set<String> runningAndFailoverPath;

	public FailoverListenerManager(final JobScheduler jobScheduler) {
		super(jobScheduler);
		configService = jobScheduler.getConfigService();
		executionService = jobScheduler.getExecutionService();
		failoverService = jobScheduler.getFailoverService();
		executionPath = JobNodePath.getNodeFullPath(jobName, ExecutionNode.ROOT);
		runningAndFailoverPath = new HashSet<>();
	}

	@Override
	public void start() {
		zkCacheManager.addTreeCacheListener(new ExecutionPathListener(), executionPath, 1);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		isShutdown = true;
		zkCacheManager.closeTreeCache(executionPath, 1);
		closeRunningAndFailoverNodeCaches();
	}

	private void closeRunningAndFailoverNodeCaches() {
		Iterator<String> iterator = runningAndFailoverPath.iterator();
		while (iterator.hasNext()) {
			String next = iterator.next();
			zkCacheManager.closeNodeCache(next);
		}
	}

	private synchronized void failover(final Integer item) {
		if (jobScheduler == null || jobScheduler.getJob() == null) {
			return;
		}
		if (!jobScheduler.getJob().isFailoverSupported() || !configService.isFailover()
				|| executionService.isCompleted(item)) {
			return;
		}

		failoverService.createCrashedFailoverFlag(item);

		if (!executionService.hasRunningItems(jobScheduler.getShardingService().getLocalHostShardingItems())) {
			failoverService.failoverIfNecessary();
		}
	}

	class ExecutionPathListener extends AbstractJobListener {

		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			try {
				if (isShutdown) {
					return;
				}
				if (executionPath.equals(path)) {
					return;
				}
				int item = getItem(path);
				String runningPath = JobNodePath.getNodeFullPath(jobName, ExecutionNode.getRunningNode(item));
				String failoverPath = JobNodePath.getNodeFullPath(jobName, FailoverNode.getExecutionFailoverNode(item));
				switch (event.getType()) {
				case NODE_ADDED:
					zkCacheManager.addNodeCacheListener(new RunningPathListener(item), runningPath);
					runningAndFailoverPath.add(runningPath);
					zkCacheManager.addNodeCacheListener(new FailoverPathJobListener(item), failoverPath);
					runningAndFailoverPath.add(failoverPath);
					break;
				case NODE_REMOVED:
					zkCacheManager.closeNodeCache(runningPath);
					runningAndFailoverPath.remove(runningPath);
					zkCacheManager.closeNodeCache(failoverPath);
					runningAndFailoverPath.remove(failoverPath);
					break;
				default:
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		private int getItem(String path) {
			return Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
		}

	}

	class RunningPathListener implements NodeCacheListener {

		private int item;

		public RunningPathListener(int item) {
			this.item = item;
		}

		@Override
		public void nodeChanged() throws Exception {
			zkCacheManager.getExecutorService().execute(new Runnable() {
				@Override
				public void run() {
					try {
						if (isShutdown) {
							return;
						}
						if (!executionService.isRunning(item)) {
							failover(item);
						}
					} catch (Throwable t) {
						log.error(t.getMessage(), t);
					}
				}
			});
		}
	}

	class FailoverPathJobListener implements NodeCacheListener {

		private int item;

		public FailoverPathJobListener(int item) {
			this.item = item;
		}

		@Override
		public void nodeChanged() throws Exception {
			zkCacheManager.getExecutorService().execute(new Runnable() {
				@Override
				public void run() {
					try {
						if (isShutdown) {
							return;
						}
						if (!executionService.isFailover(item)) {
							failover(item);
						}
					} catch (Throwable t) {
						log.error(t.getMessage(), t);
					}
				}
			});
		}
	}

}
