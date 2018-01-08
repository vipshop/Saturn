package com.vip.saturn.job.console.domain;

import java.util.List;

/**
 * @author hebelala
 */
public class ForecastCronResult {

	private String timeZone;
	private String cron;
	private List<String> nextFireTimes;

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public String getCron() {
		return cron;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}

	public List<String> getNextFireTimes() {
		return nextFireTimes;
	}

	public void setNextFireTimes(List<String> nextFireTimes) {
		this.nextFireTimes = nextFireTimes;
	}

}
