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

package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.AlarmStatisticsService;
import com.vip.saturn.job.console.utils.PermissionKeys;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;

/**
 * 所有集群的告警统计
 *
 * @author hebelala
 */
@RequestMapping("/console/alarmStatistics")
public class OverallAlarmStatisticsController extends AbstractGUIController {

	@Resource
	private AlarmStatisticsService alarmStatisticsService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalJobs")
	public SuccessResponseEntity getAbnormalJobs(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(alarmStatisticsService.getAbnormalJobsString());
		}
		return new SuccessResponseEntity(alarmStatisticsService.getAbnormalJobsStringByZKCluster(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/unableFailoverJobs")
	public SuccessResponseEntity getUnableFailoverJobs(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(alarmStatisticsService.getUnableFailoverJobsString());
		}
		return new SuccessResponseEntity(alarmStatisticsService.getUnableFailoverJobsStringByZKCluster(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/timeout4AlarmJobs")
	public SuccessResponseEntity getTimeout4AlarmJobs(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(alarmStatisticsService.getTimeout4AlarmJobsString());
		}
		return new SuccessResponseEntity(alarmStatisticsService.getTimeout4AlarmJobsStringByZKCluster(zkClusterKey));
	}

	/**
	 * 根据zk集群查询禁用时长超时的作业
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/disabledTimeoutJobs")
	public SuccessResponseEntity getDisabledTimeoutJobs(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(alarmStatisticsService.getDisabledTimeoutJobsString());
		}
		return new SuccessResponseEntity(alarmStatisticsService.getDisabledTimeoutJobsStringByZKCluster(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/countOfAlarmJobs")
	public SuccessResponseEntity getCountOfAlarmJobs() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getCountOfAlarmJobs());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalContainers")
	public SuccessResponseEntity getAbnormalContainers(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(alarmStatisticsService.getAbnormalContainers());
		}
		return new SuccessResponseEntity(alarmStatisticsService.getAbnormalContainers(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/setAbnormalJobMonitorStatusToRead")
	public SuccessResponseEntity setAbnormalJobMonitorStatusToRead(@AuditParam("uuid") @RequestParam String uuid)
			throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.alarmCenterSetAbnormalJobRead);
		alarmStatisticsService.setAbnormalJobMonitorStatusToRead(uuid);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/setTimeout4AlarmJobMonitorStatusToRead")
	public SuccessResponseEntity setTimeout4AlarmJobMonitorStatusToRead(@AuditParam("uuid") @RequestParam String uuid)
			throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.alarmCenterSetTimeout4AlarmJobRead);
		alarmStatisticsService.setTimeout4AlarmJobMonitorStatusToRead(uuid);
		return new SuccessResponseEntity();
	}

}
