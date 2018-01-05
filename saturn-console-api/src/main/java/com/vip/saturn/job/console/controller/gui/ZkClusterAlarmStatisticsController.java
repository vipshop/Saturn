package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.service.ZkClusterAlarmStatisticsService;
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
@RequestMapping("/console/{zkClusterKey:.*}/alarmStatistics")
public class ZkClusterAlarmStatisticsController extends AbstractGUIController {

	@Resource
	private ZkClusterAlarmStatisticsService zkClusterAlarmStatisticsService;

	private ZkCluster validateAndGetZKCluster(String zkClusterKey) throws SaturnJobConsoleGUIException {
		ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
		if (zkCluster == null) {
			throw new SaturnJobConsoleGUIException(String.format("该集群key（%s）不存在", zkClusterKey));
		}
		return zkCluster;
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalJobs")
	public SuccessResponseEntity getAbnormalJobs(@PathVariable String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		return new SuccessResponseEntity(zkClusterAlarmStatisticsService.getAbnormalJobs(zkCluster.getZkAddr()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/unableFailoverJobs")
	public SuccessResponseEntity getUnableFailoverJobs(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		return new SuccessResponseEntity(zkClusterAlarmStatisticsService.getUnableFailoverJobs(zkCluster.getZkAddr()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/timeout4AlarmJobs")
	public SuccessResponseEntity getTimeout4AlarmJobs(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		return new SuccessResponseEntity(zkClusterAlarmStatisticsService.getTimeout4AlarmJobs(zkCluster.getZkAddr()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalContainers")
	public SuccessResponseEntity getAbnormalContainers(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		return new SuccessResponseEntity(zkClusterAlarmStatisticsService.getAbnormalContainers(zkCluster.getZkAddr()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/setAbnormalJobMonitorStatusToRead")
	public SuccessResponseEntity setAbnormalJobMonitorStatusToRead(
			@AuditParam("zkClusterKey") @PathVariable String zkClusterKey,
			@AuditParam("uuid") @RequestParam String uuid) throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		zkClusterAlarmStatisticsService.setAbnormalJobMonitorStatusToRead(zkCluster.getZkAddr(), uuid);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/setTimeout4AlarmJobMonitorStatusToRead")
	public SuccessResponseEntity setTimeout4AlarmJobMonitorStatusToRead(
			@AuditParam("zkClusterKey") @PathVariable String zkClusterKey,
			@AuditParam("uuid") @RequestParam String uuid) throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		zkClusterAlarmStatisticsService.setTimeout4AlarmJobMonitorStatusToRead(zkCluster.getZkAddr(), uuid);
		return new SuccessResponseEntity();
	}

}
