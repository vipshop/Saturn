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

package com.vip.saturn.job.internal.sharding;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.SaturnConstant;
import com.vip.saturn.job.exception.JobShuttingDownException;
import com.vip.saturn.job.internal.election.LeaderElectionService;
import com.vip.saturn.job.internal.execution.ExecutionService;
import com.vip.saturn.job.internal.server.ServerService;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.internal.storage.TransactionExecutionCallback;
import com.vip.saturn.job.sharding.service.NamespaceShardingContentService;
import com.vip.saturn.job.utils.BlockUtils;
import com.vip.saturn.job.utils.ItemUtils;

/**
 * 作业分片服务.
 * 
 * 
 */
public class ShardingService extends AbstractSaturnService {
	static Logger log = LoggerFactory.getLogger(ShardingService.class);

    private LeaderElectionService leaderElectionService;
     
    private ServerService serverService;
    
    private ExecutionService executionService;
    
    private NamespaceShardingContentService namespaceShardingContentService;

    public final static String SHARDING_UN_NECESSARY = "0";
    
    private volatile boolean isShutdown;

    
    public ShardingService(final JobScheduler jobScheduler) {
    	super(jobScheduler);
    }
    
    @Override
	public synchronized void start(){
		leaderElectionService = jobScheduler.getLeaderElectionService();
        serverService = jobScheduler.getServerService();
        executionService = jobScheduler.getExecutionService();
        namespaceShardingContentService = new NamespaceShardingContentService((CuratorFramework)coordinatorRegistryCenter.getRawClient());
	}
	
    /**
     * 判断是否需要重分片.
     * @return 是否需要重分片
     */
    public boolean isNeedSharding() {
        return getJobNodeStorage().isJobNodeExisted(ShardingNode.NECESSARY) && !SHARDING_UN_NECESSARY.equals(getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.NECESSARY));
    }
    
    /**
     * 如果需要分片且当前节点为主节点, 则作业分片.
     */
    public synchronized void shardingIfNecessary() throws JobExecutionException {
        String preNecessaryData = SHARDING_UN_NECESSARY;
        if(getJobNodeStorage().isJobNodeExisted(ShardingNode.NECESSARY)) {
            preNecessaryData = getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.NECESSARY);
        }
        if(SHARDING_UN_NECESSARY.equals(preNecessaryData)) {
            return;
        }

        if(blockUntilShardingComplatedIfNotLeader()) {
        	return;
        }
		waitingOtherJobCompleted();
        log.debug("Saturn job: {} sharding begin.", jobName);
        getJobNodeStorage().fillEphemeralJobNode(ShardingNode.PROCESSING, "");
        clearShardingInfo();
        Map<String, List<Integer>> shardingItems = new LinkedHashMap<>();
        try {
            while(!isShutdown) {
                // 失败重试三次
                int retry = 3;
                while (retry-- > 0) {
                    try {
                        shardingItems = namespaceShardingContentService.getShardingItems(jobName);
                        break;
                    } catch (Exception e) {//NOSONAR
                    	log.debug("Saturn job:{} retry sharding remains:{} time",jobName,retry);
                    }
                }
                // 是否需要重拿分片
                // assert(getJobNodeStorage().isJobNodeExisted(ShardingNode.NECESSARY));
                String currentNecessaryData = getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.NECESSARY);
                if (preNecessaryData.equals(currentNecessaryData)) {
                    break;
                } else {
                    preNecessaryData = currentNecessaryData;
                }
            }
        } catch (Exception e) {
        	log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, e.getMessage()), e);
        	throw new JobExecutionException(e);
        } finally {
            log.debug("Saturn job: {} sharding get sharding content: {}", jobName, shardingItems);
        	// 清除leader/sharding/necessary节点，避免msgJob无法触发msgService.reExecuteJob()
        	getJobNodeStorage().executeInTransaction(new PersistShardingInfoTransactionExecutionCallback(shardingItems));
            log.debug("Saturn job: {} sharding completed.", jobName);
        }
    }
    
    /**
     * 如果不是leader，等待leader分片完成，返回true；如果期间变为leader，返回false。
     * @return true or false
     * @throws JobShuttingDownException 
     */
    private boolean blockUntilShardingComplatedIfNotLeader() throws JobShuttingDownException {
    	for(;;) {
    		if(isShutdown) {
    	    	throw new JobShuttingDownException();
    		}
    		if(leaderElectionService.isLeader()) {
    			return false;
    		}
			if(!(isNeedSharding() || getJobNodeStorage().isJobNodeExisted(ShardingNode.PROCESSING))) {
				return true;
			}
			log.debug("Elastic job: sleep short time until sharding completed.");
			BlockUtils.waitingShortTime();
    	}
    }
    
    private void waitingOtherJobCompleted() {
        while (!isShutdown && executionService.hasRunningItems()) {
            log.debug("Elastic job: sleep short time until other job completed.");
            BlockUtils.waitingShortTime();
        }
    }
    
    private void clearShardingInfo() {
        for (String each : serverService.getAllServers()) {
            getJobNodeStorage().removeJobNodeIfExisted(ShardingNode.getShardingNode(each));
        }
    }
    
    /**
     * 获取运行在本作业服务器的分片序列号.
     * 
     * @return 运行在本作业服务器的分片序列号
     */
    public List<Integer> getLocalHostShardingItems() {
        if (!getJobNodeStorage().isJobNodeExisted(ShardingNode.getShardingNode(executorName))) {
            return Collections.<Integer>emptyList();
        }
        return ItemUtils.toItemList(getJobNodeStorage().getJobNodeDataDirectly(ShardingNode.getShardingNode(executorName)));
    }
    
    @Override
    public void shutdown() {
    	isShutdown = true;
    }
    
    class PersistShardingInfoTransactionExecutionCallback implements TransactionExecutionCallback {
        
        private final Map<String, List<Integer>> shardingItems;
               
        public PersistShardingInfoTransactionExecutionCallback(Map<String, List<Integer>> shardingItems) {
			super();
			this.shardingItems = shardingItems;
		}

		@Override
        public void execute(final CuratorTransactionFinal curatorTransactionFinal) throws Exception {
        	if(isShutdown) {
        		return;
        	}
            for (Entry<String, List<Integer>> entry : shardingItems.entrySet()) {
                // create server node first if not exists
                String serverNodePath = JobNodePath.getServerNodePath(jobName, entry.getKey());
                if(!coordinatorRegistryCenter.isExisted(serverNodePath)) {
                    coordinatorRegistryCenter.persist(serverNodePath, "");
                }
                curatorTransactionFinal.create().forPath(JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(entry.getKey())), ItemUtils.toItemsString(entry.getValue()).getBytes(StandardCharsets.UTF_8)).and();
            }
            curatorTransactionFinal.setData().forPath(JobNodePath.getNodeFullPath(jobName, ShardingNode.NECESSARY), SHARDING_UN_NECESSARY.getBytes(StandardCharsets.UTF_8)).and();
            curatorTransactionFinal.delete().forPath(JobNodePath.getNodeFullPath(jobName, ShardingNode.PROCESSING)).and();
        }
    }
}
