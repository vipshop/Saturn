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

package com.vip.saturn.job.internal.server;

import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * Saturn服务器节点名称的常量类.
 * @author dylan.xue
 */
public final class ServerNode {

	/**
	 * 作业服务器信息根节点.
	 */
	public static final String ROOT = "servers";

	public static final String IP = ROOT + "/%s/ip";

	public static final String STATUS_APPENDIX = "status";

	public static final String STATUS = ROOT + "/%s/" + STATUS_APPENDIX;

	public static final String PROCESS_SUCCESS_COUNT = ROOT + "/%s/processSuccessCount";

	public static final String PROCESS_FAILURE_COUNT = ROOT + "/%s/processFailureCount";

	/** server 版本 **/
	public static final String VERSION = ROOT + "/%s/version";

	/** server 版本 **/
	public static final String JOB_VERSION = ROOT + "/%s/jobVersion";

	/** 对应的Executor运行节点 */
	public static final String SERVER = ROOT + "/%s";

	public static final String RUNONETIME = ROOT + "/%s/runOneTime";

	public static final String STOPONETIME = ROOT + "/%s/stopOneTime";

	private ServerNode() {
	}

	static String getVersionNode(String executorName) {
		return String.format(VERSION, executorName);
	}

	static String getJobVersionNode(String executorName) {
		return String.format(JOB_VERSION, executorName);
	}

	static String getIpNode(String executorName) {
		return String.format(IP, executorName);
	}

	static String getStatusNode(String executorName) {
		return String.format(STATUS, executorName);
	}

	static String getProcessSuccessCountNode(String executorName) {
		return String.format(PROCESS_SUCCESS_COUNT, executorName);
	}

	static String getProcessFailureCountNode(String executorName) {
		return String.format(PROCESS_FAILURE_COUNT, executorName);
	}

	static String getRunOneTimePath(String executorName) {
		return String.format(ServerNode.RUNONETIME, executorName);
	}

	static String getStopOneTimePath(String executorName) {
		return String.format(ServerNode.STOPONETIME, executorName);
	}

	/** 对应的Executor运行节点 */
	static String getServerNodePath(String executorName) {
		return String.format(SERVER, executorName);
	}

	/**
	 * 判断给定路径是否为作业服务器状态路径.
	 * 
	 * @param path 待判断的路径
	 * @return 是否为作业服务器状态路径
	 */
	public static boolean isServerStatusPath(final String jobName, final String path) {
		return path.startsWith(JobNodePath.getNodeFullPath(jobName, ServerNode.ROOT))
				&& path.endsWith(ServerNode.STATUS_APPENDIX);
	}

	/**
	 * @return 运行态的server的ZK节点路径
	 */
	public static String getServerNode(final String jobName, String executorName) {
		return JobNodePath.getNodeFullPath(jobName, getServerNodePath(executorName));
	}

	public static String getServerRoot(final String jobName) {
		return JobNodePath.getNodeFullPath(jobName, ROOT);
	}

	public static boolean isRunOneTimePath(final String jobName, String path, String executorName) {
		return path
				.startsWith(JobNodePath.getNodeFullPath(jobName, String.format(ServerNode.RUNONETIME, executorName)));
	}

	public static boolean isStopOneTimePath(final String jobName, String path, String executorName) {
		return path
				.startsWith(JobNodePath.getNodeFullPath(jobName, String.format(ServerNode.STOPONETIME, executorName)));
	}
}
