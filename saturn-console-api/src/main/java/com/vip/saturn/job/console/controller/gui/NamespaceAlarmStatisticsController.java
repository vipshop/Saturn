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

import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.AlarmStatisticsService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

/**
 * 域的告警统计
 *
 * @author hebelala
 */
@RequestMapping("/console/namespaces/{namespace:.+}/alarmStatistics")
public class NamespaceAlarmStatisticsController extends AbstractGUIController {

	@Resource
	private AlarmStatisticsService alarmStatisticsService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalJobs")
	public SuccessResponseEntity getAbnormalJobs(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getAbnormalJobsStringByNamespace(namespace));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/unableFailoverJobs")
	public SuccessResponseEntity getUnableFailoverJobs(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getUnableFailoverJobsStringByNamespace(namespace));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/timeout4AlarmJobs")
	public SuccessResponseEntity getTimeout4AlarmJobs(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getTimeout4AlarmJobsStringByNamespace(namespace));
	}

	/**
	 * 根据域名查询禁用时长超时的作业
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/disabledTimeoutJobs")
	public SuccessResponseEntity getDisabledTimeoutJobs(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getDisabledTimeoutJobsStringByNamespace(namespace));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/countOfAlarmJobs")
	public SuccessResponseEntity getCountOfAlarmJobs(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getCountOfAlarmJobsByNamespace(namespace));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalContainers")
	public SuccessResponseEntity getAbnormalContainers(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getAbnormalContainersByNamespace(namespace));
	}

}
