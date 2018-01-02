package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public class DependencyJob {

	private String jobName;
	private boolean enabled;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
