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

package com.vip.saturn.job.internal.storage;

/**
 * 作业节点路径类.
 * 
 * <p>
 * 作业节点是在普通的节点前加上作业名称的前缀.
 * </p>
 * 
 * 
 */
public final class JobNodePath {

	public static final String JOBS_NODE_NAME = "$Jobs";

	public static final String ROOT = "/" + JOBS_NODE_NAME;

	/**
	 * 获取作业节点全路径
	 * @return 作业节点全路径
	 */
	public static String getJobNameFullPath(String jn) {
		return String.format("/%s/%s", JOBS_NODE_NAME, jn);
	}

	public static String getNodeFullPath(final String jobName, final String node) {
		return String.format("/%s/%s/%s", JOBS_NODE_NAME, jobName, node);
	}

	public static String getConfigNodePath(final String jobName, final String nodeName) {
		return String.format("/%s/%s/config/%s", JOBS_NODE_NAME, jobName, nodeName);
	}

	public static String getServerNodePath(final String jobName, final String executorName) {
		return String.format("/%s/%s/servers/%s", JOBS_NODE_NAME, jobName, executorName);
	}

}
