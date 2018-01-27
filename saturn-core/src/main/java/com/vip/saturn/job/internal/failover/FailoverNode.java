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

package com.vip.saturn.job.internal.failover;

import com.vip.saturn.job.internal.election.ElectionNode;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * Saturn失效转移节点名称的常量类.
 * 
 * 
 */
public final class FailoverNode {

	static final String FAILOVER = "failover";

	static final String LEADER_ROOT = ElectionNode.ROOT + "/" + FAILOVER;

	static final String ITEMS_ROOT = LEADER_ROOT + "/items";

	static final String ITEMS = ITEMS_ROOT + "/%s";

	static final String LATCH = LEADER_ROOT + "/latch";

	private static final String EXECUTION_FAILOVER = ExecutionNode.ROOT + "/%s/" + FAILOVER;

	private final String jobName;

	public FailoverNode(final String jobName) {
		this.jobName = jobName;
	}

	static String getItemsNode(final int item) {
		return String.format(ITEMS, item);
	}

	public static String getFailoverItemsNode() {
		return ITEMS_ROOT;
	}

	public static String getExecutionFailoverNode(final int item) {
		return String.format(EXECUTION_FAILOVER, item);
	}

	/**
	 * 根据失效转移执行路径获取分片项.
	 * 
	 * @param path 失效转移执行路径
	 * @return 分片项, 不是失效转移执行路径获则返回null
	 */
	public Integer getItemByExecutionFailoverPath(final String path) {
		if (!isFailoverPath(path)) {
			return null;
		}
		return Integer.valueOf(path.substring(JobNodePath.getNodeFullPath(jobName, ExecutionNode.ROOT).length() + 1,
				path.lastIndexOf(FailoverNode.FAILOVER) - 1));
	}

	private boolean isFailoverPath(final String path) {
		return path.startsWith(JobNodePath.getNodeFullPath(jobName, ExecutionNode.ROOT))
				&& path.endsWith(FailoverNode.FAILOVER);
	}
}
