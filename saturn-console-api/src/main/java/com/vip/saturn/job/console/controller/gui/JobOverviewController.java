package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
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
import java.util.ArrayList;
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

	@RequestMapping(value = "/jobs", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> getJobs(final HttpServletRequest request, @RequestParam String namespace)
			throws SaturnJobConsoleException {
		return new ResponseEntity<>(new RequestResult(true, jobService.jobs(namespace)), HttpStatus.OK);
	}

	@RequestMapping(value = "/groups", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> getGroups(final HttpServletRequest request, @RequestParam String namespace)
			throws SaturnJobConsoleException {
		return new ResponseEntity<>(new RequestResult(true, jobService.groups(namespace)), HttpStatus.OK);
	}

	/**
	 * Get the jobs. The job is depending on the jobs.
	 */
	@RequestMapping(value = "/depending-jobs", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> getDependingJobs(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependencyJobs = jobService.dependingJobs(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, dependencyJobs), HttpStatus.OK);
	}

	@RequestMapping(value = "/depending-jobs-batch", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> batchGetDependingJob(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
		Map<String, List<DependencyJob>> dependencyJobsMap = new HashMap<>();
		for (String jobName : jobNames) {
			List<DependencyJob> dependencyJobs = jobService.dependingJobs(namespace, jobName);
			dependencyJobsMap.put(jobName, dependencyJobs);
		}
		return new ResponseEntity<>(new RequestResult(true, dependencyJobsMap), HttpStatus.OK);
	}

	/**
	 * Get the jobs. The job is depended by the jobs.
	 */
	@RequestMapping(value = "/depended-jobs", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> getDependedJobs(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependedJobs = jobService.dependedJobs(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, dependedJobs), HttpStatus.OK);
	}

	@RequestMapping(value = "/depended-jobs-batch", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> batchGetDependedJobs(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
		Map<String, List<DependencyJob>> dependencyJobsMap = new HashMap<>();
		for (String jobName : jobNames) {
			List<DependencyJob> dependedJobs = jobService.dependedJobs(namespace, jobName);
			dependencyJobsMap.put(jobName, dependedJobs);
		}
		return new ResponseEntity<>(new RequestResult(true, dependencyJobsMap), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/enable-job", method = RequestMethod.POST)
	public ResponseEntity<RequestResult> enableJob(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		jobService.enableJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/enable-job-batch", method = RequestMethod.POST)
	public ResponseEntity<RequestResult> batchEnableJob(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobNames(jobNames);
		for (String jobName : jobNames) {
			jobService.enableJob(namespace, jobName);
		}
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/disable-job", method = RequestMethod.POST)
	public ResponseEntity<RequestResult> disableJob(final HttpServletRequest request, @RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		jobService.disableJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/disable-job-batch", method = RequestMethod.POST)
	public ResponseEntity<RequestResult> batchDisableJob(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobNames(jobNames);
		for (String jobName : jobNames) {
			jobService.disableJob(namespace, jobName);
		}
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/remove-job", method = RequestMethod.POST)
	public ResponseEntity<RequestResult> removeJob(final HttpServletRequest request, @RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		jobService.removeJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@RequestMapping(value = "/remove-job-batch", method = RequestMethod.POST)
	public ResponseEntity<RequestResult> batchRemoveJob(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam List<String> jobNames) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobNames(jobNames);
		List<String> successJobNames = new ArrayList<>();
		List<String> failJobNames = new ArrayList<>();
		for (String jobName : jobNames) {
			try {
				jobService.removeJob(namespace, jobName);
				successJobNames.add(jobName);
			} catch (Exception e) {
				failJobNames.add(jobName);
			}
		}
		if (!failJobNames.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("删除成功的作业:" + successJobNames.toString()).append("，").append("删除失败的作业:")
					.append(failJobNames.toString());
			throw new SaturnJobConsoleGUIException(message.toString());
		}
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

}
