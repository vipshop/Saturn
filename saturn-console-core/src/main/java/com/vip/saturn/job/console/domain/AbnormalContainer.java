package com.vip.saturn.job.console.domain;


/**
 * @author xiaopeng.he
 */
public class AbnormalContainer {
	
	private static final long serialVersionUID = 1L;

    private final String taskId;

    private final String domainName;

    /** name and namespace */
    private final String nns;

    /** degree of the domain */
    private final String degree;

    private Integer configInstances;

    private Integer runningInstances;

    private String cause;

    public AbnormalContainer(String taskId, String domainName, String nns, String degree) {
		this.taskId = taskId;
		this.domainName = domainName;
		this.nns = nns;
		this.degree = degree;
	}
    
    public enum Cause {
        CONTAINER_INSTANCE_MISMATCH
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

	public String getTaskId() {
		return taskId;
	}

	public String getDomainName() {
		return domainName;
	}

	public String getNns() {
		return nns;
	}

	public String getDegree() {
		return degree;
	}

	@Override
	public String toString() {
		return "AbnormalContainer [taskId=" + taskId + ", domainName="
				+ domainName + ", nns=" + nns + ", degree=" + degree
				+ ", configInstances=" + configInstances
				+ ", runningInstances=" + runningInstances + ", cause=" + cause
				+ "]";
	}

}
