package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.ForecastCronResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author hebelala
 */
public interface UtilsService {

	ForecastCronResult checkAndForecastCron(String timeZone, String cron) throws SaturnJobConsoleException;

}
