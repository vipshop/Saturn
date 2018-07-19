package com.vip.saturn.job.console.controller.gui;

import com.google.common.collect.Maps;
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
import org.springframework.data.domain.Pageable;
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

	/**
	 * 按条件分页获取域下所有作业的细节信息
	 * @param namespace 域名
	 * @return 作业细节
	 */
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class) })
	@GetMapping
	public SuccessResponseEntity getJobsWithCondition(final HttpServletRequest request, @PathVariable String namespace,
			@RequestParam Map<String, Object> condition, @RequestParam(required = false, defaultValue = "1") int page,
			@RequestParam(required = false, defaultValue = "25") int size) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(getJobOverviewByPage(namespace, condition, page, size));
	}

    /**
     * 获取域下的总作业数、启用作业数和异常作业数
     * @param namespace 域名
     * @return 总作业数、启用作业数和异常作业数总数
     */
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class) })
    @GetMapping(value = "/counts")
    public SuccessResponseEntity countJobsStatus(final HttpServletRequest request, @PathVariable String namespace
            ) throws SaturnJobConsoleException {
        return new SuccessResponseEntity(countJobOverviewVo(namespace));
    }

	/**
	 * 获取域下所有作业的名字
	 * @param namespace 域名
	 * @return 全域作业名字
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/names")
	public SuccessResponseEntity getJobNames(@PathVariable String namespace) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(jobService.getJobNames(namespace));
	}

	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class) })
	@GetMapping(value = "/sharding/status")
	public SuccessResponseEntity getJobsShardingStatus(@PathVariable String namespace,
			@RequestParam List<String> jobNames) throws SaturnJobConsoleException {
		Map<String, String> jobShardingMap = new HashMap<>();
		for (String jobName : jobNames) {
			JobStatus jobStatus = jobService.getJobStatus(namespace, jobName);
			boolean isAllocated = !JobStatus.STOPPED.equals(jobStatus)
					&& jobService.isJobShardingAllocatedExecutor(namespace, jobName);
			if (isAllocated) {
				jobShardingMap.put(jobName, "已分配");
			} else {
				jobShardingMap.put(jobName, "未分配");
			}
		}
		return new SuccessResponseEntity(jobShardingMap);
	}

	public JobOverviewVo getJobOverviewByPage(String namespace, Map<String, Object> condition, int page,
			int size) throws SaturnJobConsoleException {
        JobOverviewVo jobOverviewVo = new JobOverviewVo();
        try {
            preHandleCondition(condition);
            List<JobOverviewJobVo> jobList = new ArrayList<>();
            List<JobConfig> unSystemJobs = jobService.getUnSystemJobsWithCondition(namespace, condition, page, size);
            // 当查询条件ready、running、stopping 状态时，获取作业列表会返回满足状态的所有作业
            // 若查询条件不为前面三种状态，获取作业列表返回相应页的作业集合
            Pageable pageable = PageableUtil.generatePageble(page, size);
            JobStatus jobStatus = (JobStatus) condition.get("jobStatus");
            if (jobStatus == null || JobStatus.STOPPED.equals(jobStatus)) {
                jobOverviewVo.setTotalNumber(jobService.countUnSystemJobsWithCondition(namespace, condition));
            } else {
                jobOverviewVo.setTotalNumber(unSystemJobs.size());
                unSystemJobs = unSystemJobs.subList(pageable.getOffset(), pageable.getPageSize());
            }
            updateJobOverviewDetail(namespace, jobList, unSystemJobs, jobStatus);
            jobOverviewVo.setJobs(jobList);
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Exception e) {
            throw new SaturnJobConsoleException(e);
        }

        return jobOverviewVo;
    }



    private void preHandleCondition(Map<String, Object> condition) throws SaturnJobConsoleException {
        if (condition.containsKey("groups") && SaturnConstants.NO_GROUPS_LABEL.equals(condition.get("groups"))) {
            condition.put("groups", "");
        }
        if (condition.containsKey("status")) {
			JobStatus jobStatus = JobStatus
					.getJobStatus(checkAndGetParametersValueAsString(condition, "status", false));
			condition.put("jobStatus", jobStatus);
			if (jobStatus != null) {
			    if (JobStatus.STOPPED.equals(jobStatus)) {
			        condition.put("isEnabled", 0);
                } else {
			        condition.put("isEnabled", 1);
                }
            }
        }
    }

    private void updateAbnormalJobSizeInOverview(String namespace, JobOverviewVo jobOverviewVo) {
        try {
            List<AbnormalJob> abnormalJobList = alarmStatisticsService.getAbnormalJobListByNamespace(namespace);
            jobOverviewVo.setAbnormalNumber(abnormalJobList.size());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

	private void updateJobOverviewDetail(String namespace, List<JobOverviewJobVo> jobList,
			List<JobConfig> unSystemJobs, JobStatus jobStatus) {
        for (JobConfig jobConfig : unSystemJobs) {
            try {
                jobConfig.setDefaultValues();

                JobOverviewJobVo jobOverviewJobVo = new JobOverviewJobVo();
                SaturnBeanUtils.copyProperties(jobConfig, jobOverviewJobVo);

                updateJobTypesInOverview(jobConfig, jobOverviewJobVo);

                if (StringUtils.isBlank(jobOverviewJobVo.getGroups())) {
                    jobOverviewJobVo.setGroups(SaturnConstants.NO_GROUPS_LABEL);
                }

                if (jobStatus == null) {
                    jobStatus = jobService.getJobStatus(namespace, jobConfig.getJobName());
                }
                jobOverviewJobVo.setStatus(jobStatus);

                if (!JobStatus.STOPPED.equals(jobStatus)) {// 作业如果是STOPPED状态，不需要显示已分配的executor
                    updateShardingListInOverview(namespace, jobConfig, jobOverviewJobVo);
                }
                jobList.add(jobOverviewJobVo);
            } catch (Exception e) {
                log.error("list job " + jobConfig.getJobName() + " error", e);
            }
        }
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
		boolean isAllocated = jobService.isJobShardingAllocatedExecutor(namespace, jobConfig.getJobName());
        if (isAllocated) {
            jobOverviewJobVo.setShardingList("已分配分片");
        }
    }

    public JobOverviewVo countJobOverviewVo(String namespace) throws SaturnJobConsoleException{
        JobOverviewVo jobOverviewVo = new JobOverviewVo();
        jobOverviewVo.setTotalNumber(jobService.countUnSystemJobsWithCondition(namespace,
                Maps.<String, Object>newHashMap()));
        jobOverviewVo.setEnabledNumber(jobService.countEnabledUnSystemJobs(namespace));
        // 获取该域下的异常作业数量，捕获所有异常，打日志，不抛到前台
        updateAbnormalJobSizeInOverview(namespace, jobOverviewVo);
        return jobOverviewVo;
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
