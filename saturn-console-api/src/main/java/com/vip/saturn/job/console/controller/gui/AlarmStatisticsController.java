package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

/**
 * 全域告警统计页面
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/{zkClusterKey}/alarmStatistics")
public class AlarmStatisticsController extends AbstractGUIController {

	@GetMapping(value = "/abnormalJobs")
	public SuccessResponseEntity getAbnormalJobs(final HttpServletRequest request,
			@PathVariable String zkClusterKey) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/unableFailoverJobs")
	public SuccessResponseEntity getUnableFailoverJobs(final HttpServletRequest request,
			@PathVariable String zkClusterKey) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/timeout4AlarmJobs")
	public SuccessResponseEntity getTimeout4AlarmJobs(final HttpServletRequest request,
			@PathVariable String zkClusterKey) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/abnormalContainers")
	public SuccessResponseEntity getAbnormalContainers(final HttpServletRequest request,
			@PathVariable String zkClusterKey) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/setAbnormalJobMonitorStatusToRead")
	public SuccessResponseEntity setAbnormalJobMonitorStatusToRead(final HttpServletRequest request,
			@AuditParam("zkClusterKey") @PathVariable String zkClusterKey,
			@AuditParam("uuid") @RequestParam String uuid) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/setTimeout4AlarmJobMonitorStatusToRead")
	public SuccessResponseEntity setTimeout4AlarmJobMonitorStatusToRead(final HttpServletRequest request,
			@AuditParam("zkClusterKey") @PathVariable String zkClusterKey,
			@AuditParam("uuid") @RequestParam String uuid) {
		return new SuccessResponseEntity();
	}

}
