/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. </p>
 */

package com.vip.saturn.job.console.utils;

public final class JobNodePath {

	public static final String $JOBS_NODE_NAME = "$Jobs";

	private JobNodePath() {
	}

	public static String getReportPath(String jobName) {
		return String.format("/%s/%s/control/report", $JOBS_NODE_NAME, jobName);
	}

	public static String getAnalyseResetPath(String jobName) {
		return String.format("/%s/%s/analyse/reset", $JOBS_NODE_NAME, jobName);
	}

	public static String getProcessCountPath(String jobName) {
		return String.format("/%s/%s/analyse/processCount", $JOBS_NODE_NAME, jobName);
	}

	public static String getErrorCountPath(String jobName) {
		return String.format("/%s/%s/analyse/errorCount", $JOBS_NODE_NAME, jobName);
	}

	public static String get$JobsNodePath() {
		return "/" + $JOBS_NODE_NAME;
	}

	public static String getJobNodePath(String jobName) {
		return String.format("/%s/%s", $JOBS_NODE_NAME, jobName);
	}

	public static String getConfigNodePath(final String jobName) {
		return String.format("/%s/%s/config", $JOBS_NODE_NAME, jobName);
	}

	public static String getConfigNodePath(final String jobName, final String nodeName) {
		return String.format("/%s/%s/config/%s", $JOBS_NODE_NAME, jobName, nodeName);
	}

	public static String getServerNodePath(final String jobName) {
		return String.format("/%s/%s/servers", $JOBS_NODE_NAME, jobName);
	}

	public static String getServerNodePath(final String jobName, final String executorName) {
		return String.format("/%s/%s/servers/%s", $JOBS_NODE_NAME, jobName, executorName);
	}

	public static String getServerNodePath(final String jobName, final String executorName, final String nodeName) {
		return String.format("%s/%s/%s", getServerNodePath(jobName), executorName, nodeName);
	}

	public static String getExecutionNodePath(final String jobName) {
		return String.format("/%s/%s/execution", $JOBS_NODE_NAME, jobName);
	}

	public static String getExecutionItemNodePath(final String jobName, final String item) {
		return String.format("/%s/%s/execution/%s", $JOBS_NODE_NAME, jobName, item);
	}

	public static String getItemNextFireTime(final String jobName, final String item) {
		return String.format("/%s/%s/execution/%s/%s", $JOBS_NODE_NAME, jobName, item, "nextFireTime");
	}

	public static String getExecutionNodePath(final String jobName, final String item, final String nodeName) {
		return String.format("%s/%s/%s", getExecutionNodePath(jobName), item, nodeName);
	}

	public static String getLeaderNodePath(final String jobName, final String nodeName) {
		return String.format("/%s/%s/leader/%s", $JOBS_NODE_NAME, jobName, nodeName);
	}

	public static String getRunOneTimePath(final String jobName, final String executorName) {
		return String.format("/%s/%s/servers/%s/runOneTime", $JOBS_NODE_NAME, jobName, executorName);
	}

	public static String getStopOneTimePath(final String jobName, final String executorName) {
		return String.format("/%s/%s/servers/%s/stopOneTime", $JOBS_NODE_NAME, jobName, executorName);
	}

	public static String getServerStatus(final String jobName, final String executorName) {
		return String.format("/%s/%s/servers/%s/status", $JOBS_NODE_NAME, jobName, executorName);
	}

	public static String getServerStoppedFlag(final String jobName, final String executorName) {
		return String.format("/%s/%s/servers/%s/stoped", $JOBS_NODE_NAME, jobName, executorName);
	}

	public static String getProcessSucessCount(final String jobName, final String executorName) {
		return String.format("/%s/%s/servers/%s/processSuccessCount", $JOBS_NODE_NAME, jobName, executorName);
	}

	public static String getProcessFailureCount(final String jobName, final String executorName) {
		return String.format("/%s/%s/servers/%s/processFailureCount", $JOBS_NODE_NAME, jobName, executorName);
	}

	public static String getServerSharding(final String jobName, final String executorName) {
		return String.format("/%s/%s/servers/%s/sharding", $JOBS_NODE_NAME, jobName, executorName);
	}

	public static String getFailoverNodePath(final String jobName, final String item) {
		return JobNodePath.getExecutionNodePath(jobName, item, "failover");
	}

	public static String getRunningNodePath(final String jobName, final String item) {
		return JobNodePath.getExecutionNodePath(jobName, item, "running");
	}

	public static String getCompletedNodePath(final String jobName, final String item) {
		return JobNodePath.getExecutionNodePath(jobName, item, "completed");
	}

	public static String getFailedNodePath(final String jobName, final String item) {
		return JobNodePath.getExecutionNodePath(jobName, item, "failed");
	}

	public static String getTimeoutNodePath(final String jobName, final String item) {
		return JobNodePath.getExecutionNodePath(jobName, item, "timeout");
	}

	public static String getEnabledReportNodePath(final String jobName) {
		return JobNodePath.getConfigNodePath(jobName, "enabledReport");
	}

}
