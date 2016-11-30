package com.vip.saturn.job.basic;

import java.util.Map;

import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.internal.config.ConfigurationService;

public class SaturnApi {
	private ConfigurationService configService;

	public void setConfigService(ConfigurationService configService) {
		this.configService = configService;
	}

	public void updateJobCron(String jobName, String cron, Map<String, String> customContext) {
		try {
			configService.updateJobCron(jobName, cron, customContext);
		} catch (SaturnJobException e) {
			throw new RuntimeException(e);
		}
	}
}
