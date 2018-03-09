package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public enum JobType {

	JAVA_JOB, SHELL_JOB, MSG_JOB, UNKOWN_JOB;

	public static final JobType getJobType(String jobType) {
		try {
			return valueOf(jobType);
		} catch (Exception e) {
			return UNKOWN_JOB;
		}
	}

}
