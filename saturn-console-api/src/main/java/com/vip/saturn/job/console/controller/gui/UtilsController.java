package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.UtilsService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;

/**
 * @author hebelala
 */
@Controller
@RequestMapping("/console/utils")
public class UtilsController extends AbstractGUIController {

	@Resource
	private UtilsService utilsService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/checkAndForecastCron")
	public SuccessResponseEntity checkAndForecastCron(@RequestParam String timeZone, @RequestParam String cron)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(utilsService.checkAndForecastCron(timeZone, cron));
	}

}
