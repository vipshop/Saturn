package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public enum JobType {

	JAVA_JOB, SHELL_JOB, PASSIVE_JAVA_JOB, PASSIVE_SHELL_JOB, MSG_JOB, VSHELL, UNKNOWN_JOB;

	public static final JobType getJobType(String jobType) {
		try {
			return valueOf(jobType);
		} catch (Exception e) {
			return UNKNOWN_JOB;
		}
	}

	public static boolean isCron(JobType jobType) {
		return JAVA_JOB == jobType || SHELL_JOB == jobType;
	}

	public static boolean isPassive(JobType jobType) {
		return PASSIVE_JAVA_JOB == jobType || PASSIVE_SHELL_JOB == jobType;
	}

	public static boolean isJava(JobType jobType) {
		return JAVA_JOB == jobType || PASSIVE_JAVA_JOB == jobType || MSG_JOB == jobType;
	}

	public static boolean isShell(JobType jobType) {
		return SHELL_JOB == jobType || PASSIVE_SHELL_JOB == jobType || VSHELL == jobType;
	}

	public static boolean isMsg(JobType jobType) {
		return MSG_JOB == jobType || VSHELL == jobType;
	}

}
