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

package com.vip.saturn.job.reg.base;

import java.util.List;

import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionStateListener;

/**
 * 用于协调分布式服务的注册中心.
 * 
 * 
 */
public interface CoordinatorRegistryCenter extends RegistryCenter {
    
    /**
     * 直接从注册中心而非本地缓存获取数据.
     * 
     * @param key 键
     * @return 值
     */
    String getDirectly(String key);
    
    /**
     * 获取子节点名称集合.
     * 
     * @param key 键
     * @return 子节点名称集合
     */
    List<String> getChildrenKeys(String key);
    
    /**
     * 持久化临时注册数据.
     * 
     * @param key 键
     * @param value 值
     */
    void persistEphemeral(String key, String value);
    
    /**
     * 持久化临时顺序注册数据.
     * 
     * @param key 键
     */
    void persistEphemeralSequential(String key);
    
    /**
     * 添加本地缓存.
     * 
     * @param cachePath 需加入缓存的路径
     */
    void addCacheData(String cachePath);
    
    /**
     * 获取注册中心数据缓存对象.
     * 
     * @param cachePath 缓存的节点路径
     * @return 注册中心数据缓存对象
     */
    TreeCache getRawCache(String cachePath);
    
    /**
     * 获取会话超时时间
     * @return 会话超时时间
     */
    long getSessionTimeout();
    
    /**
     * 获取namespace
     * @return
     */
    String getNamespace();
    
    /**
     * 设置executorName
     * @param executorName
     */
    void setExecutorName(String executorName);
    
    /**
     * 获取executorName
     * @return
     */
    String getExecutorName();

    /**
     * 关闭treeCache
     * @param cachePath
     */
	void closeTreeCache(String cachePath);

	/**
	 * 添加treeCache事件监听
	 * @param listener
	 * @param jobName
	 */
	void addTreeCacheListener(final TreeCacheListener listener, String jobName);

	/**
	 * 添加连接状态变更事件监听
	 * @param listener
	 */
	void addConnectionStateListener(ConnectionStateListener listener);

	/**
	 * 删除连接状态变更事件监听
	 * @param listener
	 */
	void removeConnectionStateListener(ConnectionStateListener listener);
	
	/**
	 * 是否己连上
	 * @return
	 */
	boolean isConnected();
}
