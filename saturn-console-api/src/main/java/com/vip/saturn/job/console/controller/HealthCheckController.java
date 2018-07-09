package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/_health_check")
public class HealthCheckController extends AbstractController {

	@Resource
	private SystemConfigService systemConfigService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping
	public ResponseEntity<String> healthCheck(HttpServletRequest request) {
		systemConfigService.getValueDirectly(SystemConfigProperties.MAX_JOB_NUM);
		return new ResponseEntity<>("ok", HttpStatus.OK);
	}

}
