package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.ForecastCronResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.UtilsService;
import com.vip.saturn.job.console.utils.CronExpression;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @author hebelala
 */
public class UtilsServiceImpl implements UtilsService {

	@Override
	public ForecastCronResult checkAndForecastCron(final String timeZone, final String cron)
			throws SaturnJobConsoleException {
		if (StringUtils.isBlank(timeZone)) {
			throw new SaturnJobConsoleException("timeZone不能为空");
		}
		if (StringUtils.isBlank(cron)) {
			throw new SaturnJobConsoleException("cron不能为空");
		}
		String timeZoneTrim = timeZone.trim();
		String cronTrim = cron.trim();
		if (!SaturnConstants.TIME_ZONE_IDS.contains(timeZoneTrim)) {
			throw new SaturnJobConsoleException(String.format("timeZone(%s)无效", timeZoneTrim));
		}
		TimeZone tz = TimeZone.getTimeZone(timeZoneTrim);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormat.setTimeZone(tz);
		CronExpression cronExpression = null;
		try {
			cronExpression = new CronExpression(cronTrim);
		} catch (ParseException e) {
			throw new SaturnJobConsoleException(String.format("cron(%s)格式错误:%s", cronTrim, e.getMessage()));
		}
		cronExpression.setTimeZone(tz);
		ForecastCronResult forecastCronResult = new ForecastCronResult();
		forecastCronResult.setTimeZone(timeZoneTrim);
		forecastCronResult.setCron(cronTrim);
		forecastCronResult.setNextFireTimes(new ArrayList<String>());
		Date now = new Date();
		for (int i = 0; i < 10; i++) {
			Date next = cronExpression.getNextValidTimeAfter(now);
			if (next != null) {
				forecastCronResult.getNextFireTimes().add(dateFormat.format(next));
				now = next;
			}
		}
		if (forecastCronResult.getNextFireTimes().isEmpty()) {
			throw new SaturnJobConsoleException(String.format("cron(%s)可能描述的是一个过去的时间，将不会被执行", cronTrim));
		}
		return forecastCronResult;
	}

	@Override
	public List<String> getTimeZones() throws SaturnJobConsoleException {
		return SaturnConstants.TIME_ZONE_IDS;
	}
}
