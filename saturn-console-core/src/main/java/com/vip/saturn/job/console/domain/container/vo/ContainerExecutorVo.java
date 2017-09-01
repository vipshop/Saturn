package com.vip.saturn.job.console.domain.container.vo;

/**
 * @author hebelala
 */
public class ContainerExecutorVo {

	private String executorName;
	private String ip;
	private String runningJobNames;

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getRunningJobNames() {
		return runningJobNames;
	}

	public void setRunningJobNames(String runningJobNames) {
		this.runningJobNames = runningJobNames;
	}
}
