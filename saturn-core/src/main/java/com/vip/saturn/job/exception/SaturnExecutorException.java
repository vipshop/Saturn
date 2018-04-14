package com.vip.saturn.job.exception;

public class SaturnExecutorException extends Exception {

	private final int code;

	public SaturnExecutorException(String message) {
		super(message);
		this.code = 0;
	}

	public SaturnExecutorException(int code, String message) {
		super(message);
		this.code = code;
	}

	public SaturnExecutorException(String message, Throwable cause) {
		super(message, cause);
		this.code = 0;
	}


	public int getCode() {
		return code;
	}

}

