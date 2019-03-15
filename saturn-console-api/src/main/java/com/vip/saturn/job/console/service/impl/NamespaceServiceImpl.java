package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.repository.CurrentJobConfigRepository;
import com.vip.saturn.job.console.mybatis.repository.NamespaceInfoRepository;
import com.vip.saturn.job.console.mybatis.repository.NamespaceZkClusterMappingRepository;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.NamespaceService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

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

	@Autowired
	private ExecutorService executorService;

	@Autowired
	private NamespaceInfoRepository namespaceInfoRepository;

	@Autowired
	private CurrentJobConfigRepository currentJobConfigRepository;

	@Autowired
	private NamespaceZkClusterMappingRepository namespaceZkClusterMappingRepository;

	@Autowired
	private RegistryCenterService registryCenterService;

	@Autowired
	private CuratorRepository curatorRepository;

	@Override
	public Map<String, List> importJobsFromNamespaceToNamespace(String srcNamespace, String destNamespace,
			String createdBy) throws SaturnJobConsoleException {

		if (StringUtils.isBlank(srcNamespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), "srcNamespace should not be null");
		}
		if (StringUtils.isBlank(destNamespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), "destNamespace should not be null");
		}
		if (StringUtils.equals(srcNamespace, destNamespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					"destNamespace and destNamespace should be difference");
		}


		NamespaceDomainInfo namespaceDomainInfo = registryCenterService.getNamespace(destNamespace);
		registryCenterService.refreshRegistryCenterForNamespace(namespaceDomainInfo.getZkCluster(), destNamespace);

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

	@Override
	public boolean deleteNamespace(String namespace) throws SaturnJobConsoleException {
		boolean online = isExecutorsOnline(namespace);
		if (online) {
			log.info("namespace {} has online executor, can not delete it", namespace);
			return false;
		} else {
			RegistryCenterConfiguration registryCenterConfiguration = registryCenterService
					.findConfigByNamespace(namespace);
			deleteInfosInDB(namespace);
			deleteNamespaceInZk(registryCenterConfiguration, namespace);
			return true;
		}
	}

	@Transactional(rollbackFor = Exception.class)
	protected void deleteInfosInDB(String namespace) {
		deleteNamespaceInDB(namespace);
		log.info("delete namespace in DB success - namespace {}", namespace);
		deleteNamespaceZkClusterMappingInD(namespace);
		log.info("delete namespaceZkCluster in DB success - namespace {}", namespace);
		deleteJobsInDB(namespace);
		log.info("delete jobs in DB success - namespace {}", namespace);
	}

	protected void deleteNamespaceZkClusterMappingInD(String namespace) {
		namespaceZkClusterMappingRepository.deleteByNamespace(namespace);
	}

	protected void deleteJobsInDB(String namespace) {
		currentJobConfigRepository.deleteByNamespace(namespace);
	}

	protected void deleteNamespaceInZk(RegistryCenterConfiguration registryCenterConfiguration, String namespace) {
		CuratorFramework curatorFramework = null;
		try {
			curatorFramework = curatorRepository.connect(registryCenterConfiguration.getZkAddressList(), null,
					registryCenterConfiguration.getDigest());
			curatorFramework.delete().deletingChildrenIfNeeded().forPath("/" + namespace);
			log.info("delete namespace in zk success - namespace {}", namespace);
		} catch (Exception e) {
			log.warn("fail to delete namespace:{}", namespace, e);
		} finally {
			if (curatorFramework != null) {
				curatorFramework.close();
			}
		}
	}

	protected void deleteNamespaceInDB(String namespace) {
		namespaceInfoRepository.deleteByNamespace(namespace);
	}

	protected boolean isExecutorsOnline(String namespace) throws SaturnJobConsoleException {
		List<ServerBriefInfo> executors = executorService.getExecutors(namespace, ServerStatus.ONLINE);
		if (executors.size() > 0) {
			return true;
		}
		return false;
	}
}