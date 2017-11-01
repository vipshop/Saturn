package com.vip.saturn.job.exception;

/**
 * 
 * @author xiaopeng.he
 *
 */

public class SaturnJobException extends Exception {

	private static final long serialVersionUID = 1L;

	public static final int ILLEGAL_ARGUMENT = 0;

	public static final int JOB_NOT_FOUND = 1;

	public static final int OUT_OF_ZK_LIMIT_MEMORY = 3;

	public static final int JOB_NAME_INVALID = 4;

	public static final int SYSTEM_ERROR = 5;

	private int type;

	private String message;

	public SaturnJobException(String message) {
		this(SYSTEM_ERROR, message);
	}

	public SaturnJobException(int type, String message) {
		super();
		this.type = type;
		this.message = message;
	}

	public SaturnJobException(int type, String message, Throwable cause) {
		super(cause);
		this.type = type;
		this.message = message;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
