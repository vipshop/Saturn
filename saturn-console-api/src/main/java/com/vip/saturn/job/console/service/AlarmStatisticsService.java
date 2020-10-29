/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.*;
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

	String getDisabledTimeoutJobsString() throws SaturnJobConsoleException;

	List<AlarmJobCount> getCountOfAlarmJobs() throws SaturnJobConsoleException;

	String getAbnormalContainers() throws SaturnJobConsoleException;

	void setAbnormalJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException;

	void setTimeout4AlarmJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException;

	void setDisabledTimeoutJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException;

	// 集群的告警统计

	String getAbnormalJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException;

	String getUnableFailoverJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException;

	String getTimeout4AlarmJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException;

	String getDisabledTimeoutJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException;

	String getAbnormalContainers(String zkClusterKey) throws SaturnJobConsoleException;

	// 域的告警统计

	String getAbnormalJobsStringByNamespace(String namespace) throws SaturnJobConsoleException;

	String getUnableFailoverJobsStringByNamespace(String namespace) throws SaturnJobConsoleException;

	String getTimeout4AlarmJobsStringByNamespace(String namespace) throws SaturnJobConsoleException;

	String getDisabledTimeoutJobsStringByNamespace(String namespace) throws SaturnJobConsoleException;

	List<AlarmJobCount> getCountOfAlarmJobsByNamespace(String namespace) throws SaturnJobConsoleException;

	List<AbnormalJob> getAbnormalJobListByNamespace(String namespace) throws SaturnJobConsoleException;

	List<DisabledTimeoutAlarmJob> getDisabledTimeoutJobListByNamespace(String namespace) throws SaturnJobConsoleException;

	String getAbnormalContainersByNamespace(String namespace) throws SaturnJobConsoleException;

	// 作业的告警统计

	AbnormalJob isAbnormalJob(String namespace, String jobName) throws SaturnJobConsoleException;

	AbnormalJob isUnableFailoverJob(String namespace, String jobName) throws SaturnJobConsoleException;

	Timeout4AlarmJob isTimeout4AlarmJob(String namespace, String jobName) throws SaturnJobConsoleException;

	/**
	 * 是否禁用作业超过设置时长
	 */
	DisabledTimeoutAlarmJob isDisabledTimeout(String namespace, String jobName) throws SaturnJobConsoleException;
}
