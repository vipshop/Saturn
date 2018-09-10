package com.vip.saturn.job.exception;

/**
 * Exception for handling job init fail.
 */
public class JobInitException extends JobException {

	public JobInitException(String errorMessage, Object... args) {
		super(errorMessage, args);
	}

	public JobInitException(Exception cause) {
		super(cause);
	}

	public JobInitException(Throwable cause) {
		super(cause);
	}
}
