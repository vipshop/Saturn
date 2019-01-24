package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.NamespaceService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rayleung
 */
public class NamespaceServiceImpl implements NamespaceService {

	private static final Logger log = LoggerFactory.getLogger(NamespaceServiceImpl.class);

	@Autowired
	private JobService jobService;

	@Override
	public List<String> importJobsFromNamespaceToNamespace(String srcNamespace, String destNamespace, String createdBy)
			throws SaturnJobConsoleException {

		if (StringUtils.isBlank(srcNamespace)) {
			throw new IllegalArgumentException("srcNamespace should not be null");
		}
		if (StringUtils.isBlank(destNamespace)) {
			throw new IllegalArgumentException("destNamespace should not be null");
		}
		if (StringUtils.equals(srcNamespace, destNamespace)) {
			throw new IllegalArgumentException("destNamespace and destNamespace should be difference");
		}

		try {
			List<String> successfullyImportedJobs = new ArrayList<>();
			List<JobConfig> jobConfigs = jobService.getUnSystemJobs(srcNamespace);
			for (int i = 0; i < jobConfigs.size(); i++) {
				JobConfig jobConfig = jobConfigs.get(i);
				try {
					jobService.addJob(destNamespace, jobConfig, createdBy);
					successfullyImportedJobs.add(jobConfig.getJobName());
				} catch (SaturnJobConsoleException e) {
					log.warn("fail to import job from {} to {}", srcNamespace, destNamespace, e);
				}
			}
			return successfullyImportedJobs;
		} catch (SaturnJobConsoleException e) {
			log.warn("import jobs from {} to {} fail", srcNamespace, destNamespace, e);
			throw e;
		}
	}
}