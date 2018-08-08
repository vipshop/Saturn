package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public enum JobStatus {

	READY, STOPPED, RUNNING, STOPPING;

	public static JobStatus getJobStatus(String jobStatus) {
		switch (jobStatus) {
		case "0":
			return STOPPED;
		case "1":
			return READY;
		case "2":
			return RUNNING;
		case "3":
			return STOPPING;
		default:
			return null;
		}
	}
	
}
