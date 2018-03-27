package com.vip.saturn.job.sharding.node;

/**
 * @author xiaopeng.he
 */
public class SaturnExecutorsNode {

	public static final String JOBS_NODE = "$Jobs";
	private static final String SATURN_EXECUTORS_NODE = "$SaturnExecutors";
	private static final String EXECUTORS = "executors";
	private static final String HOST = "host";
	private static final String LEADER = "leader";
	private static final String LATCH = "latch";
	private static final String SHARDING = "sharding";
	private static final String CONTENT = "content";
	private static final String IP = "ip";
	private static final String NO_TRAFFIC = "noTraffic";
	private static final String CLEAN = "clean";
	private static final String TASK = "task";
	private static final String DCOS_NODE = "$DCOS";
	private static final String TASKS = "tasks";
	public static final String SHARDING_COUNT_PATH = "/" + SATURN_EXECUTORS_NODE + "/" + SHARDING + "/" + "count";
	public static final String LEADER_HOSTNODE_PATH = "/" + SATURN_EXECUTORS_NODE + "/" + LEADER + "/" + HOST;
	public static final String LEADERNODE_PATH = "/" + SATURN_EXECUTORS_NODE + "/" + LEADER;
	public static final String EXECUTORSNODE_PATH = "/" + SATURN_EXECUTORS_NODE + "/" + EXECUTORS;
	public static final String SHARDINGNODE_PATH = "/" + SATURN_EXECUTORS_NODE + "/" + SHARDING;
	public static final String LEADER_LATCHNODE_PATH = "/" + SATURN_EXECUTORS_NODE + "/" + LEADER + "/" + LATCH;
	public static final String SHARDING_CONTENTNODE_PATH = "/" + SATURN_EXECUTORS_NODE + "/" + SHARDING + "/" + CONTENT;
	public static final String EXECUTOR_IPNODE_PATH_REGEX =
			"/\\" + SATURN_EXECUTORS_NODE + "/" + EXECUTORS + "/" + "[^/]*"
					+ "/" + IP;
	public static final String EXECUTOR_NO_TRAFFIC_NODE_PATH_REGEX =
			"/\\" + SATURN_EXECUTORS_NODE + "/" + EXECUTORS + "/" + "[^/]*"
					+ "/" + NO_TRAFFIC;
	public static final String CONFIG_VERSION_PATH = "/" + SATURN_EXECUTORS_NODE + "/config/version";

	public static final String JOBSNODE_PATH = "/" + JOBS_NODE;
	public static final String SATURNEXECUTORS_PATH = "/" + SATURN_EXECUTORS_NODE;

	/**
	 * 获取$SaturnExecutors结点完整路径
	 */
	public static String getSaturnExecutorsNodeName() {
		return SATURN_EXECUTORS_NODE;
	}

	/**
	 * 获取$SaturnExecutors/executors结点完整路径
	 */
	public static String getExecutorsNodePath() {
		return "/" + SATURN_EXECUTORS_NODE + "/" + EXECUTORS;
	}

	/**
	 * 获取ip结点名称
	 */
	public static String getIpNodeName() {
		return IP;
	}

	/**
	 * 获取noTraffic结点名称
	 */
	public static String getNoTrafficNodeName() {
		return NO_TRAFFIC;
	}

	/**
	 * 获取$SaturnExecutors/executors/xx结点完整路径
	 */
	public static String getExecutorNodePath(String executor) {
		return "/" + SATURN_EXECUTORS_NODE + "/" + EXECUTORS + "/" + executor;
	}

	/**
	 * 获取$SaturnExecutors/executors/xx/ip结点完整路径
	 */
	public static String getExecutorIpNodePath(String executor) {
		return "/" + SATURN_EXECUTORS_NODE + "/" + EXECUTORS + "/" + executor + "/" + IP;
	}

	/**
	 * 获取$SaturnExecutors/executors/xx/noTraffic结点完整路径
	 *
	 * @return true 已经被摘流量；false，otherwise；
	 */
	public static String getExecutorNoTrafficNodePath(String executor) {
		return "/" + SATURN_EXECUTORS_NODE + "/" + EXECUTORS + "/" + executor + "/" + NO_TRAFFIC;
	}

	/**
	 * 获取$SaturnExecutors/executors/xx/clean结点完整路径
	 */
	public static String getExecutorCleanNodePath(String executor) {
		return "/" + SATURN_EXECUTORS_NODE + "/" + EXECUTORS + "/" + executor + "/" + CLEAN;
	}

	/**
	 * 获取$SaturnExecutors/executors/xx/task结点完整路径
	 */
	public static String getExecutorTaskNodePath(String executor) {
		return "/" + SATURN_EXECUTORS_NODE + "/" + EXECUTORS + "/" + executor + "/" + TASK;
	}

	/**
	 * 从路径中抽取出executorName
	 */
	public static String getExecutorNameByIpPath(String path) {
		return getExecutorNameByPath(path, getIpNodeName());
	}

	/**
	 * 从路径中抽取出executorName
	 */
	public static String getExecutorNameByNoTrafficPath(String path) {
		return getExecutorNameByPath(path, getNoTrafficNodeName());
	}

	private static String getExecutorNameByPath(String path, String nodeName) {
		int lastIndexOf = path.lastIndexOf("/" + nodeName);
		String substring = path.substring(0, lastIndexOf);
		int lastIndexOf2 = substring.lastIndexOf('/');
		return substring.substring(lastIndexOf2 + 1);
	}

	/**
	 * 获取$SaturnExecutors/sharding结点完整路径
	 */
	public static String getExecutorShardingNodePath(String nodeName) {
		return "/" + SATURN_EXECUTORS_NODE + "/" + SHARDING + "/" + nodeName;
	}

	/**
	 * 获取$SaturnExecutors/sharding/content结点完整路径
	 */
	public static String getShardingContentElementNodePath(String element) {
		return "/" + SATURN_EXECUTORS_NODE + "/" + SHARDING + "/" + CONTENT + "/" + element;
	}

	/**
	 * 获取作业结点完整路径
	 */
	public static String getJobNodePath(String jobName) {
		return String.format("/%s/%s", JOBS_NODE, jobName);
	}

	/**
	 * 获取$Jobs/xx/config/shardingTotalCount结点完整路径
	 */
	public static String getJobConfigShardingTotalCountNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", JOBS_NODE, jobName, "config", "shardingTotalCount");
	}

	/**
	 * 获取$Jobs/xx/config/loadLevel结点完整路径
	 */
	public static String getJobConfigLoadLevelNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", JOBS_NODE, jobName, "config", "loadLevel");
	}

	/**
	 * 获取$Jobs/xx/config/preferList结点完整路径
	 */
	public static String getJobConfigPreferListNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", JOBS_NODE, jobName, "config", "preferList");
	}

	/**
	 * 获取$Jobs/xx/config/enabled结点完整路径
	 */
	public static String getJobConfigEnableNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", JOBS_NODE, jobName, "config", "enabled");
	}

	/**
	 * 获取$Jobs/xx/config/localMode结点完整路径
	 */
	public static String getJobConfigLocalModeNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", JOBS_NODE, jobName, "config", "localMode");
	}

	/**
	 * 获取$Jobs/xx/config/useSerial结点完整路径
	 */
	public static String getJobConfigUseSerialNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", JOBS_NODE, jobName, "config", "useSerial");
	}

	/**
	 * 获取$Jobs/xx/config/useDispreferList结点完整路径
	 */
	public static String getJobConfigUseDispreferListNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", JOBS_NODE, jobName, "config", "useDispreferList");
	}

	/**
	 * 获取$Jobs/xx/config/forceShard结点完整路径
	 */
	public static String getJobConfigForceShardNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", JOBS_NODE, jobName, "config", "forceShard");
	}

	/**
	 * 获取$Jobs/xx/leader/sharding/necessary完整路径
	 */
	public static String getJobLeaderShardingNecessaryNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s/%s", JOBS_NODE, jobName, "leader", "sharding", "necessary");
	}

	/**
	 * 获取$Jobs/xx/leader/sharding完整路径
	 */
	public static String getJobLeaderShardingNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", JOBS_NODE, jobName, "leader", "sharding");
	}

	/**
	 * 获取$Jobs/xx/execution完整路径
	 */
	public static String getJobExecutionNodePath(final String jobName) {
		return String.format("/%s/%s/execution", JOBS_NODE, jobName);
	}

	/**
	 * 获取$Jobs/xx/servers完整路径
	 */
	public static String getJobServersNodePath(String jobName) {
		return String.format("/%s/%s/servers", JOBS_NODE, jobName);
	}

	/**
	 * 获取$Jobs/xx/servers/yy完整路径
	 */
	public static String getJobServersExecutorNodePath(String jobName, String executorName) {
		return String.format("/%s/%s/servers/%s", JOBS_NODE, jobName, executorName);
	}

	/**
	 * Get the $/Jobs/jobName/servers/executorName/status node path
	 */
	public static String getJobServersExecutorStatusNodePath(String jobName, String executorName) {
		return String.format("/%s/%s/servers/%s/%s", JOBS_NODE, jobName, executorName, "status");
	}

	public static String getJobServersExecutorStatusNodePathRegex(String jobName) {
		return "/\\" + JOBS_NODE + "/" + jobName + "/" + "servers" + "/" + "[^/]*" + "/" + "status";
	}

	public static String getJobServersExecutorNameByStatusPath(String path) {
		int beginIndexOf = path.lastIndexOf("/servers/") + 9;
		int lastIndexOf = path.lastIndexOf("/status");
		return path.substring(beginIndexOf, lastIndexOf);
	}

	/**
	 * 获取$DCOS/tasks/xxx完整路径
	 */
	public static String getDcosTaskNodePath(String task) {
		return String.format("/%s/%s/%s", DCOS_NODE, TASKS, task);
	}

}
