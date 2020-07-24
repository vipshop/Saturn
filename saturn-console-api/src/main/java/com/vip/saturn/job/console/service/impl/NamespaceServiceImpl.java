/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ServerBriefInfo;
import com.vip.saturn.job.console.domain.ServerStatus;
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
import com.vip.saturn.job.console.utils.SaturnBeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vip.saturn.job.console.exception.SaturnJobConsoleException.ERROR_CODE_BAD_REQUEST;

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
					"srcNamespace and destNamespace should be difference");
		}

		try {
			List<String> successfullyImportedJobs = new ArrayList<>();
			List<String> failedJobs = new ArrayList<>();
			Map<String, List> result = new HashMap<>();
			result.put("success", successfullyImportedJobs);
			result.put("fail", failedJobs);

			List<JobConfig> jobConfigs = jobService.getUnSystemJobs(srcNamespace);
			List<JobConfig> jobConfigUpdatedList = new ArrayList<>();
			for (JobConfig jobConfig : jobConfigs) {
				String jobName = jobConfig.getJobName();
				try {
					// 如果存在上下游关联关系，直接导入会检验不通过；需要先解除关联关系，创建成功后再更新关联关系
					JobConfig jobConfigUpdated = null;
					if (StringUtils.isBlank(jobConfig.getUpStream())
							|| StringUtils.isBlank(jobConfig.getDownStream())) {
						jobConfigUpdated = new JobConfig();
						jobConfigUpdated.setJobName(jobName);
						jobConfigUpdated.setUpStream(jobConfig.getUpStream());
						jobConfigUpdated.setDownStream(jobConfig.getDownStream());
						jobConfig.setUpStream(null);
						jobConfig.setDownStream(null);
					}
					jobService.addJob(destNamespace, jobConfig, createdBy);
					if (jobConfigUpdated != null) {
						jobConfigUpdatedList.add(jobConfigUpdated);
					}
					successfullyImportedJobs.add(jobName);
				} catch (SaturnJobConsoleException e) {
					log.warn("fail to import job {} from {} to {}", jobName, srcNamespace, destNamespace, e);
					failedJobs.add(jobName);
				}
			}
			for (JobConfig jobConfig : jobConfigUpdatedList) {
				String jobName = jobConfig.getJobName();
				try {
					jobService.updateJobConfig(destNamespace, jobConfig, createdBy);
				} catch (SaturnJobConsoleException e) {
					log.warn("fail to update job upStream or downStream, namespace is {} jobName is {}", destNamespace,
							jobName, e);
					failedJobs.add(jobName);
					successfullyImportedJobs.remove(jobName);
				}
			}
			return result;
		} catch (SaturnJobConsoleException e) {
			log.warn("import jobs from {} to {} fail", srcNamespace, destNamespace, e);
			throw e;
		}
	}

	@Override
	public void deleteNamespace(String namespace) throws SaturnJobConsoleException {
		boolean online = isExecutorsOnline(namespace);
		if (online) {
			log.info("namespace {} has online executor, can not delete it", namespace);
			throw new SaturnJobConsoleException(ERROR_CODE_BAD_REQUEST,
					"namespace has online executor, can not delete it");
		} else {
			RegistryCenterConfiguration registryCenterConfiguration = registryCenterService
					.findConfigByNamespace(namespace);
			deleteInfosInDB(namespace);
			deleteNamespaceInZk(registryCenterConfiguration, namespace);
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