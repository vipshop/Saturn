/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.console;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiaopeng.he
 */
public final class SaturnEnvProperties {

	/**
	 * 指定Console在进行sharding计算和dashbaord统计等计算的服务器集群标识
	 */
	public static String NAME_VIP_SATURN_CONSOLE_CLUSTER = "VIP_SATURN_CONSOLE_CLUSTER";
	public static String VIP_SATURN_CONSOLE_CLUSTER_ID = StringUtils
			.trim(System.getProperty(NAME_VIP_SATURN_CONSOLE_CLUSTER, System.getenv(NAME_VIP_SATURN_CONSOLE_CLUSTER)));
	/**
	 * zk注册中心
	 */
	public static String NAME_VIP_SATURN_ZK_CONNECTION = "VIP_SATURN_ZK_CONNECTION";
	public static String CONTAINER_TYPE = System.getProperty("VIP_SATURN_CONTAINER_TYPE",
			System.getenv("VIP_SATURN_CONTAINER_TYPE"));
	public static String VIP_SATURN_DCOS_REST_URI = System.getProperty("VIP_SATURN_DCOS_REST_URI",
			System.getenv("VIP_SATURN_DCOS_REST_URI"));
	public static String VIP_SATURN_DCOS_REGISTRY_URI = System.getProperty("VIP_SATURN_DCOS_REGISTRY_URI",
			System.getenv("VIP_SATURN_DCOS_REGISTRY_URI"));
	public static String NAME_VIP_SATURN_EXECUTOR_CLEAN = "VIP_SATURN_EXECUTOR_CLEAN";
	public static String NAME_VIP_SATURN_DCOS_TASK = "VIP_SATURN_DCOS_TASK";
	protected static Logger log = LoggerFactory.getLogger(SaturnEnvProperties.class);

	static {
		if (CONTAINER_TYPE == null) {
			CONTAINER_TYPE = "MARATHON";
		}
	}

	private SaturnEnvProperties() {
	}

}
