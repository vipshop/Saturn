/**
 * vips Inc. Copyright (c) 2016 All Rights Reserved.
 */
package com.vip.saturn.job.console.exception;

/**
 * @author yangjuanying
 */
public class SaturnJobConsoleException extends Exception {

	private static final long serialVersionUID = -911821039567556368L;

	public SaturnJobConsoleException() {
	}

	public SaturnJobConsoleException(String message) {
		super(message);
	}

	public SaturnJobConsoleException(String message, Throwable cause) {
		super(message, cause);
	}

	public SaturnJobConsoleException(Throwable cause) {
		super(cause);
	}

	public SaturnJobConsoleException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
