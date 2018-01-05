package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public class ImportJobResult {

	private String jobName;
	private boolean success;
	private String message;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
