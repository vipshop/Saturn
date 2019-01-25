package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.NamespaceService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rayleung
 */
public class NamespaceServiceImpl implements NamespaceService {

	private static final Logger log = LoggerFactory.getLogger(NamespaceServiceImpl.class);

	@Autowired
	private JobService jobService;

	@Override
	public Map<String, List> importJobsFromNamespaceToNamespace(String srcNamespace, String destNamespace,
			String createdBy) throws SaturnJobConsoleException {

		if (StringUtils.isBlank(srcNamespace)) {
			throw new SaturnJobConsoleException(HttpStatus.BAD_REQUEST.value(), "srcNamespace should not be null");
		}
		if (StringUtils.isBlank(destNamespace)) {
			throw new SaturnJobConsoleException(HttpStatus.BAD_REQUEST.value(), "destNamespace should not be null");
		}
		if (StringUtils.equals(srcNamespace, destNamespace)) {
			throw new SaturnJobConsoleException(HttpStatus.BAD_REQUEST.value(), "destNamespace and destNamespace should be difference");
		}

		try {
			List<String> successfullyImportedJobs = new ArrayList<>();
			List<String> failedJobs = new ArrayList<>();
			Map result = new HashMap(2);
			result.put("success", successfullyImportedJobs);
			result.put("fail", failedJobs);

			List<JobConfig> jobConfigs = jobService.getUnSystemJobs(srcNamespace);
			for (int i = 0; i < jobConfigs.size(); i++) {
				JobConfig jobConfig = jobConfigs.get(i);
				try {
					jobService.addJob(destNamespace, jobConfig, createdBy);
					successfullyImportedJobs.add(jobConfig.getJobName());
				} catch (SaturnJobConsoleException e) {
					log.warn("fail to import job {} from {} to {}", jobConfig.getJobName(), srcNamespace, destNamespace,
							e);
					failedJobs.add(jobConfig.getJobName());
				}
			}
			return result;
		} catch (SaturnJobConsoleException e) {
			log.warn("import jobs from {} to {} fail", srcNamespace, destNamespace, e);
			throw e;
		}
	}
}