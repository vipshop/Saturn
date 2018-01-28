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

package com.vip.saturn.job.internal.config;

import com.google.common.base.Strings;
import com.vip.saturn.job.basic.AbstractElasticJob;
import com.vip.saturn.job.basic.JobTypeManager;
import com.vip.saturn.job.basic.SaturnConstant;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;

/**
 * 作业配置信息.
 *
 */

public class JobConfiguration {

	/**
	 * 作业名称.
	 */
	private final String jobName;

	private Class<? extends AbstractElasticJob> saturnJobClass;
	/**
	 * 如果regCenter!=null, 取值从zk取
	 */
	private CoordinatorRegistryCenter regCenter = null;
	
	private String jobClass = "";
	/**
	 * 作业分片总数.
	 */
	private int shardingTotalCount;
	/**
	 * 时区
	 */
	private String timeZone;
	/**
	 * 作业启动时间的cron表达式.
	 */
	private String cron;
	/**
	 * 作业暂停时间段，日期段。
	 */
	private String pausePeriodDate = "";
	/**
	 * 作业暂停时间段，小时分钟段。
	 */
	private String pausePeriodTime = "";
	/**
	 * 分片序列号和个性化参数对照表.
	 *
	 * <p>
	 * 分片序列号和参数用等号分隔, 多个键值对用逗号分隔. 类似map. 分片序列号从0开始, 不可大于或等于作业分片总数. 如: 0=a,1=b,2=c
	 * </p>
	 */
	private String shardingItemParameters = "";
	/**
	 * 作业自定义参数.
	 *
	 * <p>
	 * 可以配置多个相同的作业, 但是用不同的参数作为不同的调度实例.
	 * </p>
	 */
	private String jobParameter = "";
	/**
	 * 统计作业处理数据数量的间隔时间.
	 *
	 * <p>
	 * 单位: 秒. 只对处理数据流类型作业起作用.
	 * </p>
	 */
	private int processCountIntervalSeconds = 300;
	/**
	 * 是否开启失效转移.
	 */
	private boolean failover = true;
	/**
	 * 作业是否启用
	 */
	private boolean enabled = false;
	/**
	 * 作业描述信息.
	 */
	private String description = "";
	/**
	 * 本地配置是否可覆盖注册中心配置. 如果可覆盖, 每次启动作业都以本地配置为准.
	 */
	private boolean overwrite;
	/**
	 * Job超时时间
	 */
	private int timeoutSeconds;
	/**
	 * 默认不开启，只显示异常情况下的日志；异常情况的日志无论开闭都会显示；日志保存在zk，只保存最新100行；
	 */
	private boolean showNormalLog = false;
	/**
	 * 作业类型, 动态判断(非配置)
	 */
	private String jobType;
	/**
	 * 作业接收的queue名字
	 */
	private String queueName = "";
	/**
	 * 执行作业发送的channel名字
	 */
	private String channelName = "";
	/**
	 * 每个分片的权重
	 */
	private Integer loadLevel = 1;
	/**
	 * 每个作业的预分配列表
	 */
	private String preferList = "";
	/**
	 * 是否上报执行信息（上报状态信息如completed，running，timeout）
	 */
	private Boolean enabledReport = null;
	/**
	 * 是否启用本地模式
	 */
	private boolean localMode = false;
	/**
	 * 是否启用串行消费（给消息作业使用，默认是并行的，并行的fetchSize默认为64）
	 */
	private boolean useSerial = false;
	/**
	 * 是否使用非preferList
	 */
	private boolean useDispreferList = true;

	// Test Use Only!
	public JobConfiguration(String jobName, Class<? extends AbstractElasticJob> jobClass, int shardingTotalCount,
			String cron) {
		this.jobName = jobName;
		this.saturnJobClass = jobClass;
		this.shardingTotalCount = shardingTotalCount;
		this.cron = cron;
	}

	public JobConfiguration(CoordinatorRegistryCenter regCenter, String jobName) {
		this.jobName = jobName;
		this.regCenter = regCenter;
		reloadConfig();// NOSONAR
	}

	public JobConfiguration(String jobName) {
		this.jobName = jobName;
	}

	public Class<? extends AbstractElasticJob> getSaturnJobClass() {
		// 测试时可以直接设置实现类
		if (saturnJobClass != null) {
			return saturnJobClass;
		}

		return JobTypeManager.getInstance().getHandler(getJobType());
	}

	public void setSaturnJobClass(Class<? extends AbstractElasticJob> saturnJobClass) {
		this.saturnJobClass = saturnJobClass;
	}

	public void reloadConfig() {
		if (regCenter == null) {
			return;
		}

		String valStr = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.TIMEOUTSECONDS));
		if (!Strings.isNullOrEmpty(valStr)) {
			timeoutSeconds = Integer.parseInt(valStr);
		}

		jobClass = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.JOB_CLASS));
		if (jobClass != null) {
			jobClass = jobClass.trim();
		}
		jobType = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.JOB_TYPE));

		valStr = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.SHARDING_TOTAL_COUNT));
		if (!Strings.isNullOrEmpty(valStr)) {
			shardingTotalCount = Integer.parseInt(valStr);
		}

		timeZone = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.TIMEZONE));
		if (Strings.isNullOrEmpty(timeZone)) {
			timeZone = SaturnConstant.TIME_ZONE_ID_DEFAULT;
		}

		cron = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.CRON));
		pausePeriodDate = regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.PAUSE_PERIOD_DATE));
		pausePeriodTime = regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.PAUSE_PERIOD_TIME));
		shardingItemParameters = regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.SHARDING_ITEM_PARAMETERS));
		jobParameter = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.JOB_PARAMETER));

		valStr = regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.PROCESS_COUNT_INTERVAL_SECONDS));
		if (!Strings.isNullOrEmpty(valStr)) {
			processCountIntervalSeconds = Integer.parseInt(valStr);
		}

		failover = Boolean
				.parseBoolean(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.FAILOVER)));
		enabled = Boolean
				.parseBoolean(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.ENABLED)));
		description = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.DESCRIPTION));
		showNormalLog = Boolean.parseBoolean(
				regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.SHOW_NORMAL_LOG)));
		queueName = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.QUEUE_NAME));
		channelName = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.CHANNEL_NAME));

		valStr = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.LOAD_LEVEL));
		if (!Strings.isNullOrEmpty(valStr)) {
			loadLevel = Integer.valueOf(valStr);
		}

		preferList = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.PREFER_LIST));

		String enabledReportStr = regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.ENABLED_REPORT));
		if (!Strings.isNullOrEmpty(enabledReportStr)) {
			enabledReport = Boolean.valueOf(enabledReportStr);
		}

		localMode = Boolean
				.parseBoolean(
						regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.LOCAL_MODE)));
		useSerial = Boolean
				.parseBoolean(
						regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.USE_SERIAL)));
		useDispreferList = Boolean.parseBoolean(
				regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.USE_DISPREFER_LIST)));
	}

	public boolean isDeleting() {
		return regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.TO_DELETE));
	}

	public String getCronFromZk() {
		cron = regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.CRON));
		return cron;
	}

	public CoordinatorRegistryCenter getRegCenter() {
		return regCenter;
	}

	public void setRegCenter(CoordinatorRegistryCenter regCenter) {
		this.regCenter = regCenter;
	}

	public String getJobName() {
		return jobName;
	}

	public String getJobClass() {
		return jobClass;
	}

	public void setJobClass(String jobClass) {
		this.jobClass = jobClass;
	}

	public int getShardingTotalCount() {
		return shardingTotalCount;
	}

	public void setShardingTotalCount(int shardingTotalCount) {
		this.shardingTotalCount = shardingTotalCount;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public String getCron() {
		return cron;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}

	public String getPausePeriodDate() {
		return pausePeriodDate;
	}

	public void setPausePeriodDate(String pausePeriodDate) {
		this.pausePeriodDate = pausePeriodDate;
	}

	public String getPausePeriodTime() {
		return pausePeriodTime;
	}

	public void setPausePeriodTime(String pausePeriodTime) {
		this.pausePeriodTime = pausePeriodTime;
	}

	public String getShardingItemParameters() {
		return shardingItemParameters;
	}

	public void setShardingItemParameters(String shardingItemParameters) {
		this.shardingItemParameters = shardingItemParameters;
	}

	public String getJobParameter() {
		return jobParameter;
	}

	public void setJobParameter(String jobParameter) {
		this.jobParameter = jobParameter;
	}

	public int getProcessCountIntervalSeconds() {
		return processCountIntervalSeconds;
	}

	public void setProcessCountIntervalSeconds(int processCountIntervalSeconds) {
		this.processCountIntervalSeconds = processCountIntervalSeconds;
	}

	public boolean isFailover() {
		return failover;
	}

	public void setFailover(boolean failover) {
		this.failover = failover;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public boolean isShowNormalLog() {
		return showNormalLog;
	}

	public void setShowNormalLog(boolean showNormalLog) {
		this.showNormalLog = showNormalLog;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public Integer getLoadLevel() {
		return loadLevel;
	}

	public void setLoadLevel(Integer loadLevel) {
		this.loadLevel = loadLevel;
	}

	public String getPreferList() {
		return preferList;
	}

	public void setPreferList(String preferList) {
		this.preferList = preferList;
	}

	public boolean isLocalMode() {
		return localMode;
	}

	public void setLocalMode(boolean localMode) {
		this.localMode = localMode;
	}

	public boolean isUseSerial() {
		return useSerial;
	}

	public void setUseSerial(boolean useSerial) {
		this.useSerial = useSerial;
	}

	public boolean isUseDispreferList() {
		return useDispreferList;
	}

	public void setUseDispreferList(boolean useDispreferList) {
		this.useDispreferList = useDispreferList;
	}

	public void setEnabledReport(Boolean enabledReport) {
		this.enabledReport = enabledReport;
	}

	public Boolean isEnabledReport() {
		return enabledReport;
	}
}