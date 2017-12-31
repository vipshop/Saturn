package com.vip.saturn.job.console.controller.gui;

import com.google.common.collect.Lists;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ServerBriefInfo;
import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.service.ExecutorService;
import java.util.List;
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
public class ExecutorOverviewController extends AbstractGUIController {

	private static final String TRAFFIC_OPERATION_EXTRACT = "extract";

	private static final String TRAFFIC_OPERATION_RECOVER = "recover";

	@Resource
	private ExecutorService executorService;

	/**
	 * 获取域下所有executor基本信息
	 */
	@GetMapping(value = "/executors")
	public SuccessResponseEntity getExecutors(final HttpServletRequest request,
			@RequestParam String namespace) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(executorService.getExecutors(namespace));
	}

	/**
	 * 获取executor被分配的作业分片信息
	 */
	@GetMapping(value = "/executor-allocation")
	public SuccessResponseEntity getExecutorAllocation(final HttpServletRequest request,
			@RequestParam String namespace, @RequestParam String executorName) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(executorService.getExecutorAllocation(namespace, executorName));
	}

	/**
	 * 一键重排
	 */
	@Audit
	@PostMapping(value = "/shard-all")
	public SuccessResponseEntity shardAll(final HttpServletRequest request,
			@AuditParam("namespace") @RequestParam String namespace)
			throws SaturnJobConsoleException {
		executorService.shardAll(namespace);
		return new SuccessResponseEntity();
	}

	/*
	 *	摘流量与流量恢复，其中executor必须online
	 */
	@Audit
	@PostMapping(value = "/traffic")
	public SuccessResponseEntity extractOrRecoverTraffic(final HttpServletRequest request,
			@AuditParam("namespace") @RequestParam String namespace,
			@AuditParam("executorName") @RequestParam String executorName,
			@AuditParam("operation") @RequestParam String operation)
			throws SaturnJobConsoleException {
		// check executor is existed and online.
		checkExecutorStatus(namespace, executorName, ServerStatus.ONLINE, "Executor不在线，不能摘取流量");
		extractOrRecoverTraffic(namespace, executorName, operation);

		return new SuccessResponseEntity();
	}

	/*
	 *	批量摘流量与流量恢复，其中executor必须online
	 */
	@Audit
	@PostMapping(value = "/traffic-batch")
	public SuccessResponseEntity batchExtractOrRecoverTraffic(final HttpServletRequest request,
			@AuditParam("namespace") @RequestParam String namespace,
			@AuditParam("executorNames") @RequestParam List<String> executorNames,
			@AuditParam("operation") @RequestParam String operation)
			throws SaturnJobConsoleException {
		// check executor is existed and online.
		List<String> success2ExtractOrRecoverTrafficExecutors = Lists.newArrayList();
		List<String> fail2ExtractOrRecoverTrafficExecutors = Lists.newArrayList();
		for (String executorName : executorNames) {
			try {
				checkExecutorStatus(namespace, executorName, ServerStatus.ONLINE, "Executor不在线，不能摘取流量");
				extractOrRecoverTraffic(namespace, executorName, operation);
				success2ExtractOrRecoverTrafficExecutors.add(executorName);
			} catch (Exception e) {
				fail2ExtractOrRecoverTrafficExecutors.add(executorName);
			}
		}

		if (!fail2ExtractOrRecoverTrafficExecutors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("操作成功的executor:" + success2ExtractOrRecoverTrafficExecutors.toString()).append("，")
					.append("操作失败的executor:")
					.append(fail2ExtractOrRecoverTrafficExecutors.toString());
			throw new SaturnJobConsoleGUIException(message.toString());
		}

		return new SuccessResponseEntity();
	}

	private void extractOrRecoverTraffic(String namespace, String executorName, String operation)
			throws SaturnJobConsoleException {
		if (TRAFFIC_OPERATION_EXTRACT.equals(operation)) {
			executorService.trafficExtraction(namespace, executorName);
		} else if (TRAFFIC_OPERATION_RECOVER.equals(operation)) {
			executorService.trafficRecovery(namespace, executorName);
		} else {
			throw new SaturnJobConsoleGUIException("operation " + operation + "不支持");
		}
	}

	/**
	 * 移除executor
	 */
	@Audit
	@PostMapping(value = "/remove-executor")
	public SuccessResponseEntity removeExecutor(final HttpServletRequest request,
			@AuditParam("namespace") @RequestParam String namespace,
			@AuditParam("executorName") @RequestParam String executorName)
			throws SaturnJobConsoleException {
		// check executor is existed and online.
		checkExecutorStatus(namespace, executorName, ServerStatus.OFFLINE, "Executor在线，不能移除");
		executorService.removeExecutor(namespace, executorName);
		return new SuccessResponseEntity();
	}

	/**
	 * 批量移除executor
	 */
	@Audit
	@PostMapping(value = "/remove-executor-batch")
	public SuccessResponseEntity batchRemoveExecutors(final HttpServletRequest request,
			@AuditParam("namespace") @RequestParam String namespace,
			@AuditParam("executorNames") @RequestParam List<String> executorNames)
			throws SaturnJobConsoleException {
		// check executor is existed and online.
		List<String> success2RemoveExecutors = Lists.newArrayList();
		List<String> fail2RemoveExecutors = Lists.newArrayList();
		for (String executorName : executorNames) {
			try {
				checkExecutorStatus(namespace, executorName, ServerStatus.OFFLINE, "Executor在线，不能移除");
				executorService.removeExecutor(namespace, executorName);
				success2RemoveExecutors.add(executorName);
			} catch (Exception e) {
				fail2RemoveExecutors.add(executorName);
			}
		}
		if (!fail2RemoveExecutors.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("删除成功的executor:" + success2RemoveExecutors.toString()).append("，").append("删除失败的executor:")
					.append(fail2RemoveExecutors.toString());
			throw new SaturnJobConsoleGUIException(message.toString());
		}

		return new SuccessResponseEntity();
	}


	private void checkExecutorStatus(String namespace, String executorName, ServerStatus status, String errMsg)
			throws SaturnJobConsoleException {
		ServerBriefInfo executorInfo = executorService.getExecutor(namespace, executorName);
		if (executorInfo == null) {
			throw new SaturnJobConsoleGUIException("Executor不存在");
		}
		if (status != executorInfo.getStatus()) {
			throw new SaturnJobConsoleGUIException(errMsg);
		}
	}
}
