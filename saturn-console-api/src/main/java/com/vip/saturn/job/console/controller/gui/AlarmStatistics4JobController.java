package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.AlarmStatisticsService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

/**
 * 作业的告警统计
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/alarmStatistics/job/{namespace:.+}/{jobName}")
public class AlarmStatistics4JobController extends AbstractGUIController {

	@Resource
	private AlarmStatisticsService alarmStatisticsService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/isAbnormalJob")
	public SuccessResponseEntity isAbnormalJob(@PathVariable String namespace, @PathVariable String jobName)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.isAbnormalJob(namespace, jobName));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/isUnableFailoverJob")
	public SuccessResponseEntity isUnableFailoverJob(@PathVariable String namespace, @PathVariable String jobName)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.isUnableFailoverJob(namespace, jobName));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/isTimeout4AlarmJob")
	public SuccessResponseEntity isTimeout4AlarmJob(@PathVariable String namespace, @PathVariable String jobName)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.isTimeout4AlarmJob(namespace, jobName));
	}

}
