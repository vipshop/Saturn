/**
 * 
 */
package com.vip.saturn.job.console.domain;


/**
 * @author chembo.huang
 *
 */
public class AbnormalJob {
	
	private static final long serialVersionUID = 1L;
	
	private final String jobName;
	
	private final String domainName;
	
	/** name and namespace */
	private final String nns;
	
	/** degree of the domain */
	private final String degree;
	
	private String jobDegree;
	
	private long nextFireTime;
	
	private String cause;
	
	public AbnormalJob(String jobName, String domainName, String nns, String degree){
		this.jobName = jobName;
		this.domainName = domainName;
		this.nns = nns;
		this.degree = degree;
	}
	
	public enum Cause {
		NO_SHARDS, NOT_RUN, EXECUTORS_NOT_READY
	}

	public String getJobDegree() {
		return jobDegree;
	}

	public void setJobDegree(String jobDegree) {
		this.jobDegree = jobDegree;
	}

	public long getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(long nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	public String getJobName() {
		return jobName;
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
		return "AbnormalJob [jobName=" + jobName + ", domainName=" + domainName
				+ ", nns=" + nns + ", degree=" + degree + ", jobDegree="
				+ jobDegree + ", nextFireTime=" + nextFireTime + ", cause="
				+ cause + "]";
	}
	
}
