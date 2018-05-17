package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.AlarmStatisticsService;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.utils.*;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.vip.saturn.job.console.exception.SaturnJobConsoleException.ERROR_CODE_BAD_REQUEST;

/**
 * Job overview related operations.
 *
 * @author hebelala
 */
@RequestMapping("/console/namespaces/{namespace:.+}/jobs")
public class JobOverviewController extends AbstractGUIController {

	private static final Logger log = LoggerFactory.getLogger(JobOverviewController.class);

	@Resource
	private JobService jobService;

	@Resource
	private AlarmStatisticsService alarmStatisticsService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping
	public SuccessResponseEntity getJobs(final HttpServletRequest request, @PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(getJobOverviewVo(namespace));
	}

	public JobOverviewVo getJobOverviewVo(String namespace) throws SaturnJobConsoleException {
		JobOverviewVo jobOverviewVo = new JobOverviewVo();
		try {
			List<JobOverviewJobVo> jobList = new ArrayList<>();
			int enabledNumber = 0;
			List<JobConfig> unSystemJobs = jobService.getUnSystemJobs(namespace);
			if (unSystemJobs != null) {
				enabledNumber = updateJobOverviewDetail(namespace, jobList, enabledNumber, unSystemJobs);
			}
			jobOverviewVo.setJobs(jobList);
			jobOverviewVo.setEnabledNumber(enabledNumber);
			jobOverviewVo.setTotalNumber(jobList.size());

			// 获取该域下的异常作业数量，捕获所有异常，打日志，不抛到前台
			updateAbnormalJobSizeInOverview(namespace, jobOverviewVo);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}

		return jobOverviewVo;
	}

	private void updateAbnormalJobSizeInOverview(String namespace, JobOverviewVo jobOverviewVo) {
		try {
			List<AbnormalJob> abnormalJobList = alarmStatisticsService.getAbnormalJobListByNamespace(namespace);
			jobOverviewVo.setAbnormalNumber(abnormalJobList.size());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private int updateJobOverviewDetail(String namespace, List<JobOverviewJobVo> jobList, int enabledNumber,
			List<JobConfig> unSystemJobs) {
		for (JobConfig jobConfig : unSystemJobs) {
			try {
				jobConfig.setDefaultValues();

				JobOverviewJobVo jobOverviewJobVo = new JobOverviewJobVo();
				SaturnBeanUtils.copyProperties(jobConfig, jobOverviewJobVo);

				updateJobTypesInOverview(jobConfig, jobOverviewJobVo);

				if (StringUtils.isBlank(jobOverviewJobVo.getGroups())) {
					jobOverviewJobVo.setGroups(SaturnConstants.NO_GROUPS_LABEL);
				}

				JobStatus jobStatus = jobService.getJobStatus(namespace, jobConfig.getJobName());
				jobOverviewJobVo.setStatus(jobStatus);

				if (!JobStatus.STOPPED.equals(jobStatus)) {// 作业如果是STOPPED状态，不需要显示已分配的executor
					updateShardingListInOverview(namespace, jobConfig, jobOverviewJobVo);
				}

				if (jobOverviewJobVo.getEnabled()) {
					enabledNumber++;
				}
				jobList.add(jobOverviewJobVo);
			} catch (Exception e) {
				log.error("list job " + jobConfig.getJobName() + " error", e);
			}
		}
		return enabledNumber;
	}

	private void updateJobTypesInOverview(JobConfig jobConfig, JobOverviewJobVo jobOverviewJobVo) {
		JobType jobType = JobType.getJobType(jobConfig.getJobType());
		if (JobType.UNKOWN_JOB.equals(jobType)) {
			if (jobOverviewJobVo.getJobClass() != null
					&& jobOverviewJobVo.getJobClass().indexOf("SaturnScriptJob") != -1) {
				jobOverviewJobVo.setJobType(JobType.SHELL_JOB.name());
			} else {
				jobOverviewJobVo.setJobType(JobType.JAVA_JOB.name());
			}
		}
	}

	private void updateShardingListInOverview(String namespace, JobConfig jobConfig, JobOverviewJobVo jobOverviewJobVo)
			throws SaturnJobConsoleException {
		List<String> jobShardingAllocatedExecutorList = jobService
				.getJobShardingAllocatedExecutorList(namespace, jobConfig.getJobName());

		StringBuilder shardingListSb = new StringBuilder();
		for (String executor : jobShardingAllocatedExecutorList) {
			shardingListSb.append(executor).append(",");
		}
		if (shardingListSb.length() > 0) {
			jobOverviewJobVo.setShardingList(shardingListSb.substring(0, shardingListSb.length() - 1));
		}
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/groups")
	public SuccessResponseEntity getGroups(final HttpServletRequest request, @PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(jobService.getGroups(namespace));
	}

	/**
	 * 获取该作业依赖的所有作业
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/{jobName}/dependency")
	public SuccessResponseEntity getDependingJobs(final HttpServletRequest request, @PathVariable String namespace,
			@PathVariable String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependencyJobs = jobService.getDependingJobs(namespace, jobName);
		return new SuccessResponseEntity(dependencyJobs);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/dependency")
	public SuccessResponseEntity batchGetDependingJob(final HttpServletRequest request, @PathVariable String namespace,
			@RequestParam List<String> jobNames) throws SaturnJobConsoleException {
		Map<String, List<DependencyJob>> dependencyJobsMap = new HashMap<>();
		for (String jobName : jobNames) {
			List<DependencyJob> dependencyJobs = jobService.getDependingJobs(namespace, jobName);
			dependencyJobsMap.put(jobName, dependencyJobs);
		}
		return new SuccessResponseEntity(dependencyJobsMap);
	}

	/**
	 * 获取依赖该作业的所有作业
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/{jobName}/beDependedJobs")
	public SuccessResponseEntity getDependedJobs(final HttpServletRequest request, @PathVariable String namespace,
			@PathVariable String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependedJobs = jobService.getDependedJobs(namespace, jobName);
		return new SuccessResponseEntity(dependedJobs);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/beDependedJobs")
	public SuccessResponseEntity batchGetDependedJobs(final HttpServletRequest request, @PathVariable String namespace,
			@RequestParam List<String> jobNames) throws SaturnJobConsoleException {
		Map<String, List<DependencyJob>> dependencyJobsMap = new HashMap<>();
		for (String jobName : jobNames) {
			List<DependencyJob> dependedJobs = jobService.getDependedJobs(namespace, jobName);
			dependencyJobsMap.put(jobName, dependedJobs);
		}
		return new SuccessResponseEntity(dependencyJobsMap);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/{jobName}/enable")
	public SuccessResponseEntity enableJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobEnable, namespace);
		jobService.enableJob(namespace, jobName, getCurrentLoginUserName());
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/enable")
	public SuccessResponseEntity batchEnableJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobNames") @RequestParam List<String> jobNames) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobBatchEnable, namespace);
		String userName = getCurrentLoginUserName();
		for (String jobName : jobNames) {
			jobService.enableJob(namespace, jobName, userName);
		}
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/{jobName}/disable")
	public SuccessResponseEntity disableJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobDisable, namespace);
		jobService.disableJob(namespace, jobName, getCurrentLoginUserName());
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/disable")
	public SuccessResponseEntity batchDisableJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobNames") @RequestParam List<String> jobNames) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobBatchDisable, namespace);
		String userName = getCurrentLoginUserName();
		for (String jobName : jobNames) {
			jobService.disableJob(namespace, jobName, userName);
		}
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@DeleteMapping(value = "/{jobName}")
	public SuccessResponseEntity removeJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobRemove, namespace);
		jobService.removeJob(namespace, jobName);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@DeleteMapping
	public SuccessResponseEntity batchRemoveJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobNames") @RequestParam List<String> jobNames) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobBatchRemove, namespace);
		List<String> successJobNames = new ArrayList<>();
		List<String> failJobNames = new ArrayList<>();
		for (String jobName : jobNames) {
			try {
				jobService.removeJob(namespace, jobName);
				successJobNames.add(jobName);
			} catch (Exception e) {
				failJobNames.add(jobName);
				log.info("remove job failed, cause of {}", e);
			}
		}
		if (!failJobNames.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("删除成功的作业:").append(successJobNames).append("，").append("删除失败的作业:").append(failJobNames);
			throw new SaturnJobConsoleException(message.toString());
		}
		return new SuccessResponseEntity();
	}

	/**
	 * 批量设置作业的优先Executor
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/preferExecutors")
	public SuccessResponseEntity batchSetPreferExecutors(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobNames") @RequestParam List<String> jobNames,
			@AuditParam("preferList") @RequestParam String preferList) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobBatchSetPreferExecutors, namespace);
		String userName = getCurrentLoginUserName();
		for (String jobName : jobNames) {
			jobService.setPreferList(namespace, jobName, preferList, userName);
		}
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/jobs")
	public SuccessResponseEntity createJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace, JobConfig jobConfig)
			throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobAdd, namespace);
		jobService.addJob(namespace, jobConfig, getCurrentLoginUserName());
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/{jobNameCopied}/copy")
	public SuccessResponseEntity copyJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobNameCopied") @PathVariable String jobNameCopied, JobConfig jobConfig)
			throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobCopy, namespace);
		jobService.copyJob(namespace, jobConfig, jobNameCopied, getCurrentLoginUserName());
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/import")
	public SuccessResponseEntity importJobs(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace, @RequestParam("file") MultipartFile file)
			throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobImport, namespace);
		if (file.isEmpty()) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "请上传一个有内容的文件");
		}
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || !originalFilename.endsWith(".xls")) {
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST, "仅支持.xls文件导入");
		}
		AuditInfoContext.put("originalFilename", originalFilename);
		return new SuccessResponseEntity(jobService.importJobs(namespace, file, getCurrentLoginUserName()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@GetMapping(value = "/export")
	public void exportJobs(final HttpServletRequest request, @AuditParam("namespace") @PathVariable String namespace,
			final HttpServletResponse response) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.jobExport, namespace);
		File exportJobFile = jobService.exportJobs(namespace);
		String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		String exportFileName = namespace + "_allJobs_" + currentTime + ".xls";
		SaturnConsoleUtils.exportFile(response, exportJobFile, exportFileName, true);
	}

	/**
	 * 获取该作业可选择的优先Executor
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/{jobName}/executors")
	public SuccessResponseEntity getExecutors(final HttpServletRequest request, @PathVariable String namespace,
			@PathVariable String jobName) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(jobService.getCandidateExecutors(namespace, jobName));
	}

}
