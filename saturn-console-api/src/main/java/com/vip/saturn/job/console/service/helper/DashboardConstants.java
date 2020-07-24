/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DashboardConstants {

	public static final long INTERVAL_DELTA_IN_SECOND = 10 * 1000L;
	private static final Logger log = LoggerFactory.getLogger(DashboardConstants.class);
	public static int REFRESH_INTERVAL_IN_MINUTE = 7;
	public static long ALLOW_DELAY_MILLIONSECONDS = 60L * 1000L * REFRESH_INTERVAL_IN_MINUTE;
	public static long ALLOW_CONTAINER_DELAY_MILLIONSECONDS = 60L * 1000L * 3;
	/**
	 * 当JOB有其他item处于running时，job not running的告警延迟
	 */
	public static long NOT_RUNNING_WARN_DELAY_MS_WHEN_JOB_RUNNING = 1000L * 60L * 60L * 2L;

	static {
		String refreshInterval = System.getProperty("VIP_SATURN_DASHBOARD_REFRESH_INTERVAL_MINUTE",
				System.getenv("VIP_SATURN_DASHBOARD_REFRESH_INTERVAL_MINUTE"));
		if (refreshInterval != null) {
			try {
				REFRESH_INTERVAL_IN_MINUTE = Integer.parseInt(refreshInterval);
				ALLOW_DELAY_MILLIONSECONDS = 60 * 1000L * REFRESH_INTERVAL_IN_MINUTE;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		String notRunningWarnDelay = System.getProperty("VIP_SATURN_DASHBOARD_NOT_RUNNING_WARN_DELAY_MS_WHEN_JOB_RUNNING",
				System.getenv("VIP_SATURN_DASHBOARD_NOT_RUNNING_WARN_DELAY_MS_WHEN_JOB_RUNNING"));
		if (notRunningWarnDelay != null) {
			try {
				NOT_RUNNING_WARN_DELAY_MS_WHEN_JOB_RUNNING = Long.parseLong(notRunningWarnDelay);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

}
