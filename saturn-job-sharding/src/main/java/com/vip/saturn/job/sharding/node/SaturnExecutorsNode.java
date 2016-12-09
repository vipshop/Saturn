package com.vip.saturn.job.sharding.node;

/**
 * @author xiaopeng.he
 */
public class SaturnExecutorsNode {

	private static final String $SATURNEXECUTORS = "$SaturnExecutors";
	private static final String EXECUTORS = "executors";
	private static final String HOST = "host";
	private static final String LEADER = "leader";
	private static final String LATCH = "latch";
	private static final String SHARDING = "sharding";
	private static final String CONTENT = "content";
	public static final String $JOBS = "$Jobs";
	private static final String IP = "ip";
	private static final String CLEAN = "clean";
	private static final String TASK = "task";
	private static final String $DCOS = "$DCOS";
	private static final String TASKS = "tasks";
	public static final String SHARDING_COUNT_PATH = "/" + $SATURNEXECUTORS + "/" + SHARDING + "/" + "count";
	public final static String LEADER_HOSTNODE_PATH = "/" + $SATURNEXECUTORS + "/" + LEADER + "/" + HOST;
	public final static String LEADERNODE_PATH = "/" + $SATURNEXECUTORS + "/" + LEADER;
	public final static String EXECUTORSNODE_PATH = "/" + $SATURNEXECUTORS + "/" + EXECUTORS;
    public static String LEADER_LATCHNODE_PATH = "/" + $SATURNEXECUTORS + "/" + LEADER + "/" + LATCH;
    public static String SHARDING_CONTENTNODE_PATH = "/" + $SATURNEXECUTORS + "/" + SHARDING + "/" + CONTENT;
    public final static String JOBCONFIG_ENABLE_NODE_PATH_REGEX = "/\\" + $JOBS + "/" + "[^/]*" + "/" + "config" + "/" + "enabled";
	public final static String JOBCONFIG_FORCESHARD_NODE_PATH_REGEX = "/\\" + $JOBS + "/" + "[^/]*" + "/" + "config" + "/" + "forceShard";
    public static final String EXECUTOR_IPNODE_PATH_REGEX = "/\\" + $SATURNEXECUTORS + "/" + EXECUTORS + "/" + "[^/]*" + "/" + IP;
    public static final String CONFIG_VERSION_PATH = "/" + $SATURNEXECUTORS + "/config/version";
    
	public static final String $JOBSNODE_PATH = "/" + $JOBS;   
	public static final String $SATURNEXECUTORS_PATH = "/" + $SATURNEXECUTORS;
	
	/**
	 * 获取$SaturnExecutors结点完整路径
	 * @return
	 */
	public static String get$SaturnExecutorsNodeName() {
		return $SATURNEXECUTORS;
	}
	
	/**
	 *  获取$SaturnExecutors/executors结点完整路径
	 * @return
	 */
	public static String getExecutorsNodePath() {
		return "/" + $SATURNEXECUTORS  + "/" + EXECUTORS;
	}
	
	/**
	 * 获取ip结点名称
	 * @return
	 */
	public static String getIpNodeName() {
		return IP;
	}

	/**
	 * 获取$SaturnExecutors/executors/xx结点完整路径
	 * @param executor
	 * @return
	 */
	public static String getExecutorNodePath(String executor) {
		return "/" + $SATURNEXECUTORS + "/" + EXECUTORS + "/" + executor;
	}

	/**
	 * 获取$SaturnExecutors/executors/xx/ip结点完整路径
	 * @param executor
	 * @return
	 */
	public static String getExecutorIpNodePath(String executor) {
		return "/" + $SATURNEXECUTORS + "/" + EXECUTORS + "/" + executor + "/" + IP;
	}
	
	/**
	 * 获取$SaturnExecutors/executors/xx/clean结点完整路径
	 * @param executor
	 * @return
	 */
	public static String getExecutorCleanNodePath(String executor) {
		return "/" + $SATURNEXECUTORS + "/" + EXECUTORS + "/" + executor + "/" + CLEAN;
	}

	/**
	 * 获取$SaturnExecutors/executors/xx/task结点完整路径
	 * @param executor
	 * @return
	 */
	public static String getExecutorTaskNodePath(String executor) {
		return "/" + $SATURNEXECUTORS + "/" + EXECUTORS + "/" + executor + "/" + TASK;
	}
	
	/**
	 * 从路径中抽取出executorName
	 * @param path
	 * @return
	 */
	public static String getExecutorNameByIpPath(String path) {
		int lastIndexOf = path.lastIndexOf("/" + SaturnExecutorsNode.getIpNodeName());
		String substring = path.substring(0, lastIndexOf);
		int lastIndexOf2 = substring.lastIndexOf('/');
		return substring.substring(lastIndexOf2 + 1);
	}

	/**
	 * 获取$SaturnExecutors/sharding/content结点完整路径
	 * @param element
	 * @return
	 */
    public static String getShardingContentElementNodePath(String element) {
    	return "/" + $SATURNEXECUTORS + "/" + SHARDING + "/" + CONTENT + "/" + element;
    }

	/**
	 * 获取作业结点完整路径
	 */
	public static String getJobNodePath(String jobName) {
		return String.format("/%s/%s", $JOBS, jobName);
	}

    /**
     * 获取$Jobs/xx/config/shardingTotalCount结点完整路径
     * @param jobName
     * @return
     */
    public static String getJobConfigShardingTotalCountNodePath(String jobName) {
    	return String.format("/%s/%s/%s/%s", $JOBS, jobName, "config", "shardingTotalCount");
    }
    
    /**
     * 获取$Jobs/xx/config/loadLevel结点完整路径
     * @param jobName
     * @return
     */
    public static String getJobConfigLoadLevelNodePath(String jobName) {
    	return String.format("/%s/%s/%s/%s", $JOBS, jobName, "config", "loadLevel");
    }
    
    /**
     * 获取$Jobs/xx/config/preferList结点完整路径
     * @param jobName
     * @return
     */
    public static String getJobConfigPreferListNodePath(String jobName) {
    	return String.format("/%s/%s/%s/%s", $JOBS, jobName, "config", "preferList");
    }
  
    /**
     * 获取$Jobs/xx/config/enabled结点完整路径
     * @param jobName
     * @return
     */
    public static String getJobConfigEnableNodePath(String jobName) {
    	return String.format("/%s/%s/%s/%s", $JOBS, jobName, "config", "enabled");
    }

    /**
     * 获取$Jobs/xx/config/localMode结点完整路径
     * @param jobName
     * @return
     */
	public static String getJobConfigLocalModeNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", $JOBS, jobName, "config", "localMode");
	}
	
    /**
     * 获取$Jobs/xx/config/useSerial结点完整路径
     * @param jobName
     * @return
     */
	public static String getJobConfigUseSerialNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", $JOBS, jobName, "config", "useSerial");
	}
	
    /**
     * 获取$Jobs/xx/config/useDispreferList结点完整路径
     * @param jobName
     * @return
     */
	public static String getJobConfigUseDispreferListNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", $JOBS, jobName, "config", "useDispreferList");
	}

	/**
	 * 获取$Jobs/xx/config/forceShard结点完整路径
	 * @param jobName
	 * @return
	 */
	public static String getJobConfigForceShardNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", $JOBS, jobName, "config", "forceShard");
	}
    
	/**
	 * 从$Jobs/xx/config/enabled中抽取出jobName
	 * @param path
	 * @return
	 */
    public static String getJobNameByConfigEnabledPath(String path) {
		int lastIndexOf = path.lastIndexOf("/config/enabled");
		String substring = path.substring(0, lastIndexOf);
		int lastIndexOf2 = substring.lastIndexOf('/');
		return substring.substring(lastIndexOf2 + 1);
	}

	/**
	 * 从$Jobs/xx/config/forceShard中抽取出jobName
	 * @param path
	 * @return
	 */
	public static String getJobNameByConfigForceShardPath(String path) {
		int lastIndexOf = path.lastIndexOf("/config/forceShard");
		String substring = path.substring(0, lastIndexOf);
		int lastIndexOf2 = substring.lastIndexOf('/');
		return substring.substring(lastIndexOf2 + 1);
	}
    
    /**
     * 获取$Jobs/xx/leader/sharding/necessary完整路径
     * @param jobName
     * @return
     */
    public static String getJobLeaderShardingNecessaryNodePath(String jobName) {
    	return String.format("/%s/%s/%s/%s/%s", $JOBS, jobName, "leader", "sharding", "necessary");
    }
    
    /**
     * 获取$Jobs/xx/leader/sharding完整路径
     * @param jobName
     * @return
     */
    public static String getJobLeaderShardingNodePath(String jobName) {
		return String.format("/%s/%s/%s/%s", $JOBS, jobName, "leader", "sharding");
    }
    
    /**
     * 获取$Jobs/xx/execution完整路径
     * @param jobName
     * @return
     */
    public static String getJobExecutionNodePath(final String jobName) {
        return String.format("/%s/%s/execution", $JOBS, jobName);
    }

    /**
     * 获取$Jobs/xx/servers/yy完整路径
     * @param jobName
     * @param executorName
     * @return
     */
	public static String getJobServersExecutorNodePath(String jobName, String executorName) {
		return String.format("/%s/%s/servers/%s", $JOBS, jobName, executorName);
	}

	/**
	 * Get the $/Jobs/jobName/servers/executorName/status node path
	 */
	public static String getJobServersExecutorStatusNodePath(String jobName, String executorName) {
		return String.format("/%s/%s/servers/%s/%s", $JOBS, jobName, executorName, "status");
	}

	public static String getJobServersExecutorStatusNodePathRegex(String jobName) {
		return "/\\" + $JOBS + "/"+ jobName + "/"  + "servers" + "/" + "[^/]*" + "/" + "status";
	}

	public static String getJobServersExecutorNameByStatusPath(String path) {
		int beginIndexOf = path.lastIndexOf("/servers/") + 9;
		int lastIndexOf = path.lastIndexOf("/status");
		return path.substring(beginIndexOf, lastIndexOf);
	}

	/**
	 * 获取$DCOS/tasks/xxx完整路径
	 * @param task
	 * @return
	 */
	public static String getDcosTaskNodePath(String task) {
		return String.format("/%s/%s/%s", $DCOS, TASKS, task);
	}

}
