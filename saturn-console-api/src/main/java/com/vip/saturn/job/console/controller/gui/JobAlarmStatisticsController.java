package com.vip.saturn.job.console.controller.gui;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.AlarmStatisticsService;
import com.vip.saturn.job.console.utils.PermissionKeys;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * 作业的告警统计
 *
 * @author hebelala
 */
@RequestMapping("/console/namespaces/{namespace:.+}")
public class JobAlarmStatisticsController extends AbstractGUIController {

	@Resource
	private AlarmStatisticsService alarmStatisticsService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/jobs/{jobName}/isAbnormal")
	public SuccessResponseEntity isAbnormalJob(@PathVariable String namespace, @PathVariable String jobName)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.isAbnormalJob(namespace, jobName));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/jobs/{jobName}/isUnableFailover")
	public SuccessResponseEntity isUnableFailoverJob(@PathVariable String namespace, @PathVariable String jobName)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.isUnableFailoverJob(namespace, jobName));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/jobs/{jobName}/isTimeout4Alarm")
	public SuccessResponseEntity isTimeout4AlarmJob(@PathVariable String namespace, @PathVariable String jobName)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.isTimeout4AlarmJob(namespace, jobName));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/setAbnormalJobMonitorStatusToRead")
	public SuccessResponseEntity setAbnormalJobMonitorStatusToRead(@AuditParam("uuid") @RequestParam String uuid,
			@AuditParam("namespace") @PathVariable String namespace) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.alarmCenterSetAbnormalJobRead, namespace);
		alarmStatisticsService.setAbnormalJobMonitorStatusToRead(uuid);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/setTimeout4AlarmJobMonitorStatusToRead")
	public SuccessResponseEntity setTimeout4AlarmJobMonitorStatusToRead(@AuditParam("uuid") @RequestParam String uuid,
			@AuditParam("namespace") @PathVariable String namespace) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.alarmCenterSetTimeout4AlarmJobRead, namespace);
		alarmStatisticsService.setTimeout4AlarmJobMonitorStatusToRead(uuid);
		return new SuccessResponseEntity();
	}

}
