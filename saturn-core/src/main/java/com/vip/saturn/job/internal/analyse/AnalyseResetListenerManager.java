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

package com.vip.saturn.job.internal.analyse;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.listener.AbstractJobListener;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;
import com.vip.saturn.job.internal.statistics.ProcessCountStatistics;
import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * @author chembo.huang
 */
public class AnalyseResetListenerManager extends AbstractListenerManager {

	static Logger log = LoggerFactory.getLogger(AnalyseResetListenerManager.class);

	private boolean isShutdown = false;

	public AnalyseResetListenerManager(JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	@Override
	public void start() {
		zkCacheManager.addTreeCacheListener(new AnalyseResetPathListener(),
				JobNodePath.getNodeFullPath(jobName, AnalyseNode.RESET), 0);
	}

	class AnalyseResetPathListener extends AbstractJobListener {

		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			if (isShutdown) {
				return;
			}
			if (JobNodePath.getNodeFullPath(jobName, AnalyseNode.RESET).equals(path)
					&& (Type.NODE_UPDATED == event.getType() || Type.NODE_ADDED == event.getType())) {
				if (ResetCountType.RESET_ANALYSE.equals(new String(event.getData().getData()))) {
					log.info("[{}] msg=job:{} reset anaylse count.", jobName, jobName);
					ProcessCountStatistics.resetAnalyseCount(executorName, jobName);
				} else if (ResetCountType.RESET_SERVERS.equals(new String(event.getData().getData()))) {
					log.info("[{}] msg=job:{} reset success/failure count.", jobName, jobName);
					ProcessCountStatistics.resetSuccessFailureCount(executorName, jobName);
				}
			}
		}
	}

	@Override
	public void shutdown() {
		super.shutdown();
		isShutdown = true;
		zkCacheManager.closeTreeCache(JobNodePath.getNodeFullPath(jobName, AnalyseNode.RESET), 0);
	}

}
