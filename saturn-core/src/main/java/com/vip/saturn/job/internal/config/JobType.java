package com.vip.saturn.job.internal.config;

public enum JobType {

	JAVA_JOB("JAVA_JOB"),
	SHELL_JOB("SHELL_JOB"),
	MSG_JOB("MSG_JOB"),
	UNKOWN_JOB("UNKOWN_JOB");

	private String name;

	JobType(String name) {
		this.name = name;
	}
}
