package com.vip.saturn.job.exception;

/**
 * Alarm exception for handling job init fail
 */
public class JobInitAlarmException extends JobException {

	private static final long serialVersionUID = -4586641449270158434L;

	/**
	 * @param errorMessage if the args array are not empty then should be the format of error message; otherwise, it is the error message.
	 * @param args Arguments referenced by the format specifiers in the format string
	 */
	public JobInitAlarmException(String errorMessage, Object... args) {
		super(errorMessage, args);
	}

}
