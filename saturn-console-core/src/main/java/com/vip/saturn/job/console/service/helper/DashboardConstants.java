package com.vip.saturn.job.console.service.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author timmy.hu
 */
public class DashboardConstants {

	private static final Logger log = LoggerFactory.getLogger(DashboardConstants.class);

	public static int REFRESH_INTERVAL_IN_MINUTE = 7;

	public static long ALLOW_DELAY_MILLIONSECONDS = 60L * 1000L * REFRESH_INTERVAL_IN_MINUTE;

	public static long ALLOW_CONTAINER_DELAY_MILLIONSECONDS = 60L * 1000L * 3;

	public static final long INTERVAL_DELTA_IN_SECOND = 10 * 1000L;

	static {
		String refreshInterval = System.getProperty("VIP_SATURN_DASHBOARD_REFRESH_INTERVAL_MINUTE",
				System.getenv("VIP_SATURN_DASHBOARD_REFRESH_INTERVAL_MINUTE"));
		if (refreshInterval != null) {
			try {
				REFRESH_INTERVAL_IN_MINUTE = Integer.valueOf(refreshInterval);
				ALLOW_DELAY_MILLIONSECONDS = 60 * 1000 * REFRESH_INTERVAL_IN_MINUTE;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

}
