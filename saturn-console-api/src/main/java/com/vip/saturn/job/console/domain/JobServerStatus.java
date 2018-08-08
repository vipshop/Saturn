package com.vip.saturn.job.console.domain;

import java.io.Serializable;

public class JobServerStatus implements Serializable {

	private String jobName;

	private String executorName;

	private ServerStatus serverStatus;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public ServerStatus getServerStatus() {
		return serverStatus;
	}

	public void setServerStatus(ServerStatus serverStatus) {
		this.serverStatus = serverStatus;
	}
}
