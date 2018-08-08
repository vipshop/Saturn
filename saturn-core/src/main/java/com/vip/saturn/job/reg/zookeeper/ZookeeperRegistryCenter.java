/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at <p>
 * http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License. </p>
 */

package com.vip.saturn.job.reg.zookeeper;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.vip.saturn.job.constant.Constant;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.reg.exception.RegExceptionHandler;
import com.vip.saturn.job.sharding.utils.CuratorUtils;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 基于Zookeeper的注册中心.
 */
public class ZookeeperRegistryCenter implements CoordinatorRegistryCenter {

	static Logger log = LoggerFactory.getLogger(ZookeeperRegistryCenter.class);

	private static final String SLASH_CONSTNAT = "/";

	private ZookeeperConfiguration zkConfig;

	private CuratorFramework client;

	/**
	 * 最小连接超时时间
	 */
	private static final int MIN_CONNECTION_TIMEOUT = 20 * 1000;

	/**
	 * 最小会话超时时间
	 */
	private static final int MIN_SESSION_TIMEOUT = 20 * 1000;

	/**
	 * 会话超时时间
	 */
	private int sessionTimeout;

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
		client = buildZkClient();
		client.start();

		try {
			client.getZookeeperClient().blockUntilConnectedOrTimedOut();
			if (!client.getZookeeperClient().isConnected()) {
				throw new RuntimeException("the zk client is not connected while reach connection timeout");
			}

			// check namespace node by using client, for UnknownHostException of connection string.
			client.checkExists().forPath(SLASH_CONSTNAT + zkConfig.getNamespace());
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			throw new RuntimeException("zk connect fail, zkList is " + zkConfig.getServerLists(), ex);
		}

		log.info("zkClient is created successfully.");
	}

	private CuratorFramework buildZkClient() {
		if (zkConfig.isUseNestedZookeeper()) {
			NestedZookeeperServers.getInstance()
					.startServerIfNotStarted(zkConfig.getNestedPort(), zkConfig.getNestedDataDir());
		}

		Builder builder = CuratorFrameworkFactory.builder().connectString(zkConfig.getServerLists()).retryPolicy(
				new ExponentialBackoffRetry(zkConfig.getBaseSleepTimeMilliseconds(), zkConfig.getMaxRetries(),
						zkConfig.getMaxSleepTimeMilliseconds())).namespace(zkConfig.getNamespace());

		if (0 != zkConfig.getSessionTimeoutMilliseconds()) {
			sessionTimeout = zkConfig.getSessionTimeoutMilliseconds();
		} else {
			sessionTimeout = calculateSessionTimeout();
		}
		builder.sessionTimeoutMs(sessionTimeout);

		int connectionTimeout;
		if (0 != zkConfig.getConnectionTimeoutMilliseconds()) {
			connectionTimeout = zkConfig.getConnectionTimeoutMilliseconds();
		} else {
			connectionTimeout = calculateConnectionTimeout();
		}
		builder.connectionTimeoutMs(connectionTimeout);

		if (!Strings.isNullOrEmpty(zkConfig.getDigest())) {
			builder.authorization("digest", zkConfig.getDigest().getBytes(Charset.forName(Constant.CHARSET_UTF8)))
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

		log.info(
				"msg=Saturn job: zookeeper registry center init, server lists is: {}, connection_timeout: {}, session_timeout: {}, retry_times: {}",
				zkConfig.getServerLists(), connectionTimeout, sessionTimeout, zkConfig.getMaxRetries());
		return builder.build();
	}

	private static int calculateConnectionTimeout() {
		// default SystemEnvProperties.VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT_IN_SECONDS = -1
		int connectionTimeoutInMillSeconds = 0;
		if (SystemEnvProperties.VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT_IN_SECONDS != -1) {
			connectionTimeoutInMillSeconds =
					SystemEnvProperties.VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT_IN_SECONDS * 1000;
		} else if (SystemEnvProperties.VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING) {
			// 60秒为默认不稳定网络下的设置
			connectionTimeoutInMillSeconds =
					SystemEnvProperties.VIP_SATURN_CONNECTION_TIMEOUT_IN_SECONDS_IN_UNSTABLE_NETWORK * 1000;
		}

		return connectionTimeoutInMillSeconds > MIN_CONNECTION_TIMEOUT ?
				connectionTimeoutInMillSeconds :
				MIN_CONNECTION_TIMEOUT;
	}

	private static int calculateSessionTimeout() {
		// default SystemEnvProperties.VIP_SATURN_ZK_CLIENT_SESSION_TIMEOUT_IN_SECONDS = -1
		int sessionTimeoutInMillSeconds = 0;
		if (SystemEnvProperties.VIP_SATURN_ZK_CLIENT_SESSION_TIMEOUT_IN_SECONDS != -1) {
			sessionTimeoutInMillSeconds = SystemEnvProperties.VIP_SATURN_ZK_CLIENT_SESSION_TIMEOUT_IN_SECONDS * 1000;
		} else if (SystemEnvProperties.VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING) {
			// 60秒为默认不稳定网络下的设置
			sessionTimeoutInMillSeconds =
					SystemEnvProperties.VIP_SATURN_SESSION_TIMEOUT_IN_SECONDS_IN_UNSTABLE_NETWORK * 1000;
		}

		return sessionTimeoutInMillSeconds > MIN_SESSION_TIMEOUT ? sessionTimeoutInMillSeconds : MIN_SESSION_TIMEOUT;
	}

	@Override
	public void close() {

		CloseableUtils.closeQuietly(client);
		if (zkConfig.isUseNestedZookeeper()) {
			NestedZookeeperServers.getInstance().closeServer(zkConfig.getNestedPort());
		}
	}

	@Override
	public String get(final String key) {
		return getDirectly(key);
	}

	@Override
	public String getDirectly(final String key) {
		try {
			byte[] getZnodeData = client.getData().forPath(key);
			if (getZnodeData == null) {
				return "";
			}
			return new String(getZnodeData, Charset.forName(Constant.CHARSET_UTF8));
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			RegExceptionHandler.handleException(ex);
			return null;
		}
	}

	@Override
	public List<String> getChildrenKeys(final String key) {
		List<String> result = null;
		try {
			result = client.getChildren().forPath(key);
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
				client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT)
						.forPath(key, value.getBytes());
			} else {
				update(key, value);
			}
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			RegExceptionHandler.handleException(ex);
		}
	}

	@Override
	public void update(final String key, final String value) {
		try {
			client.inTransaction().check().forPath(key).and().setData()
					.forPath(key, value.getBytes(Charset.forName(Constant.CHARSET_UTF8))).and().commit();
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			RegExceptionHandler.handleException(ex);
		}
	}

	@Override
	public void persistEphemeral(final String key, final String value) {
		try {
			if (isExisted(key)) {
				client.delete().deletingChildrenIfNeeded().forPath(key);
			}
			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
					.forPath(key, value.getBytes(Charset.forName(Constant.CHARSET_UTF8)));
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			RegExceptionHandler.handleException(ex);
		}
	}

	@Override
	public void persistEphemeralSequential(final String key) {
		try {
			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(key);
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			RegExceptionHandler.handleException(ex);
		}
	}

	@Override
	public void remove(final String key) {
		try {
			client.delete().guaranteed().forPath(key);
		} catch (KeeperException.NotEmptyException e) {
			log.debug("try to delete path:" + key + " but fail for NotEmptyException", e);
			deleteChildrenIfNeeded(key);
		} catch (KeeperException.NoNodeException e) {
			log.debug("try to delete path:" + key + " but fail for NoNodeException", e);
		} catch (final Exception ex) {
			RegExceptionHandler.handleException(ex);
		}
	}

	private void deleteChildrenIfNeeded(String key) {
		try {
			CuratorUtils.deletingChildrenIfNeeded(client, key);
		} catch (Exception e1) {
			RegExceptionHandler.handleException(e1);
		}
	}

	@Override
	public long getRegistryCenterTime(final String key) {
		long result = 0L;
		try {
			String path = client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
					.forPath(key);
			result = client.checkExists().forPath(path).getCtime();
		} catch (final Exception ex) {
			RegExceptionHandler.handleException(ex);
		}
		Preconditions.checkState(0L != result, "Cannot get registry center time.");
		return result;
	}

	@Override
	public Object getRawClient() {
		return client;
	}

	public void setClient(CuratorFramework client) {
		this.client = client;
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

	public void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	@Override
	public String getNamespace() {
		return zkConfig.getNamespace();
	}

	@Override
	public boolean isConnected() {
		return client != null && client.getZookeeperClient().isConnected();
	}

	@Override
	public CoordinatorRegistryCenter usingNamespace(String namespace) {
		// If usingNamespace is frequently-used, consider cache it
		ZookeeperConfiguration zkConfigNew = new ZookeeperConfiguration(zkConfig.getServerLists(), namespace,
				zkConfig.getBaseSleepTimeMilliseconds(), zkConfig.getMaxSleepTimeMilliseconds(),
				zkConfig.getMaxRetries());
		ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter(zkConfigNew);
		zookeeperRegistryCenter.setExecutorName(executorName);
		zookeeperRegistryCenter.setClient(client.usingNamespace(namespace));
		zookeeperRegistryCenter.setSessionTimeout(sessionTimeout);
		return zookeeperRegistryCenter;
	}

}
