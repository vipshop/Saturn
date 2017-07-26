package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.MoveNamespaceBatchStatus;
import com.vip.saturn.job.console.domain.NamespaceZkClusterMappingVo;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;
import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMapping4SqlService;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.service.NamespaceZkClusterMappingService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.ConsoleThreadFactory;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author hebelala
 */
@Service
public class NamespaceZkClusterMappingServiceImpl implements NamespaceZkClusterMappingService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceZkClusterMappingServiceImpl.class);

	@Resource
	private ZkClusterInfoService zkClusterInfoService;

	@Resource
	private NamespaceZkClusterMapping4SqlService namespaceZkclusterMapping4SqlService;

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private CurrentJobConfigService currentJobConfigService;

	@Resource
	private JobOperationService jobOperationService;

	@Resource
	private RegistryCenterService registryCenterService;

	private ExecutorService moveNamespaceBatchThreadPool;
	private Map<Long, MoveNamespaceBatchStatus> moveNamespaceBatchStatusMap = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		if (moveNamespaceBatchThreadPool != null) {
			moveNamespaceBatchThreadPool.shutdownNow();
		}
		moveNamespaceBatchThreadPool = Executors
				.newSingleThreadExecutor(new ConsoleThreadFactory("moveNamespaceBatchThread", false));
	}

	@PreDestroy
	public void destroy() {
		if (moveNamespaceBatchThreadPool != null) {
			moveNamespaceBatchThreadPool.shutdownNow();
		}
	}

	@Override
	public List<NamespaceZkClusterMappingVo> getNamespaceZkClusterMappingList() throws SaturnJobConsoleException {
		List<NamespaceZkClusterMappingVo> result = new ArrayList<>();
		List<NamespaceZkClusterMapping> namespaceZkClusterMappingList = namespaceZkclusterMapping4SqlService.getAllMappings();
		if (namespaceZkClusterMappingList != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			for (NamespaceZkClusterMapping tmp : namespaceZkClusterMappingList) {
				try {
					if (tmp.getIsDeleted() == 0) {
						NamespaceZkClusterMappingVo vo = new NamespaceZkClusterMappingVo();
						vo.setNamespace(tmp.getNamespace());
						vo.setZkClusterKey(tmp.getZkClusterKey());
						vo.setCreateTime(sdf.format(tmp.getCreateTime()));
						vo.setCreatedBy(tmp.getCreatedBy());
						vo.setLastUpdateTime(sdf.format(tmp.getLastUpdateTime()));
						vo.setLastUpdatedBy(tmp.getLastUpdatedBy());
						result.add(vo);
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
		return result;
	}

	@Override
	public void initNamespaceZkClusterMapping(String createdBy) throws SaturnJobConsoleException {
		try {
			List<ZkClusterInfo> allZkClusterInfo = zkClusterInfoService.getAllZkClusterInfo();
			if(allZkClusterInfo != null) {
				for(ZkClusterInfo zkClusterInfo : allZkClusterInfo) {
					String clusterKey = zkClusterInfo.getClusterKey();
					String connectString = zkClusterInfo.getConnectString();
					CuratorFramework curatorFramework = null;
					CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService.connectOnly(connectString, null);
					if(curatorFrameworkOp != null) {
						curatorFramework = curatorFrameworkOp.getCuratorFramework();
					}
					if (curatorFramework != null) { // not offline
						try {
							List<String> namespaces = curatorFramework.getChildren().forPath("/");
							if(namespaces != null) {
								for (String namespace : namespaces) {
									if (registryCenterService.namespaceIsCorrect(namespace, curatorFramework)) {
										namespaceZkclusterMapping4SqlService.insert(namespace, "", clusterKey, createdBy);
									}
								}
							}
						} finally {
							curatorFramework.close();
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
	}

	@Override
	public List<String> getZkClusterListWithOnline() throws SaturnJobConsoleException {
		List<String> zkClusterList = new ArrayList<>();
		Collection<ZkCluster> tmp = registryCenterService.getZkClusterList();
		if (tmp != null) {
			Iterator<ZkCluster> iterator = tmp.iterator();
			while (iterator.hasNext()) {
				ZkCluster next = iterator.next();
				if (!next.isOffline()) {
					zkClusterList.add(next.getClusterKey());
				}
			}
		}
		return zkClusterList;
	}

	@Transactional(rollbackFor = { SaturnJobConsoleException.class })
	@Override
	public void moveNamespaceTo(String namespace, String zkClusterKeyNew, String lastUpdatedBy, boolean updateDBOnly)
			throws SaturnJobConsoleException {
		try {
			LOGGER.info("start move {} to {}", namespace, zkClusterKeyNew);
			if (updateDBOnly) {
				namespaceZkclusterMapping4SqlService.update(namespace, null, zkClusterKeyNew, lastUpdatedBy);
			} else {
				String zkClusterKey = namespaceZkclusterMapping4SqlService.getZkClusterKey(namespace);
				if (zkClusterKey != null && zkClusterKey.equals(zkClusterKeyNew)) {
					throw new SaturnJobConsoleException("The namespace(" + namespace + ") is in " + zkClusterKey); // see moveNamespaceBatchTo before modify
				}
				ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKeyNew);
				if (zkCluster == null) {
					throw new SaturnJobConsoleException("The " + zkClusterKeyNew + " is not exists");
				}
				if (zkCluster.isOffline()) {
					throw new SaturnJobConsoleException("The " + zkClusterKeyNew + " zkCluster is offline");
				}
				String zkAddr = zkCluster.getZkAddr();
				CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService.connectOnly(zkAddr, null);
				if (curatorFrameworkOp == null) {
					throw new SaturnJobConsoleException("The " + zkClusterKeyNew + " zkCluster is offline");
				}
				CuratorFramework curatorFramework = curatorFrameworkOp.getCuratorFramework();
				CuratorRepository.CuratorFrameworkOp curatorFrameworkOpByNamespace = registryCenterService.connectOnly(zkAddr, namespace);
				CuratorFramework curatorFrameworkByNamespace = curatorFrameworkOpByNamespace.getCuratorFramework();
				try {
					String namespaceNodePath = "/" + namespace;
					if (curatorFramework.checkExists().forPath(namespaceNodePath) != null) {
						curatorFramework.delete().deletingChildrenIfNeeded().forPath(namespaceNodePath);
					}
					String jobsNodePath = namespaceNodePath + JobNodePath.get$JobsNodePath();
					curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT)
							.forPath(jobsNodePath);

					List<CurrentJobConfig> configs = currentJobConfigService.findConfigsByNamespace(namespace);
					LOGGER.info("get configs success, {}", namespace);
					if (configs != null) {
						for (CurrentJobConfig jobConfig : configs) {
							jobOperationService.persistJobFromDB(jobConfig, curatorFrameworkOpByNamespace);
							LOGGER.info("move {}-{} to zk success", namespace, jobConfig.getJobName());
						}
					}
				} finally {
					curatorFramework.close();
					curatorFrameworkByNamespace.close();
				}
				LOGGER.info("move {} to zk {} success", namespace, zkClusterKeyNew);
				namespaceZkclusterMapping4SqlService.update(namespace, null, zkClusterKeyNew, lastUpdatedBy);
				LOGGER.info("update mapping table success, {}-{}", namespace, zkClusterKeyNew);
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		} finally {
			LOGGER.info("end move {} to {}", namespace, zkClusterKeyNew);
		}
	}

	@Override
	public void moveNamespaceBatchTo(final String namespaces, final String zkClusterKeyNew, final String lastUpdatedBy,
			final boolean updateDBOnly, final long id) throws SaturnJobConsoleException {
		final List<String> namespaceList = new ArrayList<>();
		String[] split = namespaces.split(",");
		if (split != null) {
			for (String tmp : split) {
				String namespace = tmp.trim();
				if (!namespace.isEmpty()) {
					namespaceList.add(namespace);
				}
			}
		}
		int size = namespaceList.size();
		moveNamespaceBatchStatusMap.put(id, new MoveNamespaceBatchStatus(size));
		moveNamespaceBatchThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				MoveNamespaceBatchStatus moveNamespaceBatchStatus = moveNamespaceBatchStatusMap.get(id);
				try {
					for (String namespace : namespaceList) {
						try {
							moveNamespaceBatchStatus.setMoving(namespace);
							moveNamespaceTo(namespace, zkClusterKeyNew, lastUpdatedBy, updateDBOnly);
							moveNamespaceBatchStatus.incrementSuccessCount();
						} catch (SaturnJobConsoleException e) {
							LOGGER.error(e.getMessage(), e);
							if (("The namespace(" + namespace + ") is in " + zkClusterKeyNew).equals(e.getMessage())) {
								moveNamespaceBatchStatus.incrementIgnoreCount();
								moveNamespaceBatchStatus.getIgnoreList().add(namespace);
							} else {
								moveNamespaceBatchStatus.incrementFailCount();
								moveNamespaceBatchStatus.getFailList().add(namespace);
							}
						} finally {
							moveNamespaceBatchStatus.decrementUnDoCount();
						}
					}
				} finally {
					moveNamespaceBatchStatus.setFinished(true);
				}
			}
		});
	}

	@Override
	public MoveNamespaceBatchStatus getMoveNamespaceBatchStatus(long id) throws SaturnJobConsoleException {
		MoveNamespaceBatchStatus moveNamespaceBatchStatus = moveNamespaceBatchStatusMap.get(id);
		if (moveNamespaceBatchStatus == null) {
			throw new SaturnJobConsoleException("query no MoveNamespaceBatchStatus");
		}
		if (moveNamespaceBatchStatus.isFinished()) {
			moveNamespaceBatchStatusMap.remove(id);
		} else {
			try {
				Thread.sleep(400L);
			} catch (InterruptedException e) {// NOSONAR
			}
		}
		return moveNamespaceBatchStatus;
	}

	@Override
	public void clearMoveNamespaceBatchStatus(long id) throws SaturnJobConsoleException {
		moveNamespaceBatchStatusMap.remove(id);
	}

}
