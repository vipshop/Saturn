package com.vip.saturn.job.console.domain;

import java.util.List;

/**
 * @author hebelala
 */
public class JobInfo extends JobConfig implements Comparable<JobInfo> {

	private static final long serialVersionUID = 1L;

	private List<String> timeZonesProvided;

	private List<ExecutorProvided> preferListProvided;

	private List<String> dependenciesProvided;

	private JobStatus status;

	// 下次开始时间：所有分片中下次最早的开始时间
	private String nextFireTime;

	// 最近开始时间：所有分片中最近最早的开始时间
	private String lastBeginTime;

	// 最近完成时间：所有分片中最近最晚的完成时间
	private String lastCompleteTime;

	private String shardingList;

	private String jobRate;

	private Boolean migrateEnabled;

	@Override
	public int compareTo(JobInfo o) {
		return getJobName().compareTo(o.getJobName());
	}

	public List<String> getTimeZonesProvided() {
		return timeZonesProvided;
	}

	public void setTimeZonesProvided(List<String> timeZonesProvided) {
		this.timeZonesProvided = timeZonesProvided;
	}

	public List<ExecutorProvided> getPreferListProvided() {
		return preferListProvided;
	}

	public void setPreferListProvided(List<ExecutorProvided> preferListProvided) {
		this.preferListProvided = preferListProvided;
	}

	public List<String> getDependenciesProvided() {
		return dependenciesProvided;
	}

	public void setDependenciesProvided(List<String> dependenciesProvided) {
		this.dependenciesProvided = dependenciesProvided;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
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

	public String getShardingList() {
		return shardingList;
	}

	public void setShardingList(String shardingList) {
		this.shardingList = shardingList;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		JobInfo jobInfo = (JobInfo) o;

		if (timeZonesProvided != null ? !timeZonesProvided.equals(jobInfo.timeZonesProvided)
				: jobInfo.timeZonesProvided != null) {
			return false;
		}
		if (preferListProvided != null ? !preferListProvided.equals(jobInfo.preferListProvided)
				: jobInfo.preferListProvided != null) {
			return false;
		}
		if (dependenciesProvided != null ? !dependenciesProvided.equals(jobInfo.dependenciesProvided)
				: jobInfo.dependenciesProvided != null) {
			return false;
		}
		if (status != jobInfo.status) {
			return false;
		}
		if (nextFireTime != null ? !nextFireTime.equals(jobInfo.nextFireTime) : jobInfo.nextFireTime != null) {
			return false;
		}
		if (lastBeginTime != null ? !lastBeginTime.equals(jobInfo.lastBeginTime) : jobInfo.lastBeginTime != null) {
			return false;
		}
		if (lastCompleteTime != null ? !lastCompleteTime.equals(jobInfo.lastCompleteTime)
				: jobInfo.lastCompleteTime != null) {
			return false;
		}
		if (shardingList != null ? !shardingList.equals(jobInfo.shardingList) : jobInfo.shardingList != null) {
			return false;
		}
		if (jobRate != null ? !jobRate.equals(jobInfo.jobRate) : jobInfo.jobRate != null) {
			return false;
		}
		return migrateEnabled != null ? migrateEnabled.equals(jobInfo.migrateEnabled) : jobInfo.migrateEnabled == null;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (timeZonesProvided != null ? timeZonesProvided.hashCode() : 0);
		result = 31 * result + (preferListProvided != null ? preferListProvided.hashCode() : 0);
		result = 31 * result + (dependenciesProvided != null ? dependenciesProvided.hashCode() : 0);
		result = 31 * result + (status != null ? status.hashCode() : 0);
		result = 31 * result + (nextFireTime != null ? nextFireTime.hashCode() : 0);
		result = 31 * result + (lastBeginTime != null ? lastBeginTime.hashCode() : 0);
		result = 31 * result + (lastCompleteTime != null ? lastCompleteTime.hashCode() : 0);
		result = 31 * result + (shardingList != null ? shardingList.hashCode() : 0);
		result = 31 * result + (jobRate != null ? jobRate.hashCode() : 0);
		result = 31 * result + (migrateEnabled != null ? migrateEnabled.hashCode() : 0);
		return result;
	}
}
