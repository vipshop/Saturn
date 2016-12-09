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

package com.vip.saturn.job.internal.execution;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.SaturnSystemErrorGroup;
import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobExecutionMultipleShardingContext;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.SaturnExecutionContext;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.internal.failover.FailoverNode;
import com.vip.saturn.job.internal.server.ServerService;
import com.vip.saturn.job.internal.server.ServerStatus;

/**
 * 执行作业的服务.
 * @author dylan.xue
 */
public class ExecutionService extends AbstractSaturnService {
	static Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private ConfigurationService configService;
    
    private ServerService serverService;
    
	public ExecutionService(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}
	
	@Override
	public void start(){
		configService = jobScheduler.getConfigService();
        serverService = jobScheduler.getServerService();
	}
	
    /**
     * 更新当前作业服务器运行时分片的nextFireTime和pausePeriodEffected，如果pausePeriodEffected为false。
     * 
     * @param shardingItems 作业运行时分片上下文
     */
    public void updateNextFireTimeAndPausePeriodEffectedIfNecessary(final List<Integer> shardingItems) {
    	if (!shardingItems.isEmpty()) {
    		for (int item : shardingItems) {
    			updateNextFireTimeAndPausePeriodEffectedIfNecessary(item);
    		}
    	}
    }
    
    /**
     * 更新当前作业服务器运行时分片的nextFireTime和pausePeriodEffected。
     * 
     * @param shardingItems 作业运行时分片上下文
     */
    public void updateNextFireTimeAndPausePeriodEffected(final List<Integer> shardingItems) {
    	if (!shardingItems.isEmpty()) {
    		for (int item : shardingItems) {
    			updateNextFireTimeAndPausePeriodEffected(item);
    		}
    	}
    }
    
    private void updateNextFireTimeAndPausePeriodEffectedIfNecessary(int item) {
		String pausePeriodEffectedNode = ExecutionNode.getPausePeriodEffectedNode(item);
		if(!getJobNodeStorage().isJobNodeExisted(pausePeriodEffectedNode) || !Boolean.parseBoolean(getJobNodeStorage().getJobNodeDataDirectly(pausePeriodEffectedNode))) {
			updateNextFireTimeAndPausePeriodEffected(item);
		}
    }
    
    private void updateNextFireTimeAndPausePeriodEffected(int item) {
        if (null == jobScheduler) {
            return;
        }
        NextFireTimePausePeriodEffected nextFireTimePausePeriodEffected = jobScheduler.getNextFireTimePausePeriodEffected();
        if (null != nextFireTimePausePeriodEffected && null != nextFireTimePausePeriodEffected.getNextFireTime()) {
        	String pausePeriodEffectedNode = ExecutionNode.getPausePeriodEffectedNode(item);
            getJobNodeStorage().replaceJobNode(ExecutionNode.getNextFireTimeNode(item), nextFireTimePausePeriodEffected.getNextFireTime().getTime());
            getJobNodeStorage().replaceJobNode(pausePeriodEffectedNode, nextFireTimePausePeriodEffected.isPausePeriodEffected());
        }
    }
    
    /**
     * 注册作业启动信息.
     * 
     * @param jobExecutionShardingContext 作业运行时分片上下文
     */
	public void registerJobBegin(final JobExecutionMultipleShardingContext jobExecutionShardingContext) {
		List<Integer> shardingItems = jobExecutionShardingContext.getShardingItems();
		if (!shardingItems.isEmpty()) {
			serverService.updateServerStatus(ServerStatus.RUNNING);
			for (int item : shardingItems) {
				registerJobBeginByItem(jobExecutionShardingContext, item);
			}
		}
	}
	
	public void registerJobBeginByItem(final JobExecutionMultipleShardingContext jobExecutionShardingContext, int item) {
		if(log.isDebugEnabled()){
			log.debug("registerJobBeginByItem: " + item);
		}
		getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getCompletedNode(item));
		getJobNodeStorage().fillEphemeralJobNode(ExecutionNode.getRunningNode(item), "");

		// 清除完成状态timeout等信息
		cleanSaturnNode(item);

		getJobNodeStorage().replaceJobNode(ExecutionNode.getLastBeginTimeNode(item), System.currentTimeMillis());

		//updateNextFireTimeAndPausePeriodEffected(item);
	}
    
    /**
     * 注册作业完成信息.
     * 
     * @param jobExecutionShardingContext 作业运行时分片上下文
     * @param item 分片项
     */
    public void registerJobCompleted(final JobExecutionMultipleShardingContext jobExecutionShardingContext,Integer item) {
		registerJobCompletedByItem(jobExecutionShardingContext, item);
    }
    
	public void registerJobCompletedByItem(final JobExecutionMultipleShardingContext jobExecutionShardingContext, int item) {
		if(log.isDebugEnabled()){
			log.debug("registerJobCompletedByItem: " + item);
		}
		if (jobExecutionShardingContext instanceof SaturnExecutionContext) {
			// 为了展现分片处理失败的状态
			SaturnExecutionContext saturnContext = (SaturnExecutionContext) jobExecutionShardingContext;
			if (saturnContext.isSaturnJob()) {
				SaturnJobReturn jobRet = saturnContext.getShardingItemResults().get(item);
				if(jobRet != null) {
					int errorGroup = jobRet.getErrorGroup();
					if(errorGroup == SaturnSystemErrorGroup.SUCCESS) {
    					getJobNodeStorage().replaceJobNode(ExecutionNode.getJobMsg(item), jobRet.getReturnMsg() == null ? "" : jobRet.getReturnMsg());
    					if (configService.showNormalLog()) {
    						//saturnTemplate.opsForZSet().add(redisKey, jobLogMap, createdTimestamp);
    						getJobNodeStorage().replaceJobNode(ExecutionNode.getJobLog(item), saturnContext.getJobLog(item) == null ? "" : saturnContext.getJobLog(item));
						} 
					} else if(errorGroup == SaturnSystemErrorGroup.TIMEOUT) {
    					getJobNodeStorage().createJobNodeIfNeeded(ExecutionNode.getTimeoutNode(item));
    					getJobNodeStorage().replaceJobNode(ExecutionNode.getJobMsg(item), jobRet.getReturnMsg() == null ? "" : jobRet.getReturnMsg());
						//saturnTemplate.opsForZSet().add(redisKey, jobLogMap, createdTimestamp);
    					getJobNodeStorage().replaceJobNode(ExecutionNode.getJobLog(item), saturnContext.getJobLog(item) == null ? "" : saturnContext.getJobLog(item));
					} else {
    					getJobNodeStorage().createJobNodeIfNeeded(ExecutionNode.getFailedNode(item));
    					getJobNodeStorage().replaceJobNode(ExecutionNode.getJobMsg(item), jobRet.getReturnMsg() == null ? "" : jobRet.getReturnMsg());
						//saturnTemplate.opsForZSet().add(redisKey, jobLogMap, createdTimestamp);
    					getJobNodeStorage().replaceJobNode(ExecutionNode.getJobLog(item), saturnContext.getJobLog(item) == null ? "" : saturnContext.getJobLog(item));
					}
				} else { // if there is no jobReturn, don't save empty content to redis.
					getJobNodeStorage().createJobNodeIfNeeded(ExecutionNode.getFailedNode(item));
					getJobNodeStorage().replaceJobNode(ExecutionNode.getJobMsg(item), "执行Job没有返回值");
					getJobNodeStorage().replaceJobNode(ExecutionNode.getJobLog(item), "");
				}
			}
		}
		updateNextFireTimeAndPausePeriodEffected(item);
		getJobNodeStorage().createJobNodeIfNeeded(ExecutionNode.getCompletedNode(item));
		getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getRunningNode(item));
		getJobNodeStorage().replaceJobNode(ExecutionNode.getLastCompleteTimeNode(item), System.currentTimeMillis());
	}
    
    /**
     * 设置修复运行时分片信息标记的状态标志位.
     */
/*    public void setNeedFixExecutionInfoFlag() {
        getJobNodeStorage().createJobNodeIfNeeded(ExecutionNode.NECESSARY);
    }*/
    
    
    /**
     * 清除分配分片序列号的运行状态.
     * 
     * <p>
     * 用于作业服务器恢复连接注册中心而重新上线的场景, 先清理上次运行时信息.
     * </p>
     * 
     * @param items 需要清理的分片项列表
     */
    public void clearRunningInfo(final List<Integer> items) {
        for (int each : items) {
        	// 已被其他executor接管的正在failover的分片不清理running节点，防止清理节点时触发JobCrashedJobListener导致重新failover了一次
        	if(!getJobNodeStorage().isJobNodeExisted(FailoverNode.getExecutionFailoverNode(each))){
        		getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getRunningNode(each));
        		//清除完成状态timeout等信息
        		cleanSaturnNode(each);
        	}
        }
    }
    
    /**
     * 删除作业执行时信息.
     */
    public void removeExecutionInfo() {
        getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.ROOT);
    }
    
    /**
     * 判断该分片是否已完成.
     * 
     * @param item 运行中的分片路径
     * @return 该分片是否已完成
     */
    public boolean isCompleted(final int item) {
        return getJobNodeStorage().isJobNodeExisted(ExecutionNode.getCompletedNode(item));
    }
    
    /**
     * 判断分片项中是否还有执行中的作业.
     * 
     * @param items 需要判断的分片项列表
     * @return 分片项中是否还有执行中的作业
     */
    public boolean hasRunningItems(final List<Integer> items) {
        for (int each : items) {
            if (getJobNodeStorage().isJobNodeExisted(ExecutionNode.getRunningNode(each))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断是否还有执行中的作业.
     * 
     * @return 是否还有执行中的作业
     */
    public boolean hasRunningItems() {
        return hasRunningItems(getAllItems());
    }
    
    private List<Integer> getAllItems() {
        return Lists.transform(getJobNodeStorage().getJobNodeChildrenKeys(ExecutionNode.ROOT), new Function<String, Integer>() {
            
            @Override
            public Integer apply(final String input) {
                return Integer.parseInt(input);
            }
        });
    }
    
    /**
     * 删除Saturn的作业item信息
     * @param item 作业分片
     */
    private void cleanSaturnNode(int item){
        getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getFailedNode(item));
        getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getTimeoutNode(item));
        getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getJobMsg(item));
        getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getJobLog(item));
    }
    
}
