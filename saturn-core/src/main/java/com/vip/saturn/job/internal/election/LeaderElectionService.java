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

import com.vip.saturn.job.basic.SaturnConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.storage.LeaderExecutionCallback;
import com.vip.saturn.job.utils.BlockUtils;

/**
 * 选举主节点的服务.
 * 
 * 
 */
public class LeaderElectionService extends AbstractSaturnService{
	static Logger log = LoggerFactory.getLogger(LeaderElectionService.class);

    private boolean isShutdown;
    
    public LeaderElectionService(final JobScheduler jobScheduler) {
    	super(jobScheduler);
    }


    @Override
    public void shutdown() {
        if(!isShutdown) {
            isShutdown = true;
            releaseMyLeader();
        }
    }

    /**
     * Release my leader position
     */
    public void releaseMyLeader() {
        try {
            if (executorName.equals(getJobNodeStorage().getJobNodeData(ElectionNode.LEADER_HOST))) {
                getJobNodeStorage().removeJobNodeIfExisted(ElectionNode.LEADER_HOST);
            }
        } catch (Throwable t) {
            log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, "release my leader error"), t);
        }
    }
    
    /**
     * 选举主节点.
     */
    public void leaderElection() {
        getJobNodeStorage().executeInLeader(ElectionNode.LATCH, new LeaderElectionExecutionCallback());
    }
    
    /**
     * 判断当前节点是否是主节点.
     * 
     * <p>
     * 如果主节点正在选举中而导致取不到主节点, 则阻塞至主节点选举完成再返回.
     * </p>
     * 
     * @return 当前节点是否是主节点
     */
    public Boolean isLeader() {
        while (!isShutdown && !hasLeader()) {
            log.info("[{}] msg=Elastic job: {} leader node is electing, waiting for 100 ms at executor '{}'", jobName, jobName, executorName);
            BlockUtils.waitingShortTime();
        }
        return executorName.equals(getJobNodeStorage().getJobNodeData(ElectionNode.LEADER_HOST));
    }
    
    /**
     * 判断是否已经有主节点.
     * 
     * <p>
     * 仅为选举监听使用.
     * 程序中其他地方判断是否有主节点应使用{@code isLeader() }方法.
     * </p>
     * 
     * @return 是否已经有主节点
     */
    public boolean hasLeader() {
        return getJobNodeStorage().isJobNodeExisted(ElectionNode.LEADER_HOST);
    }
    
    class LeaderElectionExecutionCallback implements LeaderExecutionCallback {
        
        @Override
        public void execute() {
        	if(isShutdown) return;
            if (!getJobNodeStorage().isJobNodeExisted(ElectionNode.LEADER_HOST)) {
                getJobNodeStorage().fillEphemeralJobNode(ElectionNode.LEADER_HOST, executorName);
            }
        }
    }
}
