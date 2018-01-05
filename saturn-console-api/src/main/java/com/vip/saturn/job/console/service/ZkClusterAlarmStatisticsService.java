package com.vip.saturn.job.console.service;

/**
 * 集群的告警统计
 *
 * @author hebelala
 */
public interface ZkClusterAlarmStatisticsService {

	String getAbnormalJobs(String zkList);

	String getUnableFailoverJobs(String zkList);

	String getTimeout4AlarmJobs(String zkList);

	String getAbnormalContainers(String zkList);

	boolean setAbnormalJobMonitorStatusToRead(String zkList, String uuid);

	boolean setTimeout4AlarmJobMonitorStatusToRead(String zkList, String uuid);

}
