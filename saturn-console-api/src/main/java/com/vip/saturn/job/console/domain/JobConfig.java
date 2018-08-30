package com.vip.saturn.job.console.domain;

import com.vip.saturn.job.console.utils.SaturnConstants;

import java.io.Serializable;

/**
 * @author chembo.huang
 */
public class JobConfig implements Serializable {

	private static final long serialVersionUID = 7366583369937964951L;

	private String jobName;
	private String jobClass;
	private Integer shardingTotalCount;
	private String timeZone;
	private String cron;
	private String pausePeriodDate;
	private String pausePeriodTime;
	private String shardingItemParameters;
	private String jobParameter;
	private Integer processCountIntervalSeconds;
	private String description;
	private Integer timeout4AlarmSeconds;
	private Integer timeoutSeconds;
	private Boolean showNormalLog;
	private String channelName;
	private String jobType;
	private String queueName;
	private Integer loadLevel;
	private Integer jobDegree;
	private Boolean enabledReport;
	private Boolean enabled;
	private String preferList;
	private Boolean useDispreferList;
	private Boolean localMode;
	private Boolean useSerial;
	private Boolean failover;
	private String jobMode; // 系统作业等
	private String customContext;
	private String dependencies;
	private String groups;

	private Boolean isCopyJob = Boolean.FALSE;

	private Boolean rerun;

	private <T> T getDefaultIfNull(T val, T def) {
		return val == null ? def : val;
	}

	public void setDefaultValues() {
		jobClass = getDefaultIfNull(jobClass, "");
		shardingTotalCount = getDefaultIfNull(shardingTotalCount, 1);
		timeZone = getDefaultIfNull(timeZone, SaturnConstants.TIME_ZONE_ID_DEFAULT);
		cron = getDefaultIfNull(cron, "");
		pausePeriodDate = getDefaultIfNull(pausePeriodDate, "");
		pausePeriodTime = getDefaultIfNull(pausePeriodTime, "");
		jobParameter = getDefaultIfNull(jobParameter, "");
		processCountIntervalSeconds = getDefaultIfNull(processCountIntervalSeconds, 300);
		description = getDefaultIfNull(description, "");
		timeout4AlarmSeconds = timeout4AlarmSeconds == null || timeout4AlarmSeconds < 0 ? 0 : timeout4AlarmSeconds;
		timeoutSeconds = timeoutSeconds == null || timeoutSeconds < 0 ? 0 : timeoutSeconds;
		showNormalLog = getDefaultIfNull(showNormalLog, Boolean.FALSE);
		channelName = getDefaultIfNull(channelName, "");
		queueName = getDefaultIfNull(queueName, "");
		loadLevel = getDefaultIfNull(loadLevel, 1);
		jobDegree = getDefaultIfNull(jobDegree, 0);
		if (enabledReport == null) {
			if (JobType.JAVA_JOB.name().equals(jobType) || JobType.SHELL_JOB.name().equals(jobType)) {
				enabledReport = Boolean.TRUE;
			} else {
				enabledReport = Boolean.FALSE;
			}
		}
		enabled = getDefaultIfNull(enabled, Boolean.FALSE);
		preferList = getDefaultIfNull(preferList, "");
		useDispreferList = getDefaultIfNull(useDispreferList, Boolean.TRUE);
		localMode = getDefaultIfNull(localMode, Boolean.FALSE);
		useSerial = getDefaultIfNull(useSerial, Boolean.FALSE);
		failover = getDefaultIfNull(failover, !localMode); // 已经设置localMode
		jobMode = getDefaultIfNull(jobMode, "");
		dependencies = getDefaultIfNull(dependencies, "");
		groups = getDefaultIfNull(groups, "");
		rerun = getDefaultIfNull(rerun, Boolean.FALSE);
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getJobClass() {
		return jobClass;
	}

	public void setJobClass(String jobClass) {
		this.jobClass = jobClass;
	}

	public Integer getShardingTotalCount() {
		return shardingTotalCount;
	}

	public void setShardingTotalCount(Integer shardingTotalCount) {
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

	public Integer getProcessCountIntervalSeconds() {
		return processCountIntervalSeconds;
	}

	public void setProcessCountIntervalSeconds(Integer processCountIntervalSeconds) {
		this.processCountIntervalSeconds = processCountIntervalSeconds;
	}

	public Boolean getFailover() {
		return failover;
	}

	public void setFailover(Boolean failover) {
		this.failover = failover;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public Boolean getShowNormalLog() {
		return showNormalLog;
	}

	public void setShowNormalLog(Boolean showNormalLog) {
		this.showNormalLog = showNormalLog;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
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

	public Boolean getEnabledReport() {
		return enabledReport;
	}

	public void setEnabledReport(Boolean enabledReport) {
		this.enabledReport = enabledReport;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
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

	public Boolean getCopyJob() {
		return isCopyJob;
	}

	public void setCopyJob(Boolean copyJob) {
		isCopyJob = copyJob;
	}

	public String getJobMode() {
		return jobMode;
	}

	public void setJobMode(String jobMode) {
		this.jobMode = jobMode;
	}

	public String getCustomContext() {
		return customContext;
	}

	public void setCustomContext(String customContext) {
		this.customContext = customContext;
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

	public Boolean getRerun() {
		return rerun;
	}

	public void setRerun(Boolean rerun) {
		this.rerun = rerun;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		JobConfig jobConfig = (JobConfig) o;

		if (jobName != null ? !jobName.equals(jobConfig.jobName) : jobConfig.jobName != null)
			return false;
		if (jobClass != null ? !jobClass.equals(jobConfig.jobClass) : jobConfig.jobClass != null)
			return false;
		if (shardingTotalCount != null ?
				!shardingTotalCount.equals(jobConfig.shardingTotalCount) :
				jobConfig.shardingTotalCount != null)
			return false;
		if (timeZone != null ? !timeZone.equals(jobConfig.timeZone) : jobConfig.timeZone != null)
			return false;
		if (cron != null ? !cron.equals(jobConfig.cron) : jobConfig.cron != null)
			return false;
		if (pausePeriodDate != null ?
				!pausePeriodDate.equals(jobConfig.pausePeriodDate) :
				jobConfig.pausePeriodDate != null)
			return false;
		if (pausePeriodTime != null ?
				!pausePeriodTime.equals(jobConfig.pausePeriodTime) :
				jobConfig.pausePeriodTime != null)
			return false;
		if (shardingItemParameters != null ?
				!shardingItemParameters.equals(jobConfig.shardingItemParameters) :
				jobConfig.shardingItemParameters != null)
			return false;
		if (jobParameter != null ? !jobParameter.equals(jobConfig.jobParameter) : jobConfig.jobParameter != null)
			return false;
		if (processCountIntervalSeconds != null ?
				!processCountIntervalSeconds.equals(jobConfig.processCountIntervalSeconds) :
				jobConfig.processCountIntervalSeconds != null)
			return false;
		if (failover != null ? !failover.equals(jobConfig.failover) : jobConfig.failover != null)
			return false;
		if (description != null ? !description.equals(jobConfig.description) : jobConfig.description != null)
			return false;
		if (timeout4AlarmSeconds != null ?
				!timeout4AlarmSeconds.equals(jobConfig.timeout4AlarmSeconds) :
				jobConfig.timeout4AlarmSeconds != null)
			return false;
		if (timeoutSeconds != null ?
				!timeoutSeconds.equals(jobConfig.timeoutSeconds) :
				jobConfig.timeoutSeconds != null)
			return false;
		if (showNormalLog != null ? !showNormalLog.equals(jobConfig.showNormalLog) : jobConfig.showNormalLog != null)
			return false;
		if (channelName != null ? !channelName.equals(jobConfig.channelName) : jobConfig.channelName != null)
			return false;
		if (jobType != null ? !jobType.equals(jobConfig.jobType) : jobConfig.jobType != null)
			return false;
		if (queueName != null ? !queueName.equals(jobConfig.queueName) : jobConfig.queueName != null)
			return false;
		if (loadLevel != null ? !loadLevel.equals(jobConfig.loadLevel) : jobConfig.loadLevel != null)
			return false;
		if (jobDegree != null ? !jobDegree.equals(jobConfig.jobDegree) : jobConfig.jobDegree != null)
			return false;
		if (enabledReport != null ? !enabledReport.equals(jobConfig.enabledReport) : jobConfig.enabledReport != null)
			return false;
		if (enabled != null ? !enabled.equals(jobConfig.enabled) : jobConfig.enabled != null)
			return false;
		if (preferList != null ? !preferList.equals(jobConfig.preferList) : jobConfig.preferList != null)
			return false;
		if (useDispreferList != null ?
				!useDispreferList.equals(jobConfig.useDispreferList) :
				jobConfig.useDispreferList != null)
			return false;
		if (localMode != null ? !localMode.equals(jobConfig.localMode) : jobConfig.localMode != null)
			return false;
		if (useSerial != null ? !useSerial.equals(jobConfig.useSerial) : jobConfig.useSerial != null)
			return false;
		if (isCopyJob != null ? !isCopyJob.equals(jobConfig.isCopyJob) : jobConfig.isCopyJob != null)
			return false;
		if (jobMode != null ? !jobMode.equals(jobConfig.jobMode) : jobConfig.jobMode != null)
			return false;
		if (customContext != null ? !customContext.equals(jobConfig.customContext) : jobConfig.customContext != null)
			return false;
		if (dependencies != null ? !dependencies.equals(jobConfig.dependencies) : jobConfig.dependencies != null)
			return false;
		if (rerun != null ? !dependencies.equals(jobConfig.rerun) : jobConfig.rerun != null)
			return false;
		return groups != null ? groups.equals(jobConfig.groups) : jobConfig.groups == null;
	}

	@Override
	public int hashCode() {
		int result = jobName != null ? jobName.hashCode() : 0;
		result = 31 * result + (jobClass != null ? jobClass.hashCode() : 0);
		result = 31 * result + (shardingTotalCount != null ? shardingTotalCount.hashCode() : 0);
		result = 31 * result + (timeZone != null ? timeZone.hashCode() : 0);
		result = 31 * result + (cron != null ? cron.hashCode() : 0);
		result = 31 * result + (pausePeriodDate != null ? pausePeriodDate.hashCode() : 0);
		result = 31 * result + (pausePeriodTime != null ? pausePeriodTime.hashCode() : 0);
		result = 31 * result + (shardingItemParameters != null ? shardingItemParameters.hashCode() : 0);
		result = 31 * result + (jobParameter != null ? jobParameter.hashCode() : 0);
		result = 31 * result + (processCountIntervalSeconds != null ? processCountIntervalSeconds.hashCode() : 0);
		result = 31 * result + (failover != null ? failover.hashCode() : 0);
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + (timeout4AlarmSeconds != null ? timeout4AlarmSeconds.hashCode() : 0);
		result = 31 * result + (timeoutSeconds != null ? timeoutSeconds.hashCode() : 0);
		result = 31 * result + (showNormalLog != null ? showNormalLog.hashCode() : 0);
		result = 31 * result + (channelName != null ? channelName.hashCode() : 0);
		result = 31 * result + (jobType != null ? jobType.hashCode() : 0);
		result = 31 * result + (queueName != null ? queueName.hashCode() : 0);
		result = 31 * result + (loadLevel != null ? loadLevel.hashCode() : 0);
		result = 31 * result + (jobDegree != null ? jobDegree.hashCode() : 0);
		result = 31 * result + (enabledReport != null ? enabledReport.hashCode() : 0);
		result = 31 * result + (enabled != null ? enabled.hashCode() : 0);
		result = 31 * result + (preferList != null ? preferList.hashCode() : 0);
		result = 31 * result + (useDispreferList != null ? useDispreferList.hashCode() : 0);
		result = 31 * result + (localMode != null ? localMode.hashCode() : 0);
		result = 31 * result + (useSerial != null ? useSerial.hashCode() : 0);
		result = 31 * result + (isCopyJob != null ? isCopyJob.hashCode() : 0);
		result = 31 * result + (jobMode != null ? jobMode.hashCode() : 0);
		result = 31 * result + (customContext != null ? customContext.hashCode() : 0);
		result = 31 * result + (dependencies != null ? dependencies.hashCode() : 0);
		result = 31 * result + (groups != null ? groups.hashCode() : 0);
		result = 31 * result + (rerun != null ? rerun.hashCode() : 0);
		return result;
	}
}
