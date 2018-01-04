package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 全域告警统计页面
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/alarm-statistics")
public class AlarmStatisticsController extends AbstractGUIController {

	@Resource
	private DashboardService dashboardService;

	@GetMapping(value = "/abnormalJobs")
	public SuccessResponseEntity getAbnormalJobs(final HttpServletRequest request,
			@RequestParam String zkClusterKey) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/unableFailoverJobs")
	public SuccessResponseEntity getUnableFailoverJobs(final HttpServletRequest request,
			@RequestParam String zkClusterKey) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/timeout4AlarmJobs")
	public SuccessResponseEntity getTimeout4AlarmJobs(final HttpServletRequest request,
			@RequestParam String zkClusterKey) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/abnormalContainers")
	public SuccessResponseEntity getAbnormalContainers(final HttpServletRequest request,
			@RequestParam String zkClusterKey) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/setAbnormalJobMonitorStatusToRead")
	public SuccessResponseEntity setAbnormalJobMonitorStatusToRead(final HttpServletRequest request,
			@RequestParam String zkClusterKey, @RequestParam String uuid) {
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/setTimeout4AlarmJobMonitorStatusToRead")
	public SuccessResponseEntity setTimeout4AlarmJobMonitorStatusToRead(final HttpServletRequest request,
			@RequestParam String zkClusterKey, @RequestParam String uuid) {
		return new SuccessResponseEntity();
	}

}
