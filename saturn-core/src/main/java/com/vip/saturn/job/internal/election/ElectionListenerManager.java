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

package com.vip.saturn.job.internal.election;

import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;
import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * 主节点选举监听管理器.
 *
 *
 */
public class ElectionListenerManager extends AbstractListenerManager {

	static Logger log = LoggerFactory.getLogger(ElectionListenerManager.class);

	private final LeaderElectionService leaderElectionService;

	private boolean isShutdown;

	public ElectionListenerManager(final JobScheduler jobScheduler) {
		super(jobScheduler);
		leaderElectionService = new LeaderElectionService(jobScheduler);
	}

	@Override
	public void start() {
		zkCacheManager.addNodeCacheListener(new LeaderElectionJobListener(),
				JobNodePath.getNodeFullPath(jobName, ElectionNode.LEADER_HOST));
	}

	@Override
	public void shutdown() {
		isShutdown = true;
		leaderElectionService.shutdown();
		zkCacheManager.closeNodeCache(JobNodePath.getNodeFullPath(jobName, ElectionNode.LEADER_HOST));
	}

	class LeaderElectionJobListener implements NodeCacheListener {

		@Override
		public void nodeChanged() throws Exception {
			zkCacheManager.getExecutorService().execute(new Runnable() {
				@Override
				public void run() {
					try {
						log.debug("[{}] msg=Leader host nodeChanged", jobName);
						if (isShutdown) {
							log.debug("[{}] msg=ElectionListenerManager has been shutdown", jobName);
							return;
						}
						if (!leaderElectionService.hasLeader()) {
							log.info("[{}] msg=Leader crashed, elect a new leader now", jobName);
							leaderElectionService.leaderElection();
							log.info("[{}] msg=Leader election completed", jobName);
						} else {
							log.debug("[{}] msg=Leader is already existing, unnecessary to election", jobName);
						}
					} catch (Throwable t) {
						log.error(t.getMessage(), t);
					}
				}
			});
		}
	}
}
