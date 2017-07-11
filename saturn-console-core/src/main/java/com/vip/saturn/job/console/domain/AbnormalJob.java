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
	
	private String uuid;
	
	private String jobName;
	
	private String domainName;
	
	/** name and namespace */
	private String nns;
	
	/** degree of the domain */
	private  String degree;
	
	private String jobDegree;

	private String timeZone;
	
	private long nextFireTime;

	private String nextFireTimeWithTimeZoneFormat;
	
	private String cause;
	
	private boolean read = false;
	
	private transient long nextFireTimeAfterEnabledMtime;
	
	public AbnormalJob() {
		
	}
	
	public AbnormalJob(String jobName, String domainName, String nns, String degree){
		this.jobName = jobName;
		this.domainName = domainName;
		this.nns = nns;
		this.degree = degree;
	}
	
	public enum Cause {
		NO_SHARDS, NOT_RUN, EXECUTORS_NOT_READY
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public void setNns(String nns) {
		this.nns = nns;
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

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public long getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(long nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	public String getNextFireTimeWithTimeZoneFormat() {
		return nextFireTimeWithTimeZoneFormat;
	}

	public void setNextFireTimeWithTimeZoneFormat(String nextFireTimeWithTimeZoneFormat) {
		this.nextFireTimeWithTimeZoneFormat = nextFireTimeWithTimeZoneFormat;
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

	public long getNextFireTimeAfterEnabledMtime() {
		return nextFireTimeAfterEnabledMtime;
	}

	public void setNextFireTimeAfterEnabledMtime(long nextFireTimeAfterEnabledMtime) {
		this.nextFireTimeAfterEnabledMtime = nextFireTimeAfterEnabledMtime;
	}
	
	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	@Override
	public int hashCode() {
		int result = jobName.hashCode();
		result = 31 * result + domainName.hashCode();
		result = 31 * result + cause.hashCode();
		result = 31 * result + (int) (nextFireTimeAfterEnabledMtime ^ (nextFireTimeAfterEnabledMtime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbnormalJob other = (AbnormalJob) obj;
		return this.getJobName().equals(other.getJobName()) && this.getDomainName().equals(other.getDomainName()) && this.getCause().equals(other.getCause())
				&& this.getNextFireTime() == other.getNextFireTime();
	}

	@Override
	public String toString() {
		return "AbnormalJob{" +
				"jobName='" + jobName + '\'' +
				", domainName='" + domainName + '\'' +
				", nns='" + nns + '\'' +
				", degree='" + degree + '\'' +
				", jobDegree='" + jobDegree + '\'' +
				", timeZone='" + timeZone + '\'' +
				", nextFireTime=" + nextFireTime +
				", nextFireTimeWithTimeZoneFormat='" + nextFireTimeWithTimeZoneFormat + '\'' +
				", cause='" + cause + '\'' +
				'}';
	}
}
