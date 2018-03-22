package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public class RequestResult {

	private int status;

	private String message;

	private Object obj;

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getObj() {
		return this.obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}
}
