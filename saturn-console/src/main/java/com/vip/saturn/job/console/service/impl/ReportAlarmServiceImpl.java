package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.integrate.exception.ReportAlarmException;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author hebelala
 */
@Service
public class ReportAlarmServiceImpl implements ReportAlarmService {

    @Override
    public void reportErrorAlarm(Map<String, String> alarmData) throws ReportAlarmException {
        // report error alarm
    }

    @Override
    public void reportWarningAlarm(Map<String, String> alarmData) throws ReportAlarmException {
        // report warning alarm
    }

}
