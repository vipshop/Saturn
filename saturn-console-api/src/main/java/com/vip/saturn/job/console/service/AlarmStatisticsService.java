package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author hebelala
 */
public interface AlarmStatisticsService {

	// 所有集群的告警统计

	String getAbnormalJobs() throws SaturnJobConsoleException;

	String getUnableFailoverJobs() throws SaturnJobConsoleException;

	String getTimeout4AlarmJobs() throws SaturnJobConsoleException;

	String getAbnormalContainers() throws SaturnJobConsoleException;

	void setAbnormalJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException;

	void setTimeout4AlarmJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException;

	// 集群的告警统计

	String getAbnormalJobs(String zkClusterKey) throws SaturnJobConsoleException;

	String getUnableFailoverJobs(String zkClusterKey) throws SaturnJobConsoleException;

	String getTimeout4AlarmJobs(String zkClusterKey) throws SaturnJobConsoleException;

	String getAbnormalContainers(String zkClusterKey) throws SaturnJobConsoleException;

	// 域的告警统计

	String getAbnormalJobsByNamespace(String namespace) throws SaturnJobConsoleException;

	String getUnableFailoverJobsByNamespace(String namespace) throws SaturnJobConsoleException;

	String getTimeout4AlarmJobsByNamespace(String namespace) throws SaturnJobConsoleException;

	String getAbnormalContainersByNamespace(String namespace) throws SaturnJobConsoleException;

	// 作业的告警统计

	boolean isAbnormalJob(String namespace, String jobName) throws SaturnJobConsoleException;

	boolean isUnableFailoverJob(String namespace, String jobName) throws SaturnJobConsoleException;

	boolean isTimeout4AlarmJob(String namespace, String jobName) throws SaturnJobConsoleException;
}
