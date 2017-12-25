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

package com.vip.saturn.job.console.domain;

import com.vip.saturn.job.console.domain.JobBriefInfo.JobType;
import com.vip.saturn.job.console.utils.SaturnConstants;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author chembo.huang
 *
 */
public class JobConfig implements Serializable {

	private static final long serialVersionUID = 7366583369937964951L;

	private Integer rownum;

	private Long id;

	private String jobName;

	private String jobClass;

	private Integer shardingTotalCount;

	private String timeZone;

	private List<String> timeZonesProvided;

	private String cron;

	private String pausePeriodDate;

	private String pausePeriodTime;

	private String shardingItemParameters;

	private String jobParameter;

	private Integer processCountIntervalSeconds;

	private Boolean failover;

	private String description;

	private Integer timeout4AlarmSeconds;

	private Integer timeoutSeconds;

	private Boolean showNormalLog;

	private String channelName;

	private String jobType;

	private String queueName;

	private String createBy;

	private String lastUpdateBy;

	private Date createTime;

	private Date lastUpdateTime;

	private String namespace;

	private String zkList;

	private Integer loadLevel;
	/** 作业重要等级 */
	private Integer jobDegree;
	/** 作业是否上报执行信息：true表示启用，false表示禁用，对于定时作业默认是启用，对于消息作业默认是禁用 */
	private Boolean enabledReport;
	/** 作业的配置状态：true表示启用，false表示禁用，默认是禁用的 */
	private Boolean enabled;
	/** 从zk的config中读取到的已配置的预分配列表 */
	private String preferList;
	/** 从zk的servers节点读取到的预分配候选列表(servers下status节点存在的所有服务ip，即所有正常可运行的服务器) */
	private List<ExecutorProvided> preferListProvided;

	private Boolean useDispreferList;

	private Boolean localMode = false;

	private Boolean useSerial = false;

	private Boolean isCopyJob = false;

	private String originJobName;

	private String jobMode;

	private String customContext;

	private String dependencies;

	private String groups;

	private List<String> dependenciesProvided;

	public void setDefaultValues() {
		timeZone = timeZone == null ? SaturnConstants.TIME_ZONE_ID_DEFAULT : timeZone;
		timeout4AlarmSeconds = timeout4AlarmSeconds == null || timeout4AlarmSeconds < 0 ? 0 : timeout4AlarmSeconds;
		timeoutSeconds = timeoutSeconds == null || timeoutSeconds < 0 ? 0 : timeoutSeconds;
		processCountIntervalSeconds = processCountIntervalSeconds == null ? 300 : processCountIntervalSeconds;
		showNormalLog = showNormalLog == null ? false : showNormalLog;
		loadLevel = loadLevel == null ? 1 : loadLevel;
		useDispreferList = useDispreferList == null ? true : useDispreferList;
		localMode = localMode == null ? false : localMode;
		useSerial = useSerial == null ? false : useSerial;
		jobDegree = jobDegree == null ? 0 : jobDegree;
		if (enabledReport == null) {
			if (JobType.JAVA_JOB.name().equals(jobType) || JobType.SHELL_JOB.name().equals(jobType)) {
				enabledReport = true;
			} else {
				enabledReport = false;
			}
		}
		jobMode = jobMode == null ? "" : jobMode;
		dependencies = dependencies == null ? "" : dependencies;
		groups = groups == null ? "" : groups;
	}

	public Integer getRownum() {
		return rownum;
	}

	public void setRownum(Integer rownum) {
		this.rownum = rownum;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public List<String> getTimeZonesProvided() {
		return timeZonesProvided;
	}

	public void setTimeZonesProvided(List<String> timeZonesProvided) {
		this.timeZonesProvided = timeZonesProvided;
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

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public String getLastUpdateBy() {
		return lastUpdateBy;
	}

	public void setLastUpdateBy(String lastUpdateBy) {
		this.lastUpdateBy = lastUpdateBy;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getZkList() {
		return zkList;
	}

	public void setZkList(String zkList) {
		this.zkList = zkList;
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

	public List<ExecutorProvided> getPreferListProvided() {
		return preferListProvided;
	}

	public void setPreferListProvided(List<ExecutorProvided> preferListProvided) {
		this.preferListProvided = preferListProvided;
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

	public Boolean getIsCopyJob() {
		return isCopyJob;
	}

	public void setIsCopyJob(Boolean isCopyJob) {
		this.isCopyJob = isCopyJob;
	}

	public String getOriginJobName() {
		return originJobName;
	}

	public void setOriginJobName(String originJobName) {
		this.originJobName = originJobName;
	}

	public Boolean getEnabledReport() {
		return enabledReport;
	}

	public void setEnabledReport(Boolean enabledReport) {
		this.enabledReport = enabledReport;
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

	public List<String> getDependenciesProvided() {
		return dependenciesProvided;
	}

	public void setDependenciesProvided(List<String> dependenciesProvided) {
		this.dependenciesProvided = dependenciesProvided;
	}

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JobConfig jobConfig = (JobConfig) o;

		if (rownum != null ? !rownum.equals(jobConfig.rownum) : jobConfig.rownum != null) return false;
		if (id != null ? !id.equals(jobConfig.id) : jobConfig.id != null) return false;
		if (jobName != null ? !jobName.equals(jobConfig.jobName) : jobConfig.jobName != null) return false;
		if (jobClass != null ? !jobClass.equals(jobConfig.jobClass) : jobConfig.jobClass != null) return false;
		if (shardingTotalCount != null ? !shardingTotalCount.equals(jobConfig.shardingTotalCount) : jobConfig.shardingTotalCount != null)
			return false;
		if (timeZone != null ? !timeZone.equals(jobConfig.timeZone) : jobConfig.timeZone != null) return false;
		if (timeZonesProvided != null ? !timeZonesProvided.equals(jobConfig.timeZonesProvided) : jobConfig.timeZonesProvided != null)
			return false;
		if (cron != null ? !cron.equals(jobConfig.cron) : jobConfig.cron != null) return false;
		if (pausePeriodDate != null ? !pausePeriodDate.equals(jobConfig.pausePeriodDate) : jobConfig.pausePeriodDate != null)
			return false;
		if (pausePeriodTime != null ? !pausePeriodTime.equals(jobConfig.pausePeriodTime) : jobConfig.pausePeriodTime != null)
			return false;
		if (shardingItemParameters != null ? !shardingItemParameters.equals(jobConfig.shardingItemParameters) : jobConfig.shardingItemParameters != null)
			return false;
		if (jobParameter != null ? !jobParameter.equals(jobConfig.jobParameter) : jobConfig.jobParameter != null)
			return false;
		if (processCountIntervalSeconds != null ? !processCountIntervalSeconds.equals(jobConfig.processCountIntervalSeconds) : jobConfig.processCountIntervalSeconds != null)
			return false;
		if (failover != null ? !failover.equals(jobConfig.failover) : jobConfig.failover != null) return false;
		if (description != null ? !description.equals(jobConfig.description) : jobConfig.description != null)
			return false;
		if (timeout4AlarmSeconds != null ? !timeout4AlarmSeconds.equals(jobConfig.timeout4AlarmSeconds) : jobConfig.timeout4AlarmSeconds != null)
			return false;
		if (timeoutSeconds != null ? !timeoutSeconds.equals(jobConfig.timeoutSeconds) : jobConfig.timeoutSeconds != null)
			return false;
		if (showNormalLog != null ? !showNormalLog.equals(jobConfig.showNormalLog) : jobConfig.showNormalLog != null)
			return false;
		if (channelName != null ? !channelName.equals(jobConfig.channelName) : jobConfig.channelName != null)
			return false;
		if (jobType != null ? !jobType.equals(jobConfig.jobType) : jobConfig.jobType != null) return false;
		if (queueName != null ? !queueName.equals(jobConfig.queueName) : jobConfig.queueName != null) return false;
		if (createBy != null ? !createBy.equals(jobConfig.createBy) : jobConfig.createBy != null) return false;
		if (lastUpdateBy != null ? !lastUpdateBy.equals(jobConfig.lastUpdateBy) : jobConfig.lastUpdateBy != null)
			return false;
		if (createTime != null ? !createTime.equals(jobConfig.createTime) : jobConfig.createTime != null) return false;
		if (lastUpdateTime != null ? !lastUpdateTime.equals(jobConfig.lastUpdateTime) : jobConfig.lastUpdateTime != null)
			return false;
		if (namespace != null ? !namespace.equals(jobConfig.namespace) : jobConfig.namespace != null) return false;
		if (zkList != null ? !zkList.equals(jobConfig.zkList) : jobConfig.zkList != null) return false;
		if (loadLevel != null ? !loadLevel.equals(jobConfig.loadLevel) : jobConfig.loadLevel != null) return false;
		if (jobDegree != null ? !jobDegree.equals(jobConfig.jobDegree) : jobConfig.jobDegree != null) return false;
		if (enabledReport != null ? !enabledReport.equals(jobConfig.enabledReport) : jobConfig.enabledReport != null)
			return false;
		if (enabled != null ? !enabled.equals(jobConfig.enabled) : jobConfig.enabled != null) return false;
		if (preferList != null ? !preferList.equals(jobConfig.preferList) : jobConfig.preferList != null) return false;
		if (preferListProvided != null ? !preferListProvided.equals(jobConfig.preferListProvided) : jobConfig.preferListProvided != null)
			return false;
		if (useDispreferList != null ? !useDispreferList.equals(jobConfig.useDispreferList) : jobConfig.useDispreferList != null)
			return false;
		if (localMode != null ? !localMode.equals(jobConfig.localMode) : jobConfig.localMode != null) return false;
		if (useSerial != null ? !useSerial.equals(jobConfig.useSerial) : jobConfig.useSerial != null) return false;
		if (isCopyJob != null ? !isCopyJob.equals(jobConfig.isCopyJob) : jobConfig.isCopyJob != null) return false;
		if (originJobName != null ? !originJobName.equals(jobConfig.originJobName) : jobConfig.originJobName != null)
			return false;
		if (jobMode != null ? !jobMode.equals(jobConfig.jobMode) : jobConfig.jobMode != null) return false;
		if (customContext != null ? !customContext.equals(jobConfig.customContext) : jobConfig.customContext != null)
			return false;
		if (dependencies != null ? !dependencies.equals(jobConfig.dependencies) : jobConfig.dependencies != null)
			return false;
		if (groups != null ? !groups.equals(jobConfig.groups) : jobConfig.groups != null) return false;
		return dependenciesProvided != null ? dependenciesProvided.equals(jobConfig.dependenciesProvided) : jobConfig.dependenciesProvided == null;
	}

	@Override
	public int hashCode() {
		int result = rownum != null ? rownum.hashCode() : 0;
		result = 31 * result + (id != null ? id.hashCode() : 0);
		result = 31 * result + (jobName != null ? jobName.hashCode() : 0);
		result = 31 * result + (jobClass != null ? jobClass.hashCode() : 0);
		result = 31 * result + (shardingTotalCount != null ? shardingTotalCount.hashCode() : 0);
		result = 31 * result + (timeZone != null ? timeZone.hashCode() : 0);
		result = 31 * result + (timeZonesProvided != null ? timeZonesProvided.hashCode() : 0);
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
		result = 31 * result + (createBy != null ? createBy.hashCode() : 0);
		result = 31 * result + (lastUpdateBy != null ? lastUpdateBy.hashCode() : 0);
		result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
		result = 31 * result + (lastUpdateTime != null ? lastUpdateTime.hashCode() : 0);
		result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
		result = 31 * result + (zkList != null ? zkList.hashCode() : 0);
		result = 31 * result + (loadLevel != null ? loadLevel.hashCode() : 0);
		result = 31 * result + (jobDegree != null ? jobDegree.hashCode() : 0);
		result = 31 * result + (enabledReport != null ? enabledReport.hashCode() : 0);
		result = 31 * result + (enabled != null ? enabled.hashCode() : 0);
		result = 31 * result + (preferList != null ? preferList.hashCode() : 0);
		result = 31 * result + (preferListProvided != null ? preferListProvided.hashCode() : 0);
		result = 31 * result + (useDispreferList != null ? useDispreferList.hashCode() : 0);
		result = 31 * result + (localMode != null ? localMode.hashCode() : 0);
		result = 31 * result + (useSerial != null ? useSerial.hashCode() : 0);
		result = 31 * result + (isCopyJob != null ? isCopyJob.hashCode() : 0);
		result = 31 * result + (originJobName != null ? originJobName.hashCode() : 0);
		result = 31 * result + (jobMode != null ? jobMode.hashCode() : 0);
		result = 31 * result + (customContext != null ? customContext.hashCode() : 0);
		result = 31 * result + (dependencies != null ? dependencies.hashCode() : 0);
		result = 31 * result + (groups != null ? groups.hashCode() : 0);
		result = 31 * result + (dependenciesProvided != null ? dependenciesProvided.hashCode() : 0);
		return result;
	}
}
