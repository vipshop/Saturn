package com.vip.saturn.job.console.controller.gui;

import com.google.common.collect.Lists;
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
import org.springframework.util.CollectionUtils;
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

	private static final String QUERY_CONDITION_STATUS = "status";

	private static final String QUERY_CONDITION_GROUP = "groups";

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
		if (condition.containsKey(QUERY_CONDITION_STATUS)) {
			String statusStr = checkAndGetParametersValueAsString(condition, QUERY_CONDITION_STATUS, false);
			JobStatus jobStatus = JobStatus.getJobStatus(statusStr);
			if (jobStatus != null) {
				return new SuccessResponseEntity(
						getJobOverviewByStatusAndPage(namespace, jobStatus, condition, page, size));
			}
		}
		return new SuccessResponseEntity(getJobOverviewByPage(namespace, condition, page, size));
	}

    /**
     * 获取域下的总作业数、启用作业数和异常作业数
     * @param namespace 域名
     * @return 总作业数、启用作业数和异常作业数总数
     */
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class) })
    @GetMapping(value = "/counts")
	public SuccessResponseEntity countJobsStatus(final HttpServletRequest request, @PathVariable String namespace) {
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
			@RequestParam(required = false) List<String> jobNames) throws SaturnJobConsoleException {
		Map<String, String> jobShardingMap = new HashMap<>();
		if (!CollectionUtils.isEmpty(jobNames)) {
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
        }
		return new SuccessResponseEntity(jobShardingMap);
	}


	private JobOverviewVo getJobOverviewByPage(String namespace, Map<String, Object> condition, int page,
			int size) throws SaturnJobConsoleException {
        JobOverviewVo jobOverviewVo = new JobOverviewVo();
        try {
            preHandleCondition(condition);

            List<JobConfig> unSystemJobs = jobService.getUnSystemJobsWithCondition(namespace, condition, page, size);
			if (unSystemJobs == null || unSystemJobs.isEmpty()) {
				jobOverviewVo.setJobs(Lists.<JobOverviewJobVo>newArrayList());
				jobOverviewVo.setTotalNumber(0);
				return jobOverviewVo;
			}

			List<JobOverviewJobVo> jobOverviewList = updateJobOverviewDetail(namespace, unSystemJobs, null);

			jobOverviewVo.setJobs(jobOverviewList);
			jobOverviewVo.setTotalNumber(jobService.countUnSystemJobsWithCondition(namespace, condition));
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}

		return jobOverviewVo;
	}

	private JobOverviewVo getJobOverviewByStatusAndPage(String namespace, JobStatus jobStatus,
			Map<String, Object> condition, int page, int size) throws SaturnJobConsoleException {
		JobOverviewVo jobOverviewVo = new JobOverviewVo();
		try {
			preHandleStatusAndCondition(condition, jobStatus);

			List<JobConfig> unSystemJobs = jobService.getUnSystemJobsWithCondition(namespace, condition, page, size);
			if (unSystemJobs == null || unSystemJobs.isEmpty()) {
				jobOverviewVo.setJobs(Lists.<JobOverviewJobVo>newArrayList());
				jobOverviewVo.setTotalNumber(0);
				return jobOverviewVo;
			}

			Pageable pageable = PageableUtil.generatePageble(page, size);

			List<JobConfig> targetJobs = getJobSubListByPage(unSystemJobs, pageable);
			List<JobOverviewJobVo> jobOverviewList = updateJobOverviewDetail(namespace, targetJobs, jobStatus);

			jobOverviewVo.setJobs(jobOverviewList);
			jobOverviewVo.setTotalNumber(unSystemJobs.size());
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}

		return jobOverviewVo;
	}


	private void preHandleCondition(Map<String, Object> condition) {
		if (condition.containsKey(QUERY_CONDITION_GROUP) && SaturnConstants.NO_GROUPS_LABEL
				.equals(condition.get(QUERY_CONDITION_GROUP))) {
			condition.put(QUERY_CONDITION_GROUP, "");
        }
	}

	private void preHandleStatusAndCondition(Map<String, Object> condition, JobStatus jobStatus) {
		condition.put("jobStatus", jobStatus);
		if (JobStatus.STOPPED.equals(jobStatus) || JobStatus.STOPPING.equals(jobStatus)) {
			condition.put("isEnabled", SaturnConstants.JOB_IS_DISABLE);
		} else {
			condition.put("isEnabled", SaturnConstants.JOB_IS_ENABLE);
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

	private List<JobOverviewJobVo> updateJobOverviewDetail(String namespace, List<JobConfig> unSystemJobs,
			JobStatus jobStatus) {
		List<JobOverviewJobVo> result = Lists.newArrayList();
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
                    jobOverviewJobVo.setStatus(jobService.getJobStatus(namespace, jobConfig));
                } else {
                    jobOverviewJobVo.setStatus(jobStatus);
                }
				result.add(jobOverviewJobVo);
            } catch (Exception e) {
                log.error("list job " + jobConfig.getJobName() + " error", e);
            }
        }

		return result;
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

	protected List<JobConfig> getJobSubListByPage(List<JobConfig> unSystemJobs, Pageable pageable) {
        int totalCount = unSystemJobs.size();
        int offset = pageable.getOffset();
        int end = offset + pageable.getPageSize();
        int fromIndex = totalCount >= offset ? offset : -1;
        int toIndex = totalCount >= end ? end : totalCount;
        if (fromIndex == -1 || fromIndex > toIndex) {
            return Lists.newArrayList();
        }
        return unSystemJobs.subList(fromIndex, toIndex);
    }

	private JobOverviewVo countJobOverviewVo(String namespace) {
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
