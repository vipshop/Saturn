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
