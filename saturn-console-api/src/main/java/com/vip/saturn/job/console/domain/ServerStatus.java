package com.vip.saturn.job.console.domain;

/**
 * @author chembo.huang
 */
public enum ServerStatus {

	ONLINE, OFFLINE;

	/**
	 * if the status ephemeral node doesn't exist, it's offline.
	 *
	 * @param status should be ready or running or null.
	 */
	public static ServerStatus getServerStatus(final String status) {
		if (null == status) {
			return ServerStatus.OFFLINE;
		}
		return ServerStatus.ONLINE;
	}
}