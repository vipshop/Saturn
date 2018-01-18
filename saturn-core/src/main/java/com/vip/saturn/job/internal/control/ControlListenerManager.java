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

package com.vip.saturn.job.internal.control;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.listener.AbstractJobListener;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;
import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * @author chembo.huang
 *
 */
public class ControlListenerManager extends AbstractListenerManager {
	static Logger log = LoggerFactory.getLogger(ControlListenerManager.class);

	private boolean isShutdown = false;

	private ReportService reportService;

	public ControlListenerManager(JobScheduler jobScheduler) {
		super(jobScheduler);
		reportService = jobScheduler.getReportService();
	}

	@Override
	public void start() {
		zkCacheManager.addTreeCacheListener(new ReportPathListener(),
				JobNodePath.getNodeFullPath(jobName, ControlNode.REPORT_NODE), 0);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		isShutdown = true;
		zkCacheManager.closeTreeCache(JobNodePath.getNodeFullPath(jobName, ControlNode.REPORT_NODE), 0);
	}

	class ReportPathListener extends AbstractJobListener {
		@Override
		protected void dataChanged(CuratorFramework client, TreeCacheEvent event, String path) {
			if (isShutdown) {
				return;
			}
			if (ControlNode.isReportPath(jobName, path)
					&& (Type.NODE_UPDATED == event.getType() || Type.NODE_ADDED == event.getType())) {
				log.info("[{}] msg={} received report event from console, start to flush data to zk.", jobName,
						jobName);
				reportService.reportData2Zk();
			}
		}
	}

}
