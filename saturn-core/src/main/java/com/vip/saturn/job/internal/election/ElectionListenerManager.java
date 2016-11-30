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

package com.vip.saturn.job.internal.election;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.listener.AbstractJobListener;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;

/**
 * 主节点选举监听管理器.
 * 
 * 
 */
public class ElectionListenerManager extends AbstractListenerManager {
	static Logger log = LoggerFactory.getLogger(ElectionListenerManager.class);

    private final LeaderElectionService leaderElectionService;
    
    private final ElectionNode electionNode;
    
    private boolean isShutdown;

	public ElectionListenerManager(final JobScheduler jobScheduler) {
        super(jobScheduler);
        leaderElectionService = new LeaderElectionService(jobScheduler);
        electionNode = new ElectionNode(jobName);
    }
    
    @Override
    public void start() {
        addDataListener(new LeaderElectionJobListener(), jobConfiguration.getJobName());
    }
    
    @Override
    public void shutdown() {
    	isShutdown = true;
    	leaderElectionService.shutdown();
    }
    
    class LeaderElectionJobListener extends AbstractJobListener {
        
        @Override
        protected void dataChanged(final CuratorFramework client, final TreeCacheEvent event, final String path) {
        	if(isShutdown) return;
            if (electionNode.isLeaderHostPath(path) && Type.NODE_REMOVED == event.getType() && !leaderElectionService.hasLeader()) {
                log.info("[{}] msg=Elastic job: leader crashed, elect a new leader now.",jobName);
                leaderElectionService.leaderElection();
                log.info("[{}] msg=Elastic job: leader election completed.",jobName);
            }
        }
    }
}
