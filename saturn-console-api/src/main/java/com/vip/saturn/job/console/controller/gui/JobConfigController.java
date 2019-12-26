package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.utils.PermissionKeys;
import com.vip.saturn.job.console.vo.UpdateJobConfigVo;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
@RequestMapping("/console/namespaces/{namespace:.+}/jobs/{jobName}/config")
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
			@AuditParam("jobName") @PathVariable String jobName, UpdateJobConfigVo updateJobConfigVo)
			throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobUpdate, namespace);
		jobService.updateJobConfig(namespace, updateJobConfigVo.toJobConfig(), getCurrentLoginUserName());
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping("/runAtOnce")
	public SuccessResponseEntity runAtOnce(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobRunAtOnce, namespace);
		jobService.runAtOnce(namespace, jobName);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping("/stopAtOnce")
	public SuccessResponseEntity stopAtOnce(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobStopAtOnce, namespace);
		jobService.stopAtOnce(namespace, jobName);
		return new SuccessResponseEntity();
	}

}
