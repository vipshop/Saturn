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

package com.vip.saturn.job.console.utils;

/**
 * @deprecated useless for now
 * @author yangjuanying
 */
@Deprecated
public class ContainerNodePath {

	public static final String $CONTAINERS_NODE_NAME = "$DCOS";

	private ContainerNodePath() {
	}

	public static String get$ContainerNodePath() {
		return "/" + $CONTAINERS_NODE_NAME;
	}

	public static String getDcosTasksNodePath() {
		return String.format("/%s/tasks", $CONTAINERS_NODE_NAME);
	}

	public static String getDcosTaskIdNodePath(final String taskId) {
		return String.format("%s/%s", getDcosTasksNodePath(), taskId);
	}

	public static String getDcosTaskConfigNodePath(final String taskId) {
		return String.format("%s/config", getDcosTaskIdNodePath(taskId));
	}

	public static String getDcosTaskScaleJobsNodePath(final String taskId) {
		return String.format("%s/scaleJobs", getDcosTaskIdNodePath(taskId));
	}

	public static String getDcosTaskScaleJobNodePath(final String taskId, final String jobName) {
		return String.format("%s/scaleJobs/%s", getDcosTaskIdNodePath(taskId), jobName);
	}

	public static String getDcosConfigTokenNodePath() {
		return String.format("%s/config/token", get$ContainerNodePath());
	}
}
