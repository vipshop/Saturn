package com.vip.saturn.job;

import java.util.HashSet;
import java.util.Set;

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

	public static Set<Integer> getAllSystemErrorGroups(){
		Set<Integer> resultSet = new HashSet<>();
		resultSet.add(SUCCESS);
		resultSet.add(FAIL);
		resultSet.add(TIMEOUT);
		resultSet.add(FAIL_NEED_RAISE_ALARM);

		return resultSet;
	}

}
