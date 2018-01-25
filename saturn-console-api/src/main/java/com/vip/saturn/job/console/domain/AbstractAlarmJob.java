package com.vip.saturn.job.console.domain;

public class AbstractAlarmJob {

	protected String jobName;

	protected String domainName;

	/**
	 * name and namespace
	 */
	protected String nns;

	/**
	 * degree of the domain
	 */
	protected String degree;

	public AbstractAlarmJob() {
	}

	public AbstractAlarmJob(String jobName, String domainName, String nns, String degree) {
		this.jobName = jobName;
		this.domainName = domainName;
		this.nns = nns;
		this.degree = degree;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}
}
