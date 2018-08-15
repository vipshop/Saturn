package com.vip.saturn.job;

/**
 *
 * @author xiaopeng.he
 *
 */
public final class SaturnSystemReturnCode {

	public static final int SUCCESS = 0;

	public static final int SYSTEM_FAIL = 1;

	public static final int USER_FAIL = 2;

	/** 作业执行不参与计数 */
	public static final int JOB_NO_COUNT = 9999;

	public static boolean include(int returnCode) {
		return returnCode == SUCCESS || returnCode == SYSTEM_FAIL || returnCode == USER_FAIL
				|| returnCode == JOB_NO_COUNT;
	}

}
