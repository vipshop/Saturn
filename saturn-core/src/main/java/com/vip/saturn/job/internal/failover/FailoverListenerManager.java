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

package com.vip.saturn.job.internal.failover;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.execution.ExecutionService;
import com.vip.saturn.job.internal.listener.AbstractJobListener;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;


/**
 * 失效转移监听管理器.
 * 
 * 
 */
public class FailoverListenerManager extends AbstractListenerManager {
	static Logger log = LoggerFactory.getLogger(FailoverListenerManager.class);

    private final ConfigurationService configService;
    
    private final ExecutionService executionService;
    
    private final FailoverService failoverService;
    
    private final ExecutionNode executionNode;
    
    private final FailoverNode failoverNode;


	public FailoverListenerManager(final JobScheduler jobScheduler) {
		super(jobScheduler);
        configService = jobScheduler.getConfigService();
        executionService = jobScheduler.getExecutionService();
        failoverService = jobScheduler.getFailoverService();
        executionNode = new ExecutionNode(jobConfiguration.getJobName());
        failoverNode = new FailoverNode(jobConfiguration.getJobName());
	}
    
    @Override
    public void start() {
        addDataListener(new JobCrashedJobListener(), configService.getJobName());
        addDataListener(new FailoverJobCrashedJobListener(), configService.getJobName());
        addDataListener(new FailoverSettingsChangedJobListener(), configService.getJobName());
        addDataListener(new FailoverDisabledJobListener(), configService.getJobName());
    }
    
    private synchronized void failover(final Integer item, final TreeCacheEvent event ,final String path) {
       	if(jobScheduler == null || jobScheduler.getJob() == null){
    		return;
    	}
    	if(!jobScheduler.getJob().isFailoverSupported()){
    		return;
    	}
    	
/*    	try {
			Thread.sleep(new Random().nextInt(200));// 防止多机并发同时执行setCrashedFailoverFlag，范围为[0,200]的随机数
		} catch (InterruptedException e) {
			log.error("sleep InterruptedException", e);
		}*/
    	
    	if (!isJobCrashAndNeedFailover(item, event)) {
            return;
        }
    	log.info(" {} - {} is setting crashed item flag {} {} {} ",jobScheduler.getExecutorName(),jobScheduler.getJobName(),item, event.getType(),path);
    	
    	String failoverPath = FailoverNode.getItemsNode(item);
    	
    	if(jobScheduler.getJobNodeStorage().isJobNodeExisted(failoverPath)){
    		return;
    	}
    	
    	String random = ""+Double.toString(Math.random());
    	boolean setCrashedFlag = failoverService.setCrashedFailoverFlag(item,random);
    	if(setCrashedFlag){
    		log.info("setCrashedFailoverType:{},setCrashedFailoverPath:{}",event.getType(),path);
    	}
        String value =  jobScheduler.getJobNodeStorage().getJobNodeDataDirectly(failoverPath);
        if(!random.equals(value)){
        	return;
        }
    	if (!executionService.hasRunningItems(jobScheduler.getShardingService().getLocalHostShardingItems())) {
            failoverService.failoverIfNecessary();
        }
    }
    
    private boolean isJobCrashAndNeedFailover(final Integer item, final TreeCacheEvent event) {
      // 如果分片不为空 && 有Executor是掉线状态 && 如果该failover分片没有其他Executor接管(本条件是为防止zk抖动重连时重跑已被其他Executor接管的failover分片) && 该分片正在运行 && 开启了failover的作业配置开关
      return null != item && Type.NODE_REMOVED == event.getType() && !jobScheduler.getJobNodeStorage().isJobNodeExisted(FailoverNode.getExecutionFailoverNode(item)) && !executionService.isCompleted(item) && configService.isFailover(); //NOSONAR
    }
    
    class JobCrashedJobListener extends AbstractJobListener {
        
        @Override
        protected void dataChanged(final CuratorFramework client, final TreeCacheEvent event, final String path) {
            failover(executionNode.getItemByRunningItemPath(path), event ,path);
        }
    }
    
    class FailoverJobCrashedJobListener extends AbstractJobListener {
        
        @Override
        protected void dataChanged(final CuratorFramework client, final TreeCacheEvent event, final String path) {
            failover(failoverNode.getItemByExecutionFailoverPath(path), event , path);
        }
    }
    
    class FailoverSettingsChangedJobListener extends AbstractJobListener {
        
        @Override
        protected void dataChanged(final CuratorFramework client, final TreeCacheEvent event, final String path) {
            if (ConfigurationNode.isFailoverPath(jobConfiguration.getJobName(), path) && Type.NODE_UPDATED == event.getType()) {
                if (!Boolean.valueOf(new String(event.getData().getData()))) {
                    failoverService.removeFailoverInfo();
                }
            }
        }
    }
    
    class FailoverDisabledJobListener extends AbstractJobListener {
        
        @Override
        protected void dataChanged(final CuratorFramework client, final TreeCacheEvent event, final String path) {
            if (path.matches(SaturnExecutorsNode.JOBCONFIG_ENABLE_NODE_PATH_REGEX) && Type.NODE_UPDATED == event.getType()) {
                if (!Boolean.valueOf(new String(event.getData().getData()))) {
                    failoverService.removeFailoverInfo();// 禁用作业时清除failover标识
                }
            }
        }
    }
}
