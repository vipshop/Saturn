package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import com.vip.saturn.job.console.vo.DependencyJob;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Job overview page controller.
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/job-overview")
public class JobOverviewController extends AbstractGUIController {

	@Resource
	private JobService jobService;

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/jobs", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> list(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		return new ResponseEntity<>(new RequestResult(true, jobService.jobs(namespace)), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/groups", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> groups(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		return new ResponseEntity<>(new RequestResult(true, jobService.groups(namespace)), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/dependent-jobs", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> dependentJob(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_name", required = true) String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		List<DependencyJob> dependencyJobs = jobService.dependentJobs(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, dependencyJobs), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/dependent-jobs-batch", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> dependentJobBatch(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_names", required = true) List<String> jobNames)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobNames(jobNames);
		Map<String, List<DependencyJob>> dependencyJobsMap = new HashMap<>();
		for (String jobName : jobNames) {
			List<DependencyJob> dependencyJobs = jobService.dependentJobs(namespace, jobName);
			dependencyJobsMap.put(jobName, dependencyJobs);
		}
		return new ResponseEntity<>(new RequestResult(true, dependencyJobsMap), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/depended-jobs", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> dependedJobs(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_name", required = true) String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		List<DependencyJob> dependedJobs = jobService.dependedJobs(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, dependedJobs), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/depended-jobs-batch", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> dependedJobsBatch(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_names", required = true) List<String> jobNames)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobNames(jobNames);
		Map<String, List<DependencyJob>> dependencyJobsMap = new HashMap<>();
		for (String jobName : jobNames) {
			List<DependencyJob> dependedJobs = jobService.dependedJobs(namespace, jobName);
			dependencyJobsMap.put(jobName, dependedJobs);
		}
		return new ResponseEntity<>(new RequestResult(true, dependencyJobsMap), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/enable-job", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> enableJob(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_name", required = true) String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		jobService.enableJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/enable-job-batch", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> enableJob(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_names", required = true) List<String> jobNames)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobNames(jobNames);
		for (String jobName : jobNames) {
			jobService.enableJob(namespace, jobName);
		}
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/disable-job", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> disableJob(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_name", required = true) String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		jobService.disableJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/disable-job-batch", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> disableJobBatch(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_names", required = true) List<String> jobNames)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobNames(jobNames);
		for (String jobName : jobNames) {
			jobService.disableJob(namespace, jobName);
		}
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

}
