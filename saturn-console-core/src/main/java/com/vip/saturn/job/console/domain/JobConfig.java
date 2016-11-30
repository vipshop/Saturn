/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.domain;

import java.io.Serializable;
import java.util.Date;

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
    
    private String cron;
    
    private String pausePeriodDate;
    
    private String pausePeriodTime;
    
    private String shardingItemParameters;
    
    private String jobParameter;
    
    private Integer processCountIntervalSeconds;
    
    private Boolean failover;
    
    private String description;
    
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
    /** 作业的配置状态：true表示启用，false表示禁用，默认是禁用的*/
    private Boolean enabled;
    /** 从zk的config中读取到的已配置的预分配列表*/
    private String preferList;
    /** 从zk的servers节点读取到的预分配候选列表(servers下status节点存在的所有服务ip，即所有正常可运行的服务器)*/
    private String preferListCandidate;
    
    private Boolean useDispreferList;
    
    private Boolean localMode = false;
    
    private Boolean useSerial = false;
    
    private Boolean isCopyJob = false;
    
    private String originJobName;

    public void setDefaultValues() {
        timeoutSeconds = timeoutSeconds == null || timeoutSeconds < 0 ? 0 : timeoutSeconds;
        processCountIntervalSeconds = processCountIntervalSeconds == null ? 300 : processCountIntervalSeconds;
        showNormalLog = showNormalLog == null ? false : showNormalLog;
        loadLevel = loadLevel == null ? 1 : loadLevel;
        useDispreferList = useDispreferList == null ? true : useDispreferList;
        localMode = localMode == null ? false : localMode;
        useSerial = useSerial == null ? false : useSerial;
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

	public String getPreferListCandidate() {
		return preferListCandidate;
	}

	public void setPreferListCandidate(String preferListCandidate) {
		this.preferListCandidate = preferListCandidate;
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
    
}
