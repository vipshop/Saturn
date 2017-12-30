package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ServerBriefInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Executor overview page controller.
 *
 * @author kfchu
 */
@Controller
@RequestMapping("/console/executor-overview")
public class ExecutorOverviewController {

	private static final String TRAFFIC_OPERATION_EXTRACT = "extract";

	private static final String TRAFFIC_OPERATION_RECOVER = "recover";

	@Resource
	private ExecutorService executorService;

	@GetMapping(value = "/executors")
	public ResponseEntity<RequestResult> getExecutors(final HttpServletRequest request,
			@RequestParam String namespace) throws SaturnJobConsoleException {
		return new ResponseEntity<>(new RequestResult(true, executorService.getExecutors(namespace)), HttpStatus.OK);
	}

	@GetMapping(value = "/executor-allocation")
	public ResponseEntity<RequestResult> getExecutorAllocation(final HttpServletRequest request,
			@RequestParam String namespace, @RequestParam String executorName) throws SaturnJobConsoleException {
		return new ResponseEntity<>(
				new RequestResult(true, executorService.getExecutorAllocation(namespace, executorName)),
				HttpStatus.OK);
	}

	/*
	 *	摘流量与流量恢复。executor必须online。
	 */
	@Audit
	@PostMapping(value = "/traffic")
	public ResponseEntity<RequestResult> extractOrRecoverTraffic(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam String executorName, @RequestParam String operation) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putExecutorName(executorName);

		// check executor is existed and online.
		ServerBriefInfo executorInfo = executorService.getExecutor(namespace, executorName);
		if (executorInfo == null) {
			throw new SaturnJobConsoleGUIException("Executor不存在");
		}
		if (executorInfo.getServerIp() == null) {
			throw new SaturnJobConsoleGUIException("Executor不在线，不能摘取流量");
		}

		if (TRAFFIC_OPERATION_EXTRACT.equals(operation)) {
			executorService.trafficExtraction(namespace, executorName);
		} else if (TRAFFIC_OPERATION_RECOVER.equals(operation)) {
			executorService.trafficRecovery(namespace, executorName);
		} else {
			throw new SaturnJobConsoleException("operation " + operation + "不支持");
		}

		return new ResponseEntity<>(new RequestResult(true), HttpStatus.OK);
	}

	/**
	 * 一键重排
	 */
	@Audit
	@PostMapping(value = "/shard-all")
	public ResponseEntity<RequestResult> shardAllJobs(final HttpServletRequest request, @RequestParam String namespace)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		executorService.shardAll(namespace);
		return new ResponseEntity<>(new RequestResult(true), HttpStatus.OK);
	}


}
