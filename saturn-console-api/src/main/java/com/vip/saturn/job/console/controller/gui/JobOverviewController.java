package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.domain.ExecutorProvided;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import com.vip.saturn.job.console.vo.DependencyJob;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

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

	@GetMapping(value = "/jobs")
	public ResponseEntity<RequestResult> getJobs(final HttpServletRequest request, @RequestParam String namespace)
			throws SaturnJobConsoleException {
		return new ResponseEntity<>(new RequestResult(true, jobService.getJobs(namespace)), HttpStatus.OK);
	}

	@GetMapping(value = "/groups")
	public ResponseEntity<RequestResult> getGroups(final HttpServletRequest request, @RequestParam String namespace)
			throws SaturnJobConsoleException {
		return new ResponseEntity<>(new RequestResult(true, jobService.getGroups(namespace)), HttpStatus.OK);
	}

	/**
	 * 获取该作业依赖的所有作业
	 */
	@GetMapping(value = "/depending-jobs")
	public ResponseEntity<RequestResult> getDependingJobs(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependencyJobs = jobService.getDependingJobs(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, dependencyJobs), HttpStatus.OK);
	}

	@GetMapping(value = "/depending-jobs-batch")
	public ResponseEntity<RequestResult> batchGetDependingJob(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
		Map<String, List<DependencyJob>> dependencyJobsMap = new HashMap<>();
		for (String jobName : jobNames) {
			List<DependencyJob> dependencyJobs = jobService.getDependingJobs(namespace, jobName);
			dependencyJobsMap.put(jobName, dependencyJobs);
		}
		return new ResponseEntity<>(new RequestResult(true, dependencyJobsMap), HttpStatus.OK);
	}

	/**
	 * 获取依赖该作业的所有作业
	 */
	@GetMapping(value = "/depended-jobs")
	public ResponseEntity<RequestResult> getDependedJobs(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		List<DependencyJob> dependedJobs = jobService.getDependedJobs(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, dependedJobs), HttpStatus.OK);
	}

	@GetMapping(value = "/depended-jobs-batch")
	public ResponseEntity<RequestResult> batchGetDependedJobs(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam List<String> jobNames)
			throws SaturnJobConsoleException {
		Map<String, List<DependencyJob>> dependencyJobsMap = new HashMap<>();
		for (String jobName : jobNames) {
			List<DependencyJob> dependedJobs = jobService.getDependedJobs(namespace, jobName);
			dependencyJobsMap.put(jobName, dependedJobs);
		}
		return new ResponseEntity<>(new RequestResult(true, dependencyJobsMap), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/enable-job")
	public ResponseEntity<RequestResult> enableJob(final HttpServletRequest request,
			@RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		jobService.enableJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/enable-job-batch")
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
	@PostMapping(value = "/disable-job")
	public ResponseEntity<RequestResult> disableJob(final HttpServletRequest request, @RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		jobService.disableJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/disable-job-batch")
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
	@PostMapping(value = "/remove-job")
	public ResponseEntity<RequestResult> removeJob(final HttpServletRequest request, @RequestParam String namespace,
			@RequestParam String jobName) throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobName(jobName);
		jobService.removeJob(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/remove-job-batch")
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

	/**
	 * 获取该域下所有在线的Executor，用于批量选择优先Executor
	 */
	@GetMapping(value = "/online-executors")
	public ResponseEntity<RequestResult> getOnlineExecutors(final HttpServletRequest request,
			@RequestParam String namespace) throws SaturnJobConsoleException {
		List<ExecutorProvided> onlineExecutors = jobService.getOnlineExecutors(namespace);
		return new ResponseEntity<>(new RequestResult(true, onlineExecutors), HttpStatus.OK);
	}

	/**
	 * 批量设置作业的优先Executor
	 */
	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/set-prefer-executors-batch")
	public ResponseEntity<RequestResult> batchSetPreferExecutors(final HttpServletRequest request,
			@RequestParam String namespace, @RequestParam List<String> jobNames, @RequestParam String preferList)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.putJobNames(jobNames);
		AuditInfoContext.put("preferList", preferList);
		for (String jobName : jobNames) {
			jobService.setPreferList(namespace, jobName, preferList);
		}
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/add-job")
	public ResponseEntity<RequestResult> addJob(final HttpServletRequest request,
			@RequestParam String namespace, JobConfig jobConfig)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		AuditInfoContext.put("jobConfig", jobConfig.toString());
		jobService.addJob(namespace, jobConfig);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@PostMapping(value = "/import-jobs")
	public ResponseEntity<RequestResult> importJobs(final HttpServletRequest request,
			@RequestParam String namespace, @RequestParam("file") MultipartFile file)
			throws SaturnJobConsoleException {
		AuditInfoContext.putNamespace(namespace);
		if (file.isEmpty()) {
			throw new SaturnJobConsoleGUIException("请上传一个有内容的文件");
		}
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || !originalFilename.endsWith(".xls")) {
			throw new SaturnJobConsoleGUIException("仅支持.xls文件导入");
		}
		AuditInfoContext.put("originalFilename", originalFilename);
		jobService.importJobs(namespace, file);
		return new ResponseEntity<>(new RequestResult(true, ""), HttpStatus.OK);
	}

	@Audit(type = AuditType.WEB)
	@GetMapping(value = "exportJob")
	public void exportJob(HttpServletRequest request, @RequestParam String namespace, HttpServletResponse response)
			throws SaturnJobConsoleException {
		File exportJobFile = null;
		try {
			exportJobFile = jobService.exportJobs(namespace);

			String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
			String fileName = getNamespace() + "_allJobs_" + currentTime + ".xls";

			response.setContentType("application/octet-stream");
			response.setHeader("Content-disposition",
					"attachment; filename=" + new String(fileName.getBytes("UTF-8"), "ISO8859-1"));

			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(exportJobFile));
			BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
			byte[] buff = new byte[2048];
			int bytesRead;
			while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
				bos.write(buff, 0, bytesRead);
			}
			bis.close();
			bos.close();
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
	@GetMapping(value = "/executors")
	public ResponseEntity<RequestResult> getExecutors(final HttpServletRequest request,
			@RequestParam String namespace, @RequestParam String jobName) throws SaturnJobConsoleException {
		List<ExecutorProvided> executors = jobService.getExecutors(namespace, jobName);
		return new ResponseEntity<>(new RequestResult(true, executors), HttpStatus.OK);
	}

}
