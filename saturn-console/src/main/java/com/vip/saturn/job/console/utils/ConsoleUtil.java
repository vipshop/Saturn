package com.vip.saturn.job.console.utils;

import com.vip.saturn.job.console.SaturnEnvProperties;

public class ConsoleUtil {

	public static boolean isDashboardOn() {
		return SaturnEnvProperties.SATURN_CONSOLE_DB_URL != null
				&& SaturnEnvProperties.SATURN_CONSOLE_DB_USERNAME != null
				&& SaturnEnvProperties.SATURN_CONSOLE_DB_PASSWORD != null;
	}
}
