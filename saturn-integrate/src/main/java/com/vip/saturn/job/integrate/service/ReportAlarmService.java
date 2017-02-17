package com.vip.saturn.job.integrate.service;

import com.vip.saturn.job.integrate.exception.ReportAlarmException;

import java.util.Map;

/**
 * @author hebelala
 */
public interface ReportAlarmService {

    void reportErrorAlarm(Map<String, String> alarmData) throws ReportAlarmException;

    void reportWarningAlarm(Map<String, String> alarmData) throws ReportAlarmException;

}
