/**
 * vips Inc. Copyright (c) 2016 All Rights Reserved.
 */
package com.vip.saturn.job.console.exception;

/**
 * @author yangjuanying
 */
public class SaturnJobConsoleException extends Exception {

	public static final int ERROR_CODE_NOT_EXISTED = 1;

	public static final int ERROR_CODE_BAD_REQUEST = 2;

	public static final int ERROR_CODE_INTERNAL_ERROR = 0;

	public static final int ERROR_CODE_AUTHN_FAIL = 4;

	private int errorCode = ERROR_CODE_INTERNAL_ERROR;

	public SaturnJobConsoleException() {
	}

	public SaturnJobConsoleException(String message) {
		super(message);
		this.errorCode = ERROR_CODE_INTERNAL_ERROR;
	}

	public SaturnJobConsoleException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public SaturnJobConsoleException(String message, Throwable cause) {
		super(message, cause);
		this.errorCode = ERROR_CODE_INTERNAL_ERROR;
	}

	public SaturnJobConsoleException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public SaturnJobConsoleException(Throwable cause) {
		super(cause);
	}

	public SaturnJobConsoleException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public int getErrorCode() {
		return errorCode;
	}
}
