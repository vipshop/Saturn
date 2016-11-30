package com.vip.saturn.job.exception;

/**
 * 
 * @author xiaopeng.he
 *
 */

public class SaturnJobException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int CRON_VALID = 0;

	public static final int JOB_NOT_FOUND = 1;

	public static final int OUT_OF_ZK_LIMIT_MEMORY = 3;
	
	public static final int JOBNAME_VALID = 4;

	private int type;

	private String message;

	public SaturnJobException(int type, String message) {
		super();
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
