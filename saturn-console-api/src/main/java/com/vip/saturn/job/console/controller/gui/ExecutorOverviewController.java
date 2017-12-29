package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.ServerDimensionService;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Executor overview page controller.
 *
 * @author kfchu
 */
@Controller
@RequestMapping("/console/executor-overview")
public class ExecutorOverviewController {

	@Resource
	private ExecutorService executorService;

	@RequestMapping(value = "/executors", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> getExecutors(final HttpServletRequest request,
			@RequestParam String namespace) {
		AuditInfoContext.putNamespace(namespace);
		return new ResponseEntity<>(new RequestResult(true, executorService.getExecutors(namespace)), HttpStatus.OK);
	}

	@RequestMapping(value = "/executor-allocation", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> getExecutorAllocation(final HttpServletRequest request,
			@RequestParam String namespace, @RequestParam String jobName) {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		return new ResponseEntity<>(new RequestResult(true, executorService.getExecutorAllocation(namespace, jobName)),
				HttpStatus.OK);
	}
}
