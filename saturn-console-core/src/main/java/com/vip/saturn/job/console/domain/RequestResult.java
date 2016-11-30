package com.vip.saturn.job.console.domain;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class RequestResult {

	private boolean success;
	
	private String message;
	
	private Object obj;

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}

	public boolean isSuccess() {
		return this.success;
	}

	public String getMessage() {
		return this.message;
	}

	public Object getObj() {
		return this.obj;
	}
}
