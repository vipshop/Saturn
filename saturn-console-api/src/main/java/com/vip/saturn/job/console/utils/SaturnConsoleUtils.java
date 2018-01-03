package com.vip.saturn.job.console.utils;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Utils for saturn console.
 *
 * @author kfchu
 */
public class SaturnConsoleUtils {

	private static DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
			.withLocale(Locale.SIMPLIFIED_CHINESE);


	public static String parseMillisecond2DisplayTime(String longInStr) {
		if (StringUtils.isBlank(longInStr)) {
			return null;
		}
		return dtf.print(new DateTime(Long.parseLong(longInStr)));
	}
}
