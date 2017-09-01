package com.vip.saturn.job.console.domain;

/**
 * @author xiaopeng.he
 */
public class RestApiJobInfo {

	private String jobName;

	private String description;

	private Boolean enabled;

	private String runningStatus;

	private RestApiJobConfig jobConfig;

	private RestApiJobStatistics statistics;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getRunningStatus() {
		return runningStatus;
	}

	public void setRunningStatus(String runningStatus) {
		this.runningStatus = runningStatus;
	}

	public RestApiJobConfig getJobConfig() {
		return jobConfig;
	}

	public void setJobConfig(RestApiJobConfig jobConfig) {
		this.jobConfig = jobConfig;
	}

	public RestApiJobStatistics getStatistics() {
		return statistics;
	}

	public void setStatistics(RestApiJobStatistics statistics) {
		this.statistics = statistics;
	}
}
