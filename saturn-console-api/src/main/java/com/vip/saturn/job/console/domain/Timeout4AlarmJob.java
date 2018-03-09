package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
public class Timeout4AlarmJob extends AbstractAlarmJob {

	private int timeout4AlarmSeconds;

	private List<Integer> timeoutItems = new ArrayList<>();

	public Timeout4AlarmJob() {
	}

	public Timeout4AlarmJob(String jobName, String domainName, String nns, String degree) {
		super(jobName, domainName, nns, degree);
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
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Timeout4AlarmJob that = (Timeout4AlarmJob) o;

		if (!jobName.equals(that.jobName)) {
			return false;
		}
		return domainName.equals(that.domainName);
	}

	@Override
	public int hashCode() {
		int result = jobName.hashCode();
		result = 31 * result + domainName.hashCode();
		return result;
	}
}
