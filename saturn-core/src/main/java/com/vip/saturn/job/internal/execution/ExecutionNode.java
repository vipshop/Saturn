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

package com.vip.saturn.job.internal.execution;

import com.vip.saturn.job.internal.election.ElectionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * Saturn执行状态节点名称的常量类.
 * 
 * @author dylan
 */
public final class ExecutionNode {

	/**
	 * 执行状态根节点.
	 */
	public static final String ROOT = "execution";

	public static final String RUNNING_APPENDIX = "running";

	public static final String RUNNING = ROOT + "/%s/" + RUNNING_APPENDIX;

	public static final String COMPLETED = ROOT + "/%s/completed";

	/** 作业执行execution节点 */
	public static final String EXECUTION_NODE = ROOT + "/%s";

	/** 执行失败 */
	public static final String FAILED = ROOT + "/%s/failed";
	/** 执行超时 */
	public static final String TIMEOUT = ROOT + "/%s/timeout";

	/** 作业执行返回信息 */
	public static final String JOB_MSG = ROOT + "/%s/jobMsg";

	/** 作业日志信息 */
	public static final String JOB_LOG = ROOT + "/%s/jobLog";

	public static final String LAST_BEGIN_TIME = ROOT + "/%s/lastBeginTime";

	public static final String NEXT_FIRE_TIME = ROOT + "/%s/nextFireTime";

	public static final String LAST_COMPLETE_TIME = ROOT + "/%s/lastCompleteTime";

	public static final String LEADER_ROOT = ElectionNode.ROOT + "/" + ROOT;

	private final String jobName;

	public ExecutionNode(final String jobName) {
		this.jobName = jobName;
	}

	/**
	 * 获取作业运行状态节点路径.
	 * 
	 * @param item 作业项
	 * @return 作业运行状态节点路径
	 */
	public static String getRunningNode(final int item) {
		return String.format(RUNNING, item);
	}

	public static String getCompletedNode(final int item) {
		return String.format(COMPLETED, item);
	}

	/** 执行出错节点 */
	public static String getFailedNode(final int item) {
		return String.format(FAILED, item);
	}

	/** 执行超时节点 */
	public static String getTimeoutNode(final int item) {
		return String.format(TIMEOUT, item);
	}

	/** 作业执行返回信息 */
	public static String getJobMsg(final int item) {
		return String.format(JOB_MSG, item);
	}

	/** 作业运行日志 */
	public static String getJobLog(final int item) {
		return String.format(JOB_LOG, item);
	}

	public static String getLastBeginTimeNode(final int item) {
		return String.format(LAST_BEGIN_TIME, item);
	}

	public static String getNextFireTimeNode(final int item) {
		return String.format(NEXT_FIRE_TIME, item);
	}

	public static String getLastCompleteTimeNode(final int item) {
		return String.format(LAST_COMPLETE_TIME, item);
	}

	public static String getExecutionNode(final String item) {
		return String.format(EXECUTION_NODE, item);
	}

	/**
	 * 根据运行中的分片路径获取分片项.
	 * 
	 * @param path 运行中的分片路径
	 * @return 分片项, 不是运行中的分片路径获则返回null
	 */
	public Integer getItemByRunningItemPath(final String path) {
		if (!isRunningItemPath(path)) {
			return null;
		}
		return Integer.valueOf(path.substring(JobNodePath.getNodeFullPath(jobName, ROOT).length() + 1,
				path.lastIndexOf(RUNNING_APPENDIX) - 1));
	}

	private boolean isRunningItemPath(final String path) {
		return path.startsWith(JobNodePath.getNodeFullPath(jobName, ROOT)) && path.endsWith(RUNNING_APPENDIX);
	}

}
