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

	protected String uuid;

	protected boolean read;

	protected String jobDegree;

	protected boolean rerun;

	public AbstractAlarmJob() {
	}

	public AbstractAlarmJob(String jobName, String domainName, String nns, String degree) {
		this.jobName = jobName;
		this.domainName = domainName;
		this.nns = nns;
		this.degree = degree;
		this.rerun = false;
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

	public String getUuid() {

		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public String getJobDegree() {
		return jobDegree;
	}

	public void setJobDegree(String jobDegree) {
		this.jobDegree = jobDegree;
	}

	public boolean isRerun() {
		return rerun;
	}

	public void setRerun(boolean rerun) {
		this.rerun = rerun;
	}
}
