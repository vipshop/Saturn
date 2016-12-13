/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.internal.server;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.listener.AbstractJobListener;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;

/**
 * 作业控制监听管理器.
 * @author dylan.xue
 */
public class JobOperationListenerManager extends AbstractListenerManager {
	static Logger log = LoggerFactory.getLogger(JobOperationListenerManager.class);

	private boolean isShutdown = false;

	public JobOperationListenerManager(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}
	
	@Override
	public void start() {
		addDataListener(new TriggerJobRunAtOnceListener(), jobName);
		addDataListener(new JobDeleteListener(), jobName);
		addDataListener(new JobForcedToStopListener(), jobName);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		isShutdown = true;
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
			if(isShutdown) return;
			if (Type.NODE_ADDED == event.getType() && ServerNode.isRunOneTimePath(jobName, path, executorName)) {
				if (!jobScheduler.getJob().isRunning()) {
					log.info("[{}] msg=job run-at-once triggered.", jobName);
					jobScheduler.triggerJob();
				} else {
					log.info("[{}] msg=job is running, run-at-once ignored.", jobName);
				}
				coordinatorRegistryCenter.remove(path);
			}
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
			if(isShutdown) return;
			if(ConfigurationNode.isToDeletePath(jobName, path) && Type.NODE_ADDED == event.getType()) {
				log.info("[{}] msg={} is going to be deleted", jobName, jobName);
				jobScheduler.shutdown(true);
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
			if(isShutdown) return;
			if (Type.NODE_ADDED == event.getType() && ServerNode.isStopOneTimePath(jobName, path, executorName)) {
				try{
					log.info("[{}] msg={} is going to be stop at once", jobName, jobName);	
					jobScheduler.getJob().forceStop();
				}finally{
					coordinatorRegistryCenter.remove(path);
				}
			}
		}
	}
}
