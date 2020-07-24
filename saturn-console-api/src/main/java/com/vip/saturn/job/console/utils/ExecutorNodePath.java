/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */
package com.vip.saturn.job.console.utils;

public class ExecutorNodePath {

	public static final String $EXECUTOR_NODE_NAME = "$SaturnExecutors";

	public static final String SHARDING_NODE_NAME = "/" + $EXECUTOR_NODE_NAME + "/sharding";

	public static final String SHARDING_COUNT_PATH = "/" + $EXECUTOR_NODE_NAME + "/sharding/count";

	private ExecutorNodePath() {
	}

	public static String get$ExecutorNodePath() {
		return "/" + $EXECUTOR_NODE_NAME;
	}

	public static String getExecutorNodePath() {
		return String.format("/%s/executors", $EXECUTOR_NODE_NAME);
	}

	public static String get$ExecutorTaskNodePath(String executorName) {
		return String.format("/%s/executors/%s/task", $EXECUTOR_NODE_NAME, executorName);
	}

	public static String getExecutorNodePath(final String executorName) {
		return String.format("%s/%s", getExecutorNodePath(), executorName);
	}

	public static String getExecutorNodePath(final String executorName, final String nodeName) {
		return String.format("%s/%s/%s", getExecutorNodePath(), executorName, nodeName);
	}

	public static String getExecutorTaskNodePath(final String executorName) {
		return getExecutorNodePath(executorName, "task");
	}

	public static String getExecutorNoTrafficNodePath(final String executorName) {
		return getExecutorNodePath(executorName, "noTraffic");
	}

	public static String getExecutorIpNodePath(final String executorName) {
		return getExecutorNodePath(executorName, "ip");
	}

	public static String getExecutorVersionNodePath(final String executorName) {
		return getExecutorNodePath(executorName, "version");
	}

	public static String getExecutorShardingNodePath(final String nodeName) {
		return String.format("%s/%s", SHARDING_NODE_NAME, nodeName);
	}

	public static String getExecutorDumpNodePath(final String executorName) {
		return getExecutorNodePath(executorName, "dump");
	}

	public static String getExecutorRestartNodePath(final String executorName) {
		return getExecutorNodePath(executorName, "restart");
	}

}
