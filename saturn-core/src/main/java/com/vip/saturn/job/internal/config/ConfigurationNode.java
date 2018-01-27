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

package com.vip.saturn.job.internal.config;

import com.vip.saturn.job.internal.storage.JobNodePath;

/**
 * Saturn配置根节点名称的常量类.
 * 
 * 
 */
public final class ConfigurationNode {

	public static final String ROOT = "config";

	public static final String JOB_CLASS = ROOT + "/jobClass";

	public static final String SHARDING_TOTAL_COUNT = ROOT + "/shardingTotalCount";

	public static final String TIMEZONE = ROOT + "/timeZone";

	public static final String CRON = ROOT + "/cron";

	public static final String PREFER_LIST = ROOT + "/preferList";

	public static final String PAUSE_PERIOD_DATE = ROOT + "/pausePeriodDate";

	public static final String PAUSE_PERIOD_TIME = ROOT + "/pausePeriodTime";

	public static final String SHARDING_ITEM_PARAMETERS = ROOT + "/shardingItemParameters";

	public static final String JOB_PARAMETER = ROOT + "/jobParameter";

	public static final String PROCESS_COUNT_INTERVAL_SECONDS = ROOT + "/processCountIntervalSeconds";

	public static final String FAILOVER = ROOT + "/failover";

	public static final String SHOW_NORMAL_LOG = ROOT + "/showNormalLog";

	public static final String DESCRIPTION = ROOT + "/description";

	public static final String TIMEOUTSECONDS = ROOT + "/timeoutSeconds";

	public static final String LOAD_LEVEL = ROOT + "/loadLevel";

	public static final String ENABLED = ROOT + "/enabled";

	public static final String ENABLED_REPORT = ROOT + "/enabledReport";

	public static final String TO_DELETE = ROOT + "/toDelete";

	public static final String LOCAL_MODE = ROOT + "/localMode";

	public static final String USE_SERIAL = ROOT + "/useSerial";

	public static final String USE_DISPREFER_LIST = ROOT + "/useDispreferList";

	/** 作业类型: 动态写入 */
	public static final String JOB_TYPE = ROOT + "/jobType";

	/**
	 * 自定义上下文
	 */
	public static final String CUSTOM_CONTEXT = ROOT + "/customContext";

	/**
	 * 作业接收的queue名字
	 */
	public static final String QUEUE_NAME = ROOT + "/queueName";

	/**
	 * 执行作业发送的channel名字
	 */
	public static final String CHANNEL_NAME = ROOT + "/channelName";

	/**
	 * 判断是否为作业配置路径.
	 * 
	 * @param path 节点路径
	 * @return 是否为作业配置路径
	 */
	public static boolean isIncludeJobConfigPath(final String jobName, final String path) {
		return path.indexOf(JobNodePath.getNodeFullPath(jobName, ROOT)) != -1;
	}

	/**
	 * 判断是否为作业分片总数路径.
	 * 
	 * @param path 节点路径
	 * @return 是否为作业分片总数路径
	 */
	public static boolean isShardingTotalCountPath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, SHARDING_TOTAL_COUNT).equals(path);
	}

	/**
	 * 判断是否为统计处理数据量的间隔秒数路径。
	 * 
	 * @param path 节点路径
	 * @return 是否为统计处理数据量的间隔秒数路径。
	 */
	public static boolean isProcessCountIntervalSecondsPath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, PROCESS_COUNT_INTERVAL_SECONDS).equals(path);
	}

	/**
	 * 判断是否为失效转移设置路径.
	 * 
	 * @param jobName 作业名称
	 * @param path 节点路径
	 * @return 是否为失效转移设置路径
	 */
	public static boolean isFailoverPath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, FAILOVER).equals(path);
	}

	/**
	 * 判断是否为作业调度配置路径.
	 * 
	 * @param path 节点路径
	 * @return 是否为作业调度配置路径
	 */
	public static boolean isCronPath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, CRON).equals(path);
	}

	public static boolean isEnabledPath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, ENABLED).equals(path);
	}

	public static boolean isToDeletePath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, TO_DELETE).equals(path);
	}

	/**
	 * 判断是否为作业暂停日期时间段配置路径
	 * @param path 节点路径
	 * @return 是否为作业暂停日期时间段配置路径
	 */
	public static boolean isPausePeriodDatePath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, PAUSE_PERIOD_DATE).equals(path);
	}

	/**
	 * 判断是否为作业暂停时分时间段配置路径
	 * @param path 节点路径
	 * @return 是否为作业暂停时分时间段配置路径
	 */
	public static boolean isPausePeriodTimePath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, PAUSE_PERIOD_TIME).equals(path);
	}

	/**
	 * @param path 指定路径
	 * @return 是否显示正常执行日志
	 */
	public static boolean isShowNormaLogPath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, SHOW_NORMAL_LOG).equals(path);
	}

	/**
	 * @param path 指定路径
	 * @return 是否为QueueName路径
	 */
	public static boolean isQueueNamePath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, QUEUE_NAME).equals(path);
	}

	/**
	 * @param path 指定路径
	 * @return 是否为ChannelName路径
	 */
	public static boolean isChannelNamePath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, CHANNEL_NAME).equals(path);
	}

	/**
	 * @param path 指定路径
	 * @return 是否为ShardingItemParameters路径
	 */
	public static boolean isShardingItemParametersPath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, SHARDING_ITEM_PARAMETERS).equals(path);
	}

	/**
	 * 
	 * @param path 指定路径
	 * @return 是否为jobParameter路径
	 */
	public static boolean isJobParameterPath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, JOB_PARAMETER).equals(path);
	}

	public static boolean isTimeoutSecondsPath(final String jobName, final String path) {
		return JobNodePath.getNodeFullPath(jobName, TIMEOUTSECONDS).equals(path);
	}

	/**
	 * @return 获取超时时间
	 */
	public static int getTimeoutSeconds(final String jobName) {
		return Integer.parseInt(JobNodePath.getNodeFullPath(jobName, TIMEOUTSECONDS));
	}
}
