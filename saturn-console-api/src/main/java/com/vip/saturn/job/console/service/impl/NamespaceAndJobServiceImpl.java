package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.NamespaceDomainInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;
import com.vip.saturn.job.console.mybatis.repository.NamespaceZkClusterMappingRepository;
import com.vip.saturn.job.console.service.NamespaceAndJobService;
import com.vip.saturn.job.console.service.NamespaceService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.*;

import static com.vip.saturn.job.console.service.impl.RegistryCenterServiceImpl.ERR_MSG_NS_ALREADY_EXIST;

/**
 * @author Ray Leung
 */

public class NamespaceAndJobServiceImpl implements NamespaceAndJobService {

	private static final Logger log = LoggerFactory.getLogger(NamespaceAndJobServiceImpl.class);
	private static final ExecutorService executorService;

	@Autowired
	private RegistryCenterService registryCenterService;

	@Autowired
	private NamespaceZkClusterMappingRepository namespaceZkClusterMappingRepository;

	@Autowired
	private NamespaceService namespaceService;

	static {
		executorService = new ThreadPoolExecutor(5, 5, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5),
				new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r, "Saturn-NamespaceAndJob-Thread");
					}
				});
	}

	@Override
	public void createNamespaceAndCloneJobs(String srcNamespace, String namespace, String zkClusterName,
			String createBy) throws SaturnJobConsoleException {
		log.info("start createNamespaceAndCloneJobs, srcNamesapce:{}, namespace:{}, zkClusterName:{}", srcNamespace,
				namespace, zkClusterName);
		NamespaceZkClusterMapping mapping = namespaceZkClusterMappingRepository.selectByNamespace(srcNamespace);
		if (mapping == null) {
			throw new SaturnJobConsoleException("no zkcluster mapping is not found");
		}

		NamespaceDomainInfo namespaceInfo = new NamespaceDomainInfo();
		namespaceInfo.setNamespace(namespace);
		namespaceInfo.setZkCluster(zkClusterName);
		namespaceInfo.setContent("");

		try {
			registryCenterService.createNamespace(namespaceInfo);
			registryCenterService.refreshRegistryCenterForNamespace(zkClusterName, srcNamespace);
		} catch (SaturnJobConsoleHttpException ex) {
			if (StringUtils.equals(String.format(ERR_MSG_NS_ALREADY_EXIST, namespace), ex.getMessage())) {
				log.warn("namespace already exists, ignore this exception and move on");
			} else {
				throw ex;
			}
		}
		namespaceService.importJobsFromNamespaceToNamespace(srcNamespace, namespace, createBy);
		log.info("end createNamespaceAndCloneJobs, srcNamesapce:{}, namespace:{}, zkClusterName:{}", srcNamespace,
				namespace, zkClusterName);
	}

	@Override
	public void aysncCreateNamespaceAndCloneJobs(final String srcNamespace, final String namespace,
			final String zkClusterName, final String createBy) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					createNamespaceAndCloneJobs(srcNamespace, namespace, zkClusterName, createBy);
				} catch (SaturnJobConsoleException e) {
					log.warn("fail to create and clone jobs, srcNamespace:{}, namespace:{}, zkClusterName:{}",
							srcNamespace, namespace, zkClusterName, e);
				}
			}
		};
		executorService.submit(runnable);
	}
}
