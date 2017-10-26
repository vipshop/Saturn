package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
public class Timeout4AlarmJob {

	private String uuid;

	private boolean read;

	private String jobName;

	private String domainName;

	/** name and namespace */
	private String nns;

	/** degree of the domain */
	private String degree;

	private String jobDegree;

	private int timeout4AlarmSeconds;

	private List<Integer> timeoutItems = new ArrayList<>();

	public Timeout4AlarmJob() {
	}

	public Timeout4AlarmJob(String jobName, String domainName, String nns, String degree) {
		this.jobName = jobName;
		this.domainName = domainName;
		this.nns = nns;
		this.degree = degree;
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

	public String getJobDegree() {
		return jobDegree;
	}

	public void setJobDegree(String jobDegree) {
		this.jobDegree = jobDegree;
	}

	public int getTimeout4AlarmSeconds() {
		return timeout4AlarmSeconds;
	}

	public void setTimeout4AlarmSeconds(int timeout4AlarmSeconds) {
		this.timeout4AlarmSeconds = timeout4AlarmSeconds;
	}

	public List<Integer> getTimeoutItems() {
		return timeoutItems;
	}

	public void setTimeoutItems(List<Integer> timeoutItems) {
		this.timeoutItems = timeoutItems;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Timeout4AlarmJob that = (Timeout4AlarmJob) o;

		if (!jobName.equals(that.jobName)) return false;
		return domainName.equals(that.domainName);
	}

	@Override
	public int hashCode() {
		int result = jobName.hashCode();
		result = 31 * result + domainName.hashCode();
		return result;
	}
}
