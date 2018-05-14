/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.reg.zookeeper;

import com.google.common.base.Strings;
import com.vip.saturn.job.reg.base.AbstractRegistryCenterConfiguration;
import com.vip.saturn.job.utils.SystemEnvProperties;

/**
 * 基于Zookeeper的注册中心配置.
 * 
 * 
 */
public class ZookeeperConfiguration extends AbstractRegistryCenterConfiguration {

	public static final int MIN_CLIENT_RETRY_TIMES = 3;

	/**
	 * 连接Zookeeper服务器的列表. 包括IP地址和端口号. 多个地址用逗号分隔. 如: host1:2181,host2:2181
	 */
	private String serverLists;

	/**
	 * 命名空间.
	 */
	private String namespace;

	/**
	 * 等待重试的间隔时间的初始值. 单位毫秒.
	 */
	private int baseSleepTimeMilliseconds;

	/**
	 * 等待重试的间隔时间的最大值. 单位毫秒.
	 */
	private int maxSleepTimeMilliseconds;

	/**
	 * 最大重试次数.
	 */
	private int maxRetries;

	/**
	 * 会话超时时间. 单位毫秒.
	 */
	private int sessionTimeoutMilliseconds;

	/**
	 * 连接超时时间. 单位毫秒.
	 */
	private int connectionTimeoutMilliseconds;

	/**
	 * 连接Zookeeper的权限令牌. 缺省为不需要权限验证.
	 */
	private String digest;

	/**
	 * 内嵌Zookeeper的端口号. -1表示不开启内嵌Zookeeper.
	 */
	private int nestedPort = -1;

	/**
	 * 内嵌Zookeeper的数据存储路径. 为空表示不开启内嵌Zookeeper.
	 */
	private String nestedDataDir;

	public ZookeeperConfiguration() {

	}

	/**
	 * 包含了必需属性的构造器.
	 * 
	 * @param serverLists 连接Zookeeper服务器的列表
	 * @param namespace 命名空间
	 * @param baseSleepTimeMilliseconds 等待重试的间隔时间的初始值
	 * @param maxSleepTimeMilliseconds 等待重试的间隔时间的最大值
	 * @param maxRetries 最大重试次数
	 */
	public ZookeeperConfiguration(final String serverLists, final String namespace, final int baseSleepTimeMilliseconds,
			final int maxSleepTimeMilliseconds, final int maxRetries) {
		this.serverLists = serverLists;
		this.namespace = namespace;
		this.baseSleepTimeMilliseconds = baseSleepTimeMilliseconds;
		this.maxSleepTimeMilliseconds = maxSleepTimeMilliseconds;
		this.maxRetries = maxRetries;
	}

	public ZookeeperConfiguration(final String serverLists, final String namespace, final int baseSleepTimeMilliseconds,
			final int maxSleepTimeMilliseconds) {
		this.serverLists = serverLists;
		this.namespace = namespace;
		this.baseSleepTimeMilliseconds = baseSleepTimeMilliseconds;
		this.maxSleepTimeMilliseconds = maxSleepTimeMilliseconds;
		this.maxRetries = calculateRetryTimes();
	}

	private int calculateRetryTimes() {
		int retryTimes = -1;
		if (SystemEnvProperties.VIP_SATURN_ZK_CLIENT_RETRY_TIMES != -1) {
			retryTimes = SystemEnvProperties.VIP_SATURN_ZK_CLIENT_RETRY_TIMES;
		} else if (SystemEnvProperties.VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING) {
			retryTimes = SystemEnvProperties.VIP_SATURN_RETRY_TIMES_IN_UNSTABLE_NETWORK;
		}

		return retryTimes > ZookeeperConfiguration.MIN_CLIENT_RETRY_TIMES ?
				retryTimes :
				ZookeeperConfiguration.MIN_CLIENT_RETRY_TIMES;
	}

	/**
	 * 判断是否需要开启内嵌Zookeeper.
	 * 
	 * @return 是否需要开启内嵌Zookeeper
	 */
	public boolean isUseNestedZookeeper() {
		return -1 != nestedPort && !Strings.isNullOrEmpty(nestedDataDir);
	}

	public String getServerLists() {
		return serverLists;
	}

	public void setServerLists(String serverLists) {
		this.serverLists = serverLists;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public int getBaseSleepTimeMilliseconds() {
		return baseSleepTimeMilliseconds;
	}

	public void setBaseSleepTimeMilliseconds(int baseSleepTimeMilliseconds) {
		this.baseSleepTimeMilliseconds = baseSleepTimeMilliseconds;
	}

	public int getMaxSleepTimeMilliseconds() {
		return maxSleepTimeMilliseconds;
	}

	public void setMaxSleepTimeMilliseconds(int maxSleepTimeMilliseconds) {
		this.maxSleepTimeMilliseconds = maxSleepTimeMilliseconds;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public int getSessionTimeoutMilliseconds() {
		return sessionTimeoutMilliseconds;
	}

	public void setSessionTimeoutMilliseconds(int sessionTimeoutMilliseconds) {
		this.sessionTimeoutMilliseconds = sessionTimeoutMilliseconds;
	}

	public int getConnectionTimeoutMilliseconds() {
		return connectionTimeoutMilliseconds;
	}

	public void setConnectionTimeoutMilliseconds(int connectionTimeoutMilliseconds) {
		this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
	}

	public String getDigest() {
		return digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public int getNestedPort() {
		return nestedPort;
	}

	public void setNestedPort(int nestedPort) {
		this.nestedPort = nestedPort;
	}

	public String getNestedDataDir() {
		return nestedDataDir;
	}

	public void setNestedDataDir(String nestedDataDir) {
		this.nestedDataDir = nestedDataDir;
	}

}
