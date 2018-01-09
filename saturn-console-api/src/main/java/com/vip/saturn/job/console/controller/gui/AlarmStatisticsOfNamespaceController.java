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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

/**
 * 域的告警统计
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/namespaces/{namespace:.+}/alarmStatistics")
public class AlarmStatisticsOfNamespaceController extends AbstractGUIController {

	@Resource
	private AlarmStatisticsService alarmStatisticsService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalJobs")
	public SuccessResponseEntity getAbnormalJobs(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getAbnormalJobsByNamespace(namespace));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/unableFailoverJobs")
	public SuccessResponseEntity getUnableFailoverJobs(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getUnableFailoverJobsByNamespace(namespace));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/timeout4AlarmJobs")
	public SuccessResponseEntity getTimeout4AlarmJobs(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getTimeout4AlarmJobsByNamespace(namespace));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/abnormalContainers")
	public SuccessResponseEntity getAbnormalContainers(@PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(alarmStatisticsService.getAbnormalContainersByNamespace(namespace));
	}

}
