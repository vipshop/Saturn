package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.DependencyJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Job overview page controller.
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/{namespace:.+}/jobs")
public class JobOverviewController extends AbstractGUIController {

	private static final Logger log = LoggerFactory.getLogger(JobOverviewController.class);

	@Resource
	private JobService jobService;

	@Resource
	private ExecutorService executorService;

	@GetMapping
	public SuccessResponseEntity getJobs(final HttpServletRequest request, @PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(jobService.getJobOverviewVo(namespace));
	}

	@GetMapping(value = "/groups")
	public SuccessResponseEntity getGroups(final HttpServletRequest request, @PathVariable String namespace)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(jobService.getGroups(namespace));
	}

	/**
	 * 获取该作业依赖的所有作业
	 */
	@GetMapping(value = "/{jobName}/dependency")
	public SuccessResponseEntity getDependingJobs(final HttpServletRequest request,
			@PathVariable String namespace,
			@PathVariable String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependencyJobs = jobService.getDependingJobs(namespace, jobName);
		return new SuccessResponseEntity(dependencyJobs);
	}

	@GetMapping(value = "/dependency")
	public SuccessResponseEntity batchGetDependingJob(final HttpServletRequest request,
			@PathVariable String namespace,
			@RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
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
	@GetMapping(value = "/{jobName}/beDependedJobs")
	public SuccessResponseEntity getDependedJobs(final HttpServletRequest request,
			@PathVariable String namespace, @PathVariable String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependedJobs = jobService.getDependedJobs(namespace, jobName);
		return new SuccessResponseEntity(dependedJobs);
	}

	@GetMapping(value = "/beDependedJobs")
	public SuccessResponseEntity batchGetDependedJobs(final HttpServletRequest request,
			@PathVariable String namespace, @RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
		Map<String, List<DependencyJob>> dependencyJobsMap = new HashMap<>();
		for (String jobName : jobNames) {
			List<DependencyJob> dependedJobs = jobService.getDependedJobs(namespace, jobName);
			dependencyJobsMap.put(jobName, dependedJobs);
		}
		return new SuccessResponseEntity(dependencyJobsMap);
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/{jobName}/enable")
	public SuccessResponseEntity enableJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		jobService.enableJob(namespace, jobName);
		return new SuccessResponseEntity();
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/enable")
	public SuccessResponseEntity batchEnableJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobNames") @RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
		for (String jobName : jobNames) {
			jobService.enableJob(namespace, jobName);
		}
		return new SuccessResponseEntity();
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/{jobName}/disable")
	public SuccessResponseEntity disableJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		jobService.disableJob(namespace, jobName);
		return new SuccessResponseEntity();
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/disable")
	public SuccessResponseEntity batchDisableJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobNames") @RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
		for (String jobName : jobNames) {
			jobService.disableJob(namespace, jobName);
		}
		return new SuccessResponseEntity();
	}

	@Audit(type = AuditType.WEB)
	@DeleteMapping(value = "/{jobName}")
	public SuccessResponseEntity removeJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		jobService.removeJob(namespace, jobName);
		return new SuccessResponseEntity();
	}

	@Audit(type = AuditType.WEB)
	@DeleteMapping
	public SuccessResponseEntity batchRemoveJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobNames") @RequestParam List<String> jobNames) throws SaturnJobConsoleException {
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
		return new SuccessResponseEntity();
	}

	/**
	 * 获取该域下所有在线的Executor，用于批量选择优先Executor
	 */
	@Deprecated
	// 前端传参数
	@GetMapping(value = "/executors")
	public SuccessResponseEntity getOnlineExecutors(final HttpServletRequest request,
			@PathVariable String namespace) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(executorService.getExecutors(namespace, ServerStatus.ONLINE));
	}

	/**
	 * 批量设置作业的优先Executor
	 */
	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/setPreferExecutors")
	public SuccessResponseEntity batchSetPreferExecutors(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobNames") @RequestParam List<String> jobNames,
			@AuditParam("preferList") @RequestParam String preferList)
			throws SaturnJobConsoleException {
		for (String jobName : jobNames) {
			jobService.setPreferList(namespace, jobName, preferList);
		}
		return new SuccessResponseEntity();
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/jobs")
	public SuccessResponseEntity createJob(final HttpServletRequest request,
			@AuditParam("namespace") @PathVariable String namespace, JobConfig jobConfig)
			throws SaturnJobConsoleException {
		jobService.addJob(namespace, jobConfig);
		return new SuccessResponseEntity();
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/{jobNameCopied}/copy")
	public SuccessResponseEntity copyJob(final HttpServletRequest request,
			@AuditParam("namespace") @RequestParam String namespace,
			@AuditParam("jobNameCopied") @PathVariable String jobNameCopied, JobConfig jobConfig)
			throws SaturnJobConsoleException {
		jobService.copyJob(namespace, jobConfig, jobNameCopied);
		return new SuccessResponseEntity();
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/import")
	public SuccessResponseEntity importJobs(final HttpServletRequest request,
			@AuditParam("namespace") @RequestParam String namespace, @RequestParam("file") MultipartFile file)
			throws SaturnJobConsoleException {
		if (file.isEmpty()) {
			throw new SaturnJobConsoleGUIException("请上传一个有内容的文件");
		}
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || !originalFilename.endsWith(".xls")) {
			throw new SaturnJobConsoleGUIException("仅支持.xls文件导入");
		}
		AuditInfoContext.put("originalFilename", originalFilename);
		jobService.importJobs(namespace, file);
		return new SuccessResponseEntity();
	}

	@Audit(type = AuditType.WEB)
	@GetMapping(value = "/export")
	public void exportJobs(final HttpServletRequest request, @AuditParam("namespace") @RequestParam String namespace,
			final HttpServletResponse response)
			throws SaturnJobConsoleException {
		File exportJobFile = null;
		try {
			exportJobFile = jobService.exportJobs(namespace);

			String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
			String fileName = namespace + "_allJobs_" + currentTime + ".xls";

			response.setContentType("application/octet-stream");
			response.setHeader("Content-disposition",
					"attachment; filename=" + new String(fileName.getBytes("UTF-8"), "ISO8859-1"));

			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			try {
				bis = new BufferedInputStream(new FileInputStream(exportJobFile));
				bos = new BufferedOutputStream(response.getOutputStream());
				byte[] buff = new byte[2048];
				int bytesRead;
				while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
					bos.write(buff, 0, bytesRead);
				}
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
				if (bos != null) {
					try {
						bos.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleGUIException(e);
		} finally {
			if (exportJobFile != null) {
				exportJobFile.delete();
			}
		}
	}

	/**
	 * 获取该作业可选择的优先Executor
	 */
	@GetMapping(value = "/{jobName}/executors")
	public SuccessResponseEntity getExecutors(final HttpServletRequest request,
			@PathVariable String namespace, @PathVariable String jobName) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(jobService.getCandidateExecutors(namespace, jobName));
	}

}
