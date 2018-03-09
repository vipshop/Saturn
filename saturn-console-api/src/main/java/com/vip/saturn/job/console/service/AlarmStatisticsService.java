package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.AbnormalJob;
import com.vip.saturn.job.console.domain.AlarmJobCount;
import com.vip.saturn.job.console.domain.Timeout4AlarmJob;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.util.List;

/**
 * @author hebelala
 */
public interface AlarmStatisticsService {

	// 所有集群的告警统计

	String getAbnormalJobsString() throws SaturnJobConsoleException;

	String getUnableFailoverJobsString() throws SaturnJobConsoleException;

	String getTimeout4AlarmJobsString() throws SaturnJobConsoleException;

	List<AlarmJobCount> getCountOfAlarmJobs() throws SaturnJobConsoleException;

	String getAbnormalContainers() throws SaturnJobConsoleException;

	void setAbnormalJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException;

	void setTimeout4AlarmJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException;

	// 集群的告警统计

	String getAbnormalJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException;

	String getUnableFailoverJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException;

	String getTimeout4AlarmJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException;

	String getAbnormalContainers(String zkClusterKey) throws SaturnJobConsoleException;

	// 域的告警统计

	String getAbnormalJobsStringByNamespace(String namespace) throws SaturnJobConsoleException;

	String getUnableFailoverJobsStringByNamespace(String namespace) throws SaturnJobConsoleException;

	String getTimeout4AlarmJobsStringByNamespace(String namespace) throws SaturnJobConsoleException;

	List<AlarmJobCount> getCountOfAlarmJobsByNamespace(String namespace) throws SaturnJobConsoleException;

	List<AbnormalJob> getAbnormalJobListByNamespace(String namespace) throws SaturnJobConsoleException;

	String getAbnormalContainersByNamespace(String namespace) throws SaturnJobConsoleException;

	// 作业的告警统计

	AbnormalJob isAbnormalJob(String namespace, String jobName) throws SaturnJobConsoleException;

	AbnormalJob isUnableFailoverJob(String namespace, String jobName) throws SaturnJobConsoleException;

	Timeout4AlarmJob isTimeout4AlarmJob(String namespace, String jobName) throws SaturnJobConsoleException;
}
