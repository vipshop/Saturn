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
package com.vip.saturn.job.sharding.listener;   

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.ExecutorCleanService;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;

/** 
 * @author yangjuanying  
 */
public class ExecutorTriggerShardingListener extends AbstractTreeCacheListener{

	static Logger log = LoggerFactory.getLogger(ExecutorTriggerShardingListener.class);
	
	private NamespaceShardingService namespaceShardingService;
	private ExecutorCleanService executorCleanService;
	
	public ExecutorTriggerShardingListener(NamespaceShardingService namespaceShardingService, ExecutorCleanService executorCleanService) {
		this.namespaceShardingService = namespaceShardingService;
		this.executorCleanService = executorCleanService;
	}
	
	/** 
	 * @see com.vip.saturn.job.sharding.listener.AbstractTreeCacheListener#childEvent(org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type, java.lang.String, java.lang.String)
	 */
	@Override
	public void childEvent(Type type, String path, String nodeData) throws Exception {
		if(isShardAllAtOnce(type,path)) {
			log.info("[] msg=shard-all-at-once triggered.");
        	namespaceShardingService.asyncShardingWhenExecutorAll();
        	executorCleanService.deleteIfExists(path);
        }
	}
	
	private boolean isShardAllAtOnce(Type type, String path) {
        return type == Type.NODE_ADDED && SaturnExecutorsNode.getExecutorShardingNodePath("shardAllAtOnce").equals(path);
    }

}
  