/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. </p>
 */

package com.vip.saturn.job.internal.server;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.listener.AbstractJobListener;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.LogUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 作业控制监听管理器.
 * @author dylan.xue
 */
public class JobOperationListenerManager extends AbstractListenerManager {

	static Logger log = LoggerFactory.getLogger(JobOperationListenerManager.class);

	private boolean isShutdown = false;

	private ExecutorService jobDeleteExecutorService;

	public JobOperationListenerManager(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	@Override
	public void start() {
		jobDeleteExecutorService = Executors
				.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-" + jobName + "-jobDelete", false));
		zkCacheManager.addTreeCacheListener(new TriggerJobRunAtOnceListener(),
				JobNodePath.getNodeFullPath(jobName, String.format(ServerNode.RUNONETIME, executorName)), 0);
		zkCacheManager.addTreeCacheListener(new JobForcedToStopListener(),
				JobNodePath.getNodeFullPath(jobName, String.format(ServerNode.STOPONETIME, executorName)), 0);
		zkCacheManager.addTreeCacheListener(new JobDeleteListener(),
				JobNodePath.getNodeFullPath(jobName, ConfigurationNode.TO_DELETE), 0);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		isShutdown = true;
		if (jobDeleteExecutorService != null) {
			// don't use shutdownNow, don't interrupt the thread
			jobDeleteExecutorService.shutdown();
		}
		zkCacheManager.closeTreeCache(
				JobNodePath.getNodeFullPath(jobName, String.format(ServerNode.RUNONETIME, executorName)), 0);
		zkCacheManager.closeTreeCache(
				JobNodePath.getNodeFullPath(jobName, String.format(ServerNode.STOPONETIME, executorName)), 0);
		zkCacheManager.closeTreeCache(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.TO_DELETE), 0);
	}

	/**
	 * runOneTime operation.<br>
	 * once triggered, delete the node.
	 * @author chembo.huang
	 *
	 */
	class TriggerJobRunAtOnceListener extends AbstractJobListener {

		@Override
		protected void dataChanged(final CuratorFramework client, final TreeCacheEvent event, final String path) {
			if (isShutdown) {
				return;
			}
			if ((Type.NODE_ADDED == event.getType() || Type.NODE_UPDATED == event.getType()) && ServerNode
					.isRunOneTimePath(jobName, path, executorName)) {
				if (!jobScheduler.getJob().isRunning()) {
					String triggeredDataStr = getTriggeredDataStr(event);
					LogUtils.info(log, jobName, "job run-at-once triggered, triggeredData:{}", triggeredDataStr);
					jobScheduler.triggerJob(triggeredDataStr);
				} else {
					LogUtils.info(log, jobName, "job is running, run-at-once ignored.");
				}
				coordinatorRegistryCenter.remove(path);
			}
		}

		private String getTriggeredDataStr(final TreeCacheEvent event) {
			String transDataStr = null;
			try {
				byte[] data = event.getData().getData();
				if (data != null) {
					transDataStr = new String(data, "UTF-8");
				}
			} catch (UnsupportedEncodingException e) {
				LogUtils.error(log, jobName, "unexpected error", e);
			}
			return transDataStr;
		}

	}

	/**
	 * 作业删除
	 * @author dylan.xue
	 *
	 */
	class JobDeleteListener extends AbstractJobListener {

		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			if (isShutdown) {
				return;
			}
			if (ConfigurationNode.isToDeletePath(jobName, path) && (Type.NODE_ADDED == event.getType()
					|| Type.NODE_UPDATED == event.getType())) {
				LogUtils.info(log, jobName, "job is going to be deleted.");
				jobDeleteExecutorService.execute(new Runnable() {
					@Override
					public void run() {
						try {
							jobScheduler.shutdown(true);
						} catch (Throwable t) {
							LogUtils.error(log, jobName, "delete job error", t);
						}
					}
				});
			}
		}
	}

	/**
	 * 作业立刻终止
	 * @author juanying.yang
	 *
	 */
	class JobForcedToStopListener extends AbstractJobListener {

		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			if (isShutdown) {
				return;
			}
			if (Type.NODE_ADDED == event.getType() || Type.NODE_UPDATED == event.getType()) {
				try {
					LogUtils.info(log, jobName, "job is going to be stopped at once.");
					jobScheduler.getJob().forceStop();
				} finally {
					coordinatorRegistryCenter.remove(path);
				}
			}
		}
	}
}
