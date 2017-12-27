package com.vip.saturn.job.console.vo;

/**
 * @author hebelala
 * @author chembo.huang
 */
public class JobInfo {

	private String jobName;

	private JobStatus status;

	private String description;

	private String timeZone;

	private String cron;

	private String jobClass;

	private String nextFireTime;// 下次开始时间：所有分片中下次最早的开始时间

	private String lastBeginTime;// 最近开始时间：所有分片中最近最早的开始时间

	private String lastCompleteTime;// 最近完成时间：所有分片中最近最晚的完成时间

	private JobType jobType;

	private String loadLevel;

	private String jobDegree;

	private Boolean enabledReport;// 点分片页签时，控制信息是否上报

	private String shardingTotalCount;

	private String preferList;

	private String shardingList;

	private Boolean isJobEnabled;

	private Boolean useDispreferList;

	private Boolean localMode;

	private String jobParameter;

	private String shardingItemParameters;

	private String queueName;

	private String channelName;

	private Integer processCountIntervalSeconds;

	private Integer timeout4AlarmSeconds;

	private Integer timeoutSeconds;

	private String pausePeriodDate;

	private String pausePeriodTime;

	private Boolean showNormalLog;

	private Boolean useSerial;

	private String jobRate;

	private Boolean migrateEnabled;

	private String groups;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public String getJobClass() {
		return jobClass;
	}

	public void setJobClass(String jobClass) {
		this.jobClass = jobClass;
	}

	public String getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(String nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	public String getLastBeginTime() {
		return lastBeginTime;
	}

	public void setLastBeginTime(String lastBeginTime) {
		this.lastBeginTime = lastBeginTime;
	}

	public String getLastCompleteTime() {
		return lastCompleteTime;
	}

	public void setLastCompleteTime(String lastCompleteTime) {
		this.lastCompleteTime = lastCompleteTime;
	}

	public JobType getJobType() {
		return jobType;
	}

	public void setJobType(JobType jobType) {
		this.jobType = jobType;
	}

	public String getLoadLevel() {
		return loadLevel;
	}

	public void setLoadLevel(String loadLevel) {
		this.loadLevel = loadLevel;
	}

	public String getJobDegree() {
		return jobDegree;
	}

	public void setJobDegree(String jobDegree) {
		this.jobDegree = jobDegree;
	}

	public Boolean getEnabledReport() {
		return enabledReport;
	}

	public void setEnabledReport(Boolean enabledReport) {
		this.enabledReport = enabledReport;
	}

	public String getShardingTotalCount() {
		return shardingTotalCount;
	}

	public void setShardingTotalCount(String shardingTotalCount) {
		this.shardingTotalCount = shardingTotalCount;
	}

	public String getPreferList() {
		return preferList;
	}

	public void setPreferList(String preferList) {
		this.preferList = preferList;
	}

	public String getShardingList() {
		return shardingList;
	}

	public void setShardingList(String shardingList) {
		this.shardingList = shardingList;
	}

	public Boolean getJobEnabled() {
		return isJobEnabled;
	}

	public void setJobEnabled(Boolean jobEnabled) {
		isJobEnabled = jobEnabled;
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

	public String getJobParameter() {
		return jobParameter;
	}

	public void setJobParameter(String jobParameter) {
		this.jobParameter = jobParameter;
	}

	public String getShardingItemParameters() {
		return shardingItemParameters;
	}

	public void setShardingItemParameters(String shardingItemParameters) {
		this.shardingItemParameters = shardingItemParameters;
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

	public Integer getProcessCountIntervalSeconds() {
		return processCountIntervalSeconds;
	}

	public void setProcessCountIntervalSeconds(Integer processCountIntervalSeconds) {
		this.processCountIntervalSeconds = processCountIntervalSeconds;
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

	public Boolean getShowNormalLog() {
		return showNormalLog;
	}

	public void setShowNormalLog(Boolean showNormalLog) {
		this.showNormalLog = showNormalLog;
	}

	public Boolean getUseSerial() {
		return useSerial;
	}

	public void setUseSerial(Boolean useSerial) {
		this.useSerial = useSerial;
	}

	public String getJobRate() {
		return jobRate;
	}

	public void setJobRate(String jobRate) {
		this.jobRate = jobRate;
	}

	public Boolean getMigrateEnabled() {
		return migrateEnabled;
	}

	public void setMigrateEnabled(Boolean migrateEnabled) {
		this.migrateEnabled = migrateEnabled;
	}

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}
}
