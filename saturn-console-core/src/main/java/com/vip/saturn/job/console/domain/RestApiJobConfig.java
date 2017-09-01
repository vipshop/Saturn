package com.vip.saturn.job.console.domain;

/**
 * @author xiaopeng.he
 */
public class RestApiJobConfig {

	private String jobClass;
	private String jobType;
	private Integer shardingTotalCount;
	private String shardingItemParameters;
	private String jobParameter;
	private String timeZone;
	private String cron;
	private String pausePeriodDate;
	private String pausePeriodTime;
	private Integer timeout4AlarmSeconds;
	private Integer timeoutSeconds;
	private String channelName;
	private String queueName;
	private Integer loadLevel;
	private Integer jobDegree;
	private Boolean enabledReport;
	private String preferList;
	private Boolean useDispreferList;
	private Boolean localMode;
	private Boolean useSerial;
	private String dependencies;
	private String groups;
	private Boolean showNormalLog;
	private Integer processCountInterValSeconds;

	public String getJobClass() {
		return jobClass;
	}

	public void setJobClass(String jobClass) {
		this.jobClass = jobClass;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public Integer getShardingTotalCount() {
		return shardingTotalCount;
	}

	public void setShardingTotalCount(Integer shardingTotalCount) {
		this.shardingTotalCount = shardingTotalCount;
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

	public Integer getTimeout4AlarmSeconds() {
		return timeout4AlarmSeconds;
	}

	public void setTimeout4AlarmSeconds(Integer timeout4AlarmSeconds) {
		this.timeout4AlarmSeconds = timeout4AlarmSeconds;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public Integer getLoadLevel() {
		return loadLevel;
	}

	public void setLoadLevel(Integer loadLevel) {
		this.loadLevel = loadLevel;
	}

	public Integer getJobDegree() {
		return jobDegree;
	}

	public void setJobDegree(Integer jobDegree) {
		this.jobDegree = jobDegree;
	}

	public String getPreferList() {
		return preferList;
	}

	public void setPreferList(String preferList) {
		this.preferList = preferList;
	}

	public Boolean getUseDispreferList() {
		return useDispreferList;
	}

	public void setUseDispreferList(Boolean useDispreferList) {
		this.useDispreferList = useDispreferList;
	}

	public Boolean getLocalMode() {
		return localMode;
	}

	public void setLocalMode(Boolean localMode) {
		this.localMode = localMode;
	}

	public Boolean getUseSerial() {
		return useSerial;
	}

	public void setUseSerial(Boolean useSerial) {
		this.useSerial = useSerial;
	}

	public Boolean getEnabledReport() {
		return enabledReport;
	}

	public void setEnabledReport(Boolean enabledReport) {
		this.enabledReport = enabledReport;
	}

	public String getDependencies() {
		return dependencies;
	}

	public void setDependencies(String dependencies) {
		this.dependencies = dependencies;
	}

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}

	public Boolean getShowNormalLog() {
		return showNormalLog;
	}

	public void setShowNormalLog(Boolean showNormalLog) {
		this.showNormalLog = showNormalLog;
	}

	public Integer getProcessCountInterValSeconds() {
		return processCountInterValSeconds;
	}

	public void setProcessCountInterValSeconds(Integer processCountInterValSeconds) {
		this.processCountInterValSeconds = processCountInterValSeconds;
	}
}
