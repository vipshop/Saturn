package com.vip.saturn.it;

/**
 * 代表执行作业的类型
 * 
 * @author linzhaoming
 *
 */
public enum JobType {
	JAVA_JOB, MSG_JOB, SHELL_JOB, VSHELL, UNKOWN_JOB;

	public static JobType getJobType(String typeName) {
		if (JAVA_JOB.toString().equalsIgnoreCase(typeName)) {
			return JAVA_JOB;
		}
		if (MSG_JOB.toString().equalsIgnoreCase(typeName)) {
			return MSG_JOB;
		}
		if (SHELL_JOB.toString().equalsIgnoreCase(typeName)) {
			return SHELL_JOB;
		}
		if (VSHELL.toString().equalsIgnoreCase(typeName)) {
			return VSHELL;
		}
		return UNKOWN_JOB;
	}

}
