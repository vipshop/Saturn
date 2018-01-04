package com.vip.saturn.job.console.domain;

import java.util.List;

/**
 * 作业配置页面的VO
 *
 * @author hebelala
 */
public class JobConfigVo extends JobConfig {

	private static final long serialVersionUID = 1L;

	private List<String> timeZonesProvided;

	private List<ExecutorProvided> preferListProvided;

	private List<String> dependenciesProvided;

	private JobStatus status;

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

		JobConfigVo that = (JobConfigVo) o;

		if (timeZonesProvided != null ? !timeZonesProvided.equals(that.timeZonesProvided)
				: that.timeZonesProvided != null) {
			return false;
		}
		if (preferListProvided != null ? !preferListProvided.equals(that.preferListProvided)
				: that.preferListProvided != null) {
			return false;
		}
		if (dependenciesProvided != null ? !dependenciesProvided.equals(that.dependenciesProvided)
				: that.dependenciesProvided != null) {
			return false;
		}
		return status == that.status;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (timeZonesProvided != null ? timeZonesProvided.hashCode() : 0);
		result = 31 * result + (preferListProvided != null ? preferListProvided.hashCode() : 0);
		result = 31 * result + (dependenciesProvided != null ? dependenciesProvided.hashCode() : 0);
		result = 31 * result + (status != null ? status.hashCode() : 0);
		return result;
	}
}
