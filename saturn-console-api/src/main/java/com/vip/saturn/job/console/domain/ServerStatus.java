package com.vip.saturn.job.console.domain;

import com.google.common.base.Strings;

/**
 * @author chembo.huang
 *
 */
public enum ServerStatus {

	ONLINE, OFFLINE;

	/**
	 * if the status ephemeral node doesn't exist, it's offline.
	 * @param status should be ready or running or null.
	 * @return
	 */
	public static ServerStatus getServerStatus(final String status) {
		if (Strings.isNullOrEmpty(status)) {
			return ServerStatus.OFFLINE;
		}
		return ServerStatus.ONLINE;
	}
}