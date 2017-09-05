package com.vip.saturn.job.console.domain;

/**
 * @author xiaopeng.he
 */
public class AbnormalContainer {

	private static final long serialVersionUID = 1L;

	private String taskId;

	private String domainName;

	/** name and namespace */
	private String nns;

	/** degree of the domain */
	private String degree;

	private Integer configInstances;

	private Integer runningInstances;

	private String cause;

	public AbnormalContainer() {
	}

	public AbnormalContainer(String taskId, String domainName, String nns, String degree) {
		this.taskId = taskId;
		this.domainName = domainName;
		this.nns = nns;
		this.degree = degree;
	}

	public enum Cause {
		CONTAINER_INSTANCE_MISMATCH
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getNns() {
		return nns;
	}

	public void setNns(String nns) {
		this.nns = nns;
	}

	public String getDegree() {
		return degree;
	}

	public void setDegree(String degree) {
		this.degree = degree;
	}

	public Integer getConfigInstances() {
		return configInstances;
	}

	public void setConfigInstances(Integer configInstances) {
		this.configInstances = configInstances;
	}

	public Integer getRunningInstances() {
		return runningInstances;
	}

	public void setRunningInstances(Integer runningInstances) {
		this.runningInstances = runningInstances;
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

}
