package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.integrate.entity.AlarmInfo;
import com.vip.saturn.job.integrate.exception.ReportAlarmException;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author hebelala
 */
public class ReportAlarmServiceImpl implements ReportAlarmService {

	private static final Logger log = LoggerFactory.getLogger(ReportAlarmServiceImpl.class);

	@Override
	public void allShardingError(String namespace, String hostValue) throws ReportAlarmException {
		log.error("allShardingError, namespace is {}, hostValue is {}", namespace, hostValue);
	}

	@Override
	public void dashboardContainerInstancesMismatch(String namespace, String taskId, int configInstances,
			int runningInstances) throws ReportAlarmException {
		log.error(
				"dashboardContainerInstancesMismatch, namespace is {}, taskId is {}, configInstances is {}, runningInstances is {}",
				namespace, taskId, configInstances, runningInstances);
	}

	@Override
	public void dashboardAbnormalJob(String namespace, String jobName, String timeZone, long shouldFiredTime)
			throws ReportAlarmException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		format.setTimeZone(TimeZone.getTimeZone(timeZone));
		String shouldFiredTimeFormatted = timeZone + " " + format.format(shouldFiredTime);
		log.error("dashboardAbnormalJob, namespace is {}, jobName is {}, timeZone is {}, shouldFiredTime is {}",
				namespace, jobName, timeZone, shouldFiredTimeFormatted);
	}

	@Override
	public void dashboardAbnormalBatchJobs(String namespace, List<Map<String, String>> jobList)
			throws ReportAlarmException {
		//do nothing
	}

	@Override
	public void dashboardTimeout4AlarmJob(String namespace, String jobName, List<Integer> timeoutItems,
			int timeout4AlarmSeconds) throws ReportAlarmException {
		log.error(
				"dashboardTimeout4AlarmJob, namespace is {}, jobName is {}, timeoutItems is {}, timeout4AlarmSeconds is {}",
				namespace, jobName, timeoutItems, timeout4AlarmSeconds);
	}

	@Override
	public void executorRestart(String namespace, String executorName, String restartTime) throws ReportAlarmException {
		log.error("executor restart, namespace is {}, executor is {}, restart on {}", namespace, executorName,
				restartTime);
	}

	@Override
	public void raise(String namespace, String jobName, String executorName, Integer shardItem, AlarmInfo alarmInfo)
			throws ReportAlarmException {
		log.error("raise, namespace is {}, jobName is {}, executorName is {}, shardItem is {}, alarmInfo is {}",
				namespace, jobName, executorName, shardItem, alarmInfo);
	}

}
