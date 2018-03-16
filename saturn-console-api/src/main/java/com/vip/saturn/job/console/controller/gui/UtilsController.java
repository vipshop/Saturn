package com.vip.saturn.job.console.controller.gui;

import com.google.common.collect.Lists;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.UtilsService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collection;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;

/**
 * @author hebelala
 */
@RequestMapping("/console/utils")
public class UtilsController extends AbstractGUIController {

	@Resource
	private UtilsService utilsService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@PostMapping(value = "/checkAndForecastCron")
	public SuccessResponseEntity checkAndForecastCron(@RequestParam String timeZone, @RequestParam String cron)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(utilsService.checkAndForecastCron(timeZone, cron));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/timeZones")
	public SuccessResponseEntity getTimeZones() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(utilsService.getTimeZones());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusterKeys")
	public SuccessResponseEntity getZkClusterKeys() {
		Collection<ZkCluster> zkClusters = registryCenterService.getZkClusterList();
		List<String> zkClusterKeys = Lists.newArrayList();
		for (ZkCluster zkCluster : zkClusters) {
			zkClusterKeys.add(zkCluster.getZkClusterKey());
		}
		return new SuccessResponseEntity(zkClusterKeys);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/version")
	public SuccessResponseEntity getVersion() {
		return new SuccessResponseEntity(version);
	}

}
