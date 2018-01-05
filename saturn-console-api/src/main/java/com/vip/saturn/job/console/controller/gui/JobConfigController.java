package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Job config related operations.
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/{namespace:.+}/jobs/{jobName}/config")
public class JobConfigController extends AbstractGUIController {

	@Resource
	private JobService jobService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping
	public SuccessResponseEntity getJobConfig(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(jobService.getJobConfigVo(namespace, jobName));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping
	public SuccessResponseEntity updateJobConfig(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName, JobConfig jobConfig)
			throws SaturnJobConsoleException {
		jobService.updateJobConfig(namespace, jobConfig);
		return new SuccessResponseEntity();
	}

}
