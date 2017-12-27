package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.vo.DependencyJob;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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

	@RequestMapping(value = "/jobs", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> list(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace) throws SaturnJobConsoleException {
		return new ResponseEntity<>(new RequestResult(true, jobService.jobs(namespace)), HttpStatus.OK);
	}

	@RequestMapping(value = "/groups", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> groups(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace) throws SaturnJobConsoleException {
		return new ResponseEntity<>(new RequestResult(true, jobService.groups(namespace)), HttpStatus.OK);
	}

	@RequestMapping(value = "/dependent-jobs", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> dependentJob(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_name", required = true) String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependencyJobs = jobService.dependentJobs(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, dependencyJobs), HttpStatus.OK);
	}

	@RequestMapping(value = "/depended-jobs", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> dependedJobs(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_name", required = true) String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependedJobs = jobService.dependedJobs(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, dependedJobs), HttpStatus.OK);
	}

	@RequestMapping(value = "/enable-job", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> enableJob(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_name", required = true) String jobName) throws SaturnJobConsoleException {
		jobService.enableJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@RequestMapping(value = "/disable-job", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> disableJob(final HttpServletRequest request,
			@RequestParam(name = "namespace", required = true) String namespace,
			@RequestParam(name = "job_name", required = true) String jobName) throws SaturnJobConsoleException {
		jobService.disableJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

}
