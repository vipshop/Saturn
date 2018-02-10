package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public class ExecutorProvided {

	private String executorName;
	private ExecutorProvidedType type;
	private ExecutorProvidedStatus status;
	private Boolean noTraffic;
	private String ip;

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public ExecutorProvidedType getType() {
		return type;
	}

	public void setType(ExecutorProvidedType type) {
		this.type = type;
	}

	public ExecutorProvidedStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutorProvidedStatus status) {
		this.status = status;
	}

	public Boolean isNoTraffic() {
		return noTraffic;
	}

	public void setNoTraffic(Boolean noTraffic) {
		this.noTraffic = noTraffic;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
}
