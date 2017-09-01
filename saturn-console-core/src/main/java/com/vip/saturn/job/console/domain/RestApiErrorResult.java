package com.vip.saturn.job.console.domain;

/**
 * @author xiaopeng.he
 */
public class RestApiErrorResult {

	private String message;

	public RestApiErrorResult() {
	}

	public RestApiErrorResult(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
