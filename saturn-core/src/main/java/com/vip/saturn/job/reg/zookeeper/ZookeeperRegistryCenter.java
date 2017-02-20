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

package com.vip.saturn.job.reg.zookeeper;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.vip.saturn.job.internal.monitor.MonitorService;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.reg.exception.RegExceptionHandler;

/**
 * 基于Zookeeper的注册中心.
 * 
 * 
 */
public class ZookeeperRegistryCenter implements CoordinatorRegistryCenter {
	static Logger log = LoggerFactory.getLogger(ZookeeperRegistryCenter.class);

    private static final String SLASH_CONSTNAT = "/";

    private ZookeeperConfiguration zkConfig;
    
    private final Map<String, TreeCache> caches = new ConcurrentHashMap<>();
    
    private CuratorFramework client;
    
	/** 连接超时时间 */
	private static int CONNECTION_TIMEOUT = 20 * 1000;
	
	/**
	 * 默认会话超时时间
	 */
	private static int SESSION_TIMEOUT = 20 * 1000;
	
	/** 会话超时时间 */
	private int sessionTimeout = SESSION_TIMEOUT;
	
	private String executorName;
	
    public ZookeeperRegistryCenter(final ZookeeperConfiguration zkConfig) {
        this.zkConfig = zkConfig;
    }
    
    
    public ZookeeperConfiguration getZkConfig() {
		return zkConfig;
	}

    @Override
	public String getExecutorName() {
		return executorName;
	}

    @Override
	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	@Override
	public void init() {
        if (zkConfig.isUseNestedZookeeper()) {
            NestedZookeeperServers.getInstance().startServerIfNotStarted(zkConfig.getNestedPort(), zkConfig.getNestedDataDir());
        }
        log.info("msg=Saturn job: zookeeper registry center init, server lists is: {}.", zkConfig.getServerLists());
        Builder builder = CuratorFrameworkFactory.builder()
                .connectString(zkConfig.getServerLists())
                .sessionTimeoutMs(SESSION_TIMEOUT)
                .connectionTimeoutMs(CONNECTION_TIMEOUT)
                .retryPolicy(new ExponentialBackoffRetry(zkConfig.getBaseSleepTimeMilliseconds(), zkConfig.getMaxRetries(), zkConfig.getMaxSleepTimeMilliseconds()))
                .namespace(zkConfig.getNamespace());
        if (0 != zkConfig.getSessionTimeoutMilliseconds()) {
            builder.sessionTimeoutMs(zkConfig.getSessionTimeoutMilliseconds());
            sessionTimeout = zkConfig.getSessionTimeoutMilliseconds();
        }
        if (0 != zkConfig.getConnectionTimeoutMilliseconds()) {
            builder.connectionTimeoutMs(zkConfig.getConnectionTimeoutMilliseconds());
        }
        if (!Strings.isNullOrEmpty(zkConfig.getDigest())) {
            builder.authorization("digest", zkConfig.getDigest().getBytes(Charset.forName("UTF-8")))
                   .aclProvider(new ACLProvider() {
                       
                       @Override
                       public List<ACL> getDefaultAcl() {
                           return ZooDefs.Ids.CREATOR_ALL_ACL;
                       }
                       
                       @Override
                       public List<ACL> getAclForPath(final String path) {
                           return ZooDefs.Ids.CREATOR_ALL_ACL;
                       }
                   });
        }
        client = builder.build();
		client.start();
        try {
        	client.getZookeeperClient().blockUntilConnectedOrTimedOut();
    		if (!client.getZookeeperClient().isConnected()) {
    			throw new Exception("the zk client is not connected");
    		}
        	client.checkExists().forPath(SLASH_CONSTNAT + zkConfig.getNamespace()); // check namespace node by using client, for UnknownHostException of connection string.         
        //CHECKSTYLE:OFF
        } catch (final Exception ex) {
        	throw new RuntimeException("zk connect fail, zkList is " + zkConfig.getServerLists(),ex);
        }

		// start monitor.
		if (zkConfig.getMonitorPort() > 0) {
			MonitorService monitorService = new MonitorService(this, zkConfig.getMonitorPort());
			monitorService.listen();
			log.info("msg=zk monitor port starts at {}. usage: telnet {jobServerIP} {} and execute dump {jobName}", zkConfig.getMonitorPort(), zkConfig.getMonitorPort());
		}
    }
    
    @Override
    public void close() {
        for (Entry<String, TreeCache> each : caches.entrySet()) {
            each.getValue().close();
            
        }
        waitForCacheClose();
        CloseableUtils.closeQuietly(client);
        if (zkConfig.isUseNestedZookeeper()) {
            NestedZookeeperServers.getInstance().closeServer(zkConfig.getNestedPort());
        }
    }
    
    /* TODO 等待500ms, cache先关闭再关闭client, 否则会抛异常
     * 因为异步处理, 可能会导致client先关闭而cache还未关闭结束.
     * 等待Curator新版本解决这个bug.
     * BUG地址：https://issues.apache.org/jira/browse/CURATOR-157
     */
    private void waitForCacheClose() {
        try {
            Thread.sleep(500L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public String get(final String key) {
        TreeCache cache = findTreeCache(key);
        if (null == cache) {
            return getDirectly(key);
        }
        ChildData resultIncache = cache.getCurrentData(key);
        if (null != resultIncache) {
            return null == resultIncache.getData() ? null : new String(resultIncache.getData(), Charset.forName("UTF-8"));
        }
        return null;
    }
    
    private TreeCache findTreeCache(final String key) {
        for (Entry<String, TreeCache> entry : caches.entrySet()) {
            if (key.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    @Override
    public String getDirectly(final String key) {
        try {
        	byte[] getZnodeData = client.getData().forPath(key);
        	if (getZnodeData == null) {
                return "";
            }
            return new String(getZnodeData, Charset.forName("UTF-8"));
        //CHECKSTYLE:OFF
        } catch (final Exception ex) {
        //CHECKSTYLE:ON
            RegExceptionHandler.handleException(ex);
            return null;
        }
    }
    
    @Override
    public List<String> getChildrenKeys(final String key) {
    	List<String> result = null;
		TreeCache cache = findTreeCache(key);
		if (null != cache) {
			Map<String, ChildData> resultIncache = cache.getCurrentChildren(key);
			if (null != resultIncache) {
				result = new ArrayList<String>();
				Set<String> names = resultIncache.keySet();
				result.addAll(names);
			}
		}

		try {
			if(result == null){
				result = client.getChildren().forPath(key);
			}		
			Collections.sort(result, new Comparator<String>() {

				@Override
				public int compare(final String o1, final String o2) {
					return o2.compareTo(o1);
				}
			});
			return result;
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			RegExceptionHandler.handleException(ex);
			return Collections.emptyList();
		}
    }
    
    @Override
    public boolean isExisted(final String key) {
		TreeCache cache = findTreeCache(key);
		if (null != cache) {
			ChildData dt = cache.getCurrentData(key);
			if(dt != null){
				return true;
			}
		}
		
		try {
			return null != client.checkExists().forPath(key);
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			RegExceptionHandler.handleException(ex);
			return false;
		}
    }
    
    @Override
    public void persist(final String key, final String value) {
        try {
            if (!isExisted(key)) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(key, value.getBytes());
            } else {
                update(key, value);
            }
        //CHECKSTYLE:OFF
        } catch (final Exception ex) {
        //CHECKSTYLE:ON
            RegExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public void update(final String key, final String value) {
        try {
            client.inTransaction().check().forPath(key).and().setData().forPath(key, value.getBytes(Charset.forName("UTF-8"))).and().commit();
        //CHECKSTYLE:OFF
        } catch (final Exception ex) {
        //CHECKSTYLE:ON
            RegExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public void persistEphemeral(final String key, final String value) {
        try {
            if (isExisted(key)) {
                client.delete().deletingChildrenIfNeeded().forPath(key);
            }
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(key, value.getBytes(Charset.forName("UTF-8")));
        //CHECKSTYLE:OFF
        } catch (final Exception ex) {
        //CHECKSTYLE:ON
            RegExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public void persistEphemeralSequential(final String key) {
        try {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(key);
        //CHECKSTYLE:OFF
        } catch (final Exception ex) {
        //CHECKSTYLE:ON
            RegExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public void remove(final String key) {
        try {
            client.delete().guaranteed().deletingChildrenIfNeeded().forPath(key);
        //CHECKSTYLE:OFF
        } catch (final Exception ex) {
        //CHECKSTYLE:ON
            RegExceptionHandler.handleException(ex);
        }
    }
    
    @Override
    public long getRegistryCenterTime(final String key) {
        long result = 0L;
        try {
            String path = client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(key);
            result = client.checkExists().forPath(path).getCtime();
        //CHECKSTYLE:OFF
        } catch (final Exception ex) {
        //CHECKSTYLE:ON
            RegExceptionHandler.handleException(ex);
        }
        Preconditions.checkState(0L != result, "Cannot get registry center time.");
        return result;
    }
    
    @Override
    public Object getRawClient() {
        return client;
    }
    
    /**
     * 注册数据监听器.
     * @param listener 连接状态监听器
     */
    @Override
    public void addTreeCacheListener(final TreeCacheListener listener, String jobName) {
    	String fullPath = JobNodePath.getJobNameFullPath(jobName);
    	TreeCache cache = getRawCache(fullPath);
    	if (cache == null) {
			log.error("[{}] msg=no tree cache for {}, add Listner {} failed.", jobName, fullPath, listener);
		} else {
            cache.getListenable().addListener(listener);
        }
    }
    @Override
    public void addCacheData(final String cachePath) {
    	final TreeCache cache = new TreeCache(client, cachePath);
        try {
            cache.start();
            log.info("msg=treecache for:{} started.", cachePath);
        //CHECKSTYLE:OFF
        } catch (final Exception ex) {
        //CHECKSTYLE:ON
            RegExceptionHandler.handleException(ex);
        }
        caches.put(cachePath + SLASH_CONSTNAT, cache);
    }
    
    /**
     * 清除本job注册的全部监听器
     */
    @Override
    public void closeTreeCache(final String cachePath){
    	TreeCache cache = getRawCache(cachePath);
    	if (cache != null) {
            cache.close();
            log.info("msg=treecache for:{} closed.", cachePath);
            caches.remove(cachePath + SLASH_CONSTNAT);
        }
    }
    @Override
    public TreeCache getRawCache(final String cachePath) {
        return caches.get(cachePath + SLASH_CONSTNAT);
    }
    
    @Override
    public void addConnectionStateListener(final ConnectionStateListener listener) {
        client.getConnectionStateListenable().addListener(listener);
    }
    
    @Override
    public void removeConnectionStateListener(final ConnectionStateListener listener) {
    	client.getConnectionStateListenable().removeListener(listener);
    }
    
    @Override
    public long getSessionTimeout() {
		return sessionTimeout;
	}

	@Override
	public String getNamespace() {
		return zkConfig.getNamespace();
	}


	@Override
	public boolean isConnected() {
		return client!=null && client.getZookeeperClient().isConnected();
	}
}
