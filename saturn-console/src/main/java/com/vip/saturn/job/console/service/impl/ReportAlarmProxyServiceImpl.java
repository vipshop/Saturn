package com.vip.saturn.job.console.service.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.integrate.service.ReportAlarmProxyService;
import com.vip.saturn.job.integrate.service.ReportAlarmService;

/**
 * 
 * @author hebelala
 *
 */
@Service
public class ReportAlarmProxyServiceImpl implements ReportAlarmProxyService {

	@Resource
	private SystemConfigService systemConfigService;

	@Resource(type = ReportAlarmServiceImpl.class)
	private ReportAlarmService reportAlarmService;

	@Resource(type = ReportAlarmWithLoggerServiceImpl.class)
	private ReportAlarmService reportAlarmWithLoggerService;

	@Override
	public ReportAlarmService getTarget() {
		String type = systemConfigService.getValueDirectly(SystemConfigProperties.REPORT_ALARM_TYPE);
		if ("TODO".equalsIgnoreCase(type)) {
			return reportAlarmService;
		} else if ("LOGGER".equalsIgnoreCase(type)) {
			return reportAlarmWithLoggerService;
		} else {
			return reportAlarmWithLoggerService;
		}
	}

}
