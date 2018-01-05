package com.vip.saturn.job.console.service;

/**
 * 所有集群的告警统计
 *
 * @author hebelala
 */
public interface AlarmStatisticsService {

	String getAbnormalJobs();

	String getUnableFailoverJobs();

	String getTimeout4AlarmJobs();

	String getAbnormalContainers();

	boolean setAbnormalJobMonitorStatusToRead(String uuid);

	boolean setTimeout4AlarmJobMonitorStatusToRead(String uuid);
}
