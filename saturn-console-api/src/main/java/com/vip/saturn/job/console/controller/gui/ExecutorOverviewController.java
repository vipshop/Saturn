package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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

	@GetMapping(value = "/executors")
	public ResponseEntity<RequestResult> getExecutors(final HttpServletRequest request,
			@RequestParam String namespace) {
		AuditInfoContext.putNamespace(namespace);
		return new ResponseEntity<>(new RequestResult(true, executorService.getExecutors(namespace)), HttpStatus.OK);
	}

	@GetMapping(value = "/executor-allocation")
	public ResponseEntity<RequestResult> getExecutorAllocation(final HttpServletRequest request,
			@RequestParam String namespace, @RequestParam String executorName) {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.put("executorName", executorName);
		return new ResponseEntity<>(
				new RequestResult(true, executorService.getExecutorAllocation(namespace, executorName)),
				HttpStatus.OK);
	}
}
