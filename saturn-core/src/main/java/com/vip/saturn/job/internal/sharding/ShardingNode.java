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

package com.vip.saturn.job.internal.sharding;

import com.vip.saturn.job.internal.election.ElectionNode;
import com.vip.saturn.job.internal.server.ServerNode;
import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * Saturn分片节点名称的常量类.
 * 
 * 
 */
public final class ShardingNode {

	public static final String LEADER_SHARDING_ROOT = ElectionNode.ROOT + "/sharding";

	public static final String NECESSARY = LEADER_SHARDING_ROOT + "/necessary";

	public static final String PROCESSING = LEADER_SHARDING_ROOT + "/processing";

	private static final String SERVER_SHARDING = ServerNode.ROOT + "/%s/sharding";

	private final String jobName;

	public ShardingNode(String jobName) {
		this.jobName = jobName;
	}

	public static String getShardingNode(final String executorName) {
		return String.format(SERVER_SHARDING, executorName);
	}

	/**
	 * 判断是否为需要重新做sharding的Path
	 * 
	 * @param path 节点路径
	 * @return 判断是否为需要重新做sharding的Path
	 */
	public boolean isShardingNecessaryPath(final String path) {
		return JobNodePath.getNodeFullPath(jobName, NECESSARY).equals(path);
	}
}
