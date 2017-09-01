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

package com.vip.saturn.job.internal.election;

import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * Saturn主服务器根节点名称的常量类.
 * 
 * 
 */
public final class ElectionNode {

	/**
	 * Saturn主服务器根节点.
	 */
	public static final String ROOT = "leader";

	static final String ELECTION_ROOT = ROOT + "/election";

	public static final String LEADER_HOST = ELECTION_ROOT + "/host";

	static final String LATCH = ELECTION_ROOT + "/latch";

	private final String jobName;

	ElectionNode(final String jobName) {
		this.jobName = jobName;
	}

	boolean isLeaderHostPath(final String path) {
		return JobNodePath.getNodeFullPath(jobName, LEADER_HOST).equals(path);
	}

}
