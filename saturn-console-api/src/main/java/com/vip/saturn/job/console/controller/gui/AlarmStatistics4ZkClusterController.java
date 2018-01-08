package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
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
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;

/**
 * 集群的告警统计
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/zkClusters/{zkClusterKey:.+}/alarmStatistics")
public class AlarmStatistics4ZkClusterController extends AbstractGUIController {

	@Resource
	private AlarmStatisticsService alarmStatisticsService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalJobs")
	public SuccessResponseEntity getAbnormalJobs(@PathVariable String zkClusterKey) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getAbnormalJobs(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/unableFailoverJobs")
	public SuccessResponseEntity getUnableFailoverJobs(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getUnableFailoverJobs(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/timeout4AlarmJobs")
	public SuccessResponseEntity getTimeout4AlarmJobs(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getTimeout4AlarmJobs(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalContainers")
	public SuccessResponseEntity getAbnormalContainers(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getAbnormalContainers(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@GetMapping(value = "/setAbnormalJobMonitorStatusToRead")
	public SuccessResponseEntity setAbnormalJobMonitorStatusToRead(
			@AuditParam("zkClusterKey") @PathVariable String zkClusterKey,
			@AuditParam("uuid") @RequestParam String uuid) throws SaturnJobConsoleException {
		alarmStatisticsService.setAbnormalJobMonitorStatusToRead(zkClusterKey, uuid);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@GetMapping(value = "/setTimeout4AlarmJobMonitorStatusToRead")
	public SuccessResponseEntity setTimeout4AlarmJobMonitorStatusToRead(
			@AuditParam("zkClusterKey") @PathVariable String zkClusterKey,
			@AuditParam("uuid") @RequestParam String uuid) throws SaturnJobConsoleException {
		alarmStatisticsService.setTimeout4AlarmJobMonitorStatusToRead(zkClusterKey, uuid);
		return new SuccessResponseEntity();
	}

}
