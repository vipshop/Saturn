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

import com.vip.saturn.job.console.domain.NamespaceDomainInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;
import com.vip.saturn.job.console.mybatis.repository.NamespaceZkClusterMappingRepository;
import com.vip.saturn.job.console.service.NamespaceAndJobService;
import com.vip.saturn.job.console.service.NamespaceService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.SaturnThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.vip.saturn.job.console.service.impl.RegistryCenterServiceImpl.ERR_MSG_NS_ALREADY_EXIST;

/**
 * @author Ray Leung
 */
public class NamespaceAndJobServiceImpl implements NamespaceAndJobService {

	private static final Logger logger = LoggerFactory.getLogger(NamespaceAndJobServiceImpl.class);

	@Autowired
	private RegistryCenterService registryCenterService;

	@Autowired
	private NamespaceZkClusterMappingRepository namespaceZkClusterMappingRepository;

	@Autowired
	private NamespaceService namespaceService;

	private ExecutorService executorService;

	@PostConstruct
	public void init() {
		if (executorService == null) {
			executorService = Executors.newFixedThreadPool(5, new SaturnThreadFactory("Saturn-NamespaceAndJob-Thread"));
		}
	}

	@PreDestroy
	public void destroy() {
		if (executorService != null) {
			executorService.shutdownNow();
		}
	}

	@Override
	public void createNamespaceAndCloneJobs(String srcNamespace, String namespace, String zkClusterName,
			String createBy) throws SaturnJobConsoleException {
		logger.info("start createNamespaceAndCloneJobs, srcNamespace: {}, namespace: {}, zkClusterName: {}",
				srcNamespace,
				namespace, zkClusterName);
		NamespaceZkClusterMapping mapping = namespaceZkClusterMappingRepository.selectByNamespace(srcNamespace);
		if (mapping == null) {
			throw new SaturnJobConsoleException("no zkCluster mapping is not found");
		}

		NamespaceDomainInfo namespaceInfo = new NamespaceDomainInfo();
		namespaceInfo.setNamespace(namespace);
		namespaceInfo.setZkCluster(zkClusterName);
		namespaceInfo.setContent("");
		try {
			registryCenterService.createNamespace(namespaceInfo);
			registryCenterService.refreshRegistryCenterForNamespace(zkClusterName, srcNamespace);
		} catch (SaturnJobConsoleHttpException e) {
			if (StringUtils.equals(String.format(ERR_MSG_NS_ALREADY_EXIST, namespace), e.getMessage())) {
				logger.warn("namespace already exists, ignore this exception and move on");
			} else {
				throw e;
			}
		}
		namespaceService.importJobsFromNamespaceToNamespace(srcNamespace, namespace, createBy);
		logger.info("finish createNamespaceAndCloneJobs, srcNamespace: {}, namespace: {}, zkClusterName: {}",
				srcNamespace,
				namespace, zkClusterName);
	}

	@Override
	public void asyncCreateNamespaceAndCloneJobs(final String srcNamespace, final String namespace,
			final String zkClusterName, final String createBy) {
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					createNamespaceAndCloneJobs(srcNamespace, namespace, zkClusterName, createBy);
				} catch (SaturnJobConsoleException e) {
					logger.warn("fail to create and clone jobs, srcNamespace: {}, namespace: {}, zkClusterName: {}",
							srcNamespace, namespace, zkClusterName, e);
				}
			}
		});
	}
}
