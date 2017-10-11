package com.vip.saturn.job.integrate.entity;

public class JobConfigInfo {

	private String namespace;

	private String jobName;

	private String perferList;

	public JobConfigInfo() {

	}

	public JobConfigInfo(String namespace, String jobName, String perferList) {
		this.namespace = namespace;
		this.jobName = jobName;
		this.perferList = perferList;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getPerferList() {
		return perferList;
	}

	public void setPerferList(String perferList) {
		this.perferList = perferList;
	}

	@Override
	public String toString() {
		return "JobConfigInfo [namespace=" + namespace + ", jobName=" + jobName + ", perferList=" + perferList + "]";
	}
}
