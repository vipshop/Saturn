/**
 *
 */
package com.vip.saturn.job.console.domain;

/**
 * @author chembo.huang
 */
public class AbnormalJob extends AbstractAlarmJob {

	private String timeZone;

	private long nextFireTime;

	private String nextFireTimeWithTimeZoneFormat;

	private String cause;

	private long nextFireTimeAfterEnabledMtimeOrLastCompleteTime;

	public AbnormalJob() {
	}

	public AbnormalJob(String jobName, String domainName, String nns, String degree) {
		super(jobName, domainName, nns, degree);
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

	public long getNextFireTimeAfterEnabledMtimeOrLastCompleteTime() {
		return nextFireTimeAfterEnabledMtimeOrLastCompleteTime;
	}

	public void setNextFireTimeAfterEnabledMtimeOrLastCompleteTime(
			long nextFireTimeAfterEnabledMtimeOrLastCompleteTime) {
		this.nextFireTimeAfterEnabledMtimeOrLastCompleteTime = nextFireTimeAfterEnabledMtimeOrLastCompleteTime;
	}

	@Override
	public int hashCode() {
		int result = jobName.hashCode();
		result = 31 * result + domainName.hashCode();
		result = 31 * result + cause.hashCode();
		result = 31 * result + (int) (nextFireTimeAfterEnabledMtimeOrLastCompleteTime
				^ (nextFireTimeAfterEnabledMtimeOrLastCompleteTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AbnormalJob other = (AbnormalJob) obj;
		return this.getJobName().equals(other.getJobName()) && this.getDomainName().equals(other.getDomainName())
				&& this.getCause().equals(other.getCause()) && this.getNextFireTime() == other.getNextFireTime();
	}

	public enum Cause {
		NO_SHARDS, NOT_RUN, EXECUTORS_NOT_READY
	}

}
