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

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Strings;
import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.election.LeaderElectionService;
import com.vip.saturn.job.utils.LocalHostService;
import com.vip.saturn.job.utils.ResourceUtils;

/**
 * 作业服务器节点服务.
 * @author dylan.xue
 */
public class ServerService extends AbstractSaturnService {
    
    private LeaderElectionService leaderElectionService;
    
    public ServerService(final JobScheduler jobScheduler) {
    	super(jobScheduler);
    	
    }
    
    @Override
	public void start(){
		leaderElectionService = jobScheduler.getLeaderElectionService();
	}
    
    /**
     * 持久化作业服务器上线相关信息.
     */
    public void persistServerOnline() {
        if (!leaderElectionService.hasLeader()) {
            leaderElectionService.leaderElection();
        }
        persistIp();
        persistVersion();
        ephemeralPersistServerReady();
    }
    
    private void persistVersion() {
		final Properties props = ResourceUtils
				.getResource("properties/saturn-core.properties");
		if (props != null) {
			String version = props.getProperty("build.version");
			if (!Strings.isNullOrEmpty(version)) {
				getJobNodeStorage().fillJobNodeIfNullOrOverwrite(ServerNode.getVersionNode(executorName), version);
			}
		}
	}
    
    public void resetCount() {
    	persistProcessFailureCount(0);
    	persistProcessSuccessCount(0);
    }

	private void persistIp() {
        getJobNodeStorage().fillJobNodeIfNullOrOverwrite(ServerNode.getIpNode(executorName), LocalHostService.cachedIpAddress);
    }
    
    private void ephemeralPersistServerReady() {
        getJobNodeStorage().fillEphemeralJobNode(ServerNode.getStatusNode(executorName), ServerStatus.READY);
    }
    
    /**
     * 清除立即运行的标记
     */
    public void clearRunOneTimePath(){
    	getJobNodeStorage().removeJobNodeIfExisted(ServerNode.getRunOneTimePath(executorName));
    }
    
    /**
     * 清除立即终止作业的标记
     */
    public void clearStopOneTimePath(){
    	getJobNodeStorage().removeJobNodeIfExisted(ServerNode.getStopOneTimePath(executorName));
    }
    
    
    /**
     * 在开始或结束执行作业时更新服务器状态.
     * 
     * @param status 服务器状态
     */
    public void updateServerStatus(final ServerStatus status) {
        getJobNodeStorage().updateJobNode(ServerNode.getStatusNode(executorName), status);
    }
    
    /**
     * 获取该作业的所有服务器列表.
     * 
     * @return 所有的作业服务器列表
     */
    public List<String> getAllServers() {
        List<String> result = getJobNodeStorage().getJobNodeChildrenKeys(ServerNode.ROOT);
        Collections.sort(result);
        return result;
    }
    
    /**
     * 判断当前服务器是否是等待执行的状态.
     * 
     * @return 当前服务器是否是等待执行的状态
     */
    public boolean isServerReady() {
        String statusNode = ServerNode.getStatusNode(executorName);
        if (getJobNodeStorage().isJobNodeExisted(statusNode) && ServerStatus.READY.name().equals(getJobNodeStorage().getJobNodeData(statusNode))) {
            return true;
        }
        return false;
    }
    
    /**
     * 持久化统计处理数据成功的数量的数据.
     * 
     * @param processSuccessCount 成功数
     */
    public void persistProcessSuccessCount(final int processSuccessCount) {
        getJobNodeStorage().replaceJobNode(ServerNode.getProcessSuccessCountNode(executorName), processSuccessCount);
    }
    
    /**
     * 持久化统计处理数据失败的数量的数据.
     * 
     * @param processFailureCount 失败数
     */
    public void persistProcessFailureCount(final int processFailureCount) {
        getJobNodeStorage().replaceJobNode(ServerNode.getProcessFailureCountNode(executorName), processFailureCount);
    }
    
}
