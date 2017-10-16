package com.vip.saturn.job.integrate.exception;

/**
 * @author timmy.hu
 */
public class UpdateJobConfigException extends Exception {

	private static final long serialVersionUID = 6261442630110127839L;

	public UpdateJobConfigException() {
		super();
	}

	public UpdateJobConfigException(String message) {
		super(message);
	}

	public UpdateJobConfigException(String message, Throwable cause) {
		super(message, cause);
	}

	public UpdateJobConfigException(Throwable cause) {
		super(cause);
	}

	public UpdateJobConfigException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
