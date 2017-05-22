package com.vip.saturn.job;

/**
 * 
 * @author xiaopeng.he
 *
 */
public final class SaturnSystemErrorGroup {

	public static final int SUCCESS = 200;

	// general fail
	public static final int FAIL = 500;

	public static final int TIMEOUT = 550;

	// alarm will be raised with this error code
	public static final int FAIL_NEED_RAISE_ALARM = 551;
	
}
