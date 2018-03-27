/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */
package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.NamespaceDomainInfo;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.NamespaceInfo;
import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.service.NamespaceInfoService;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMapping4SqlService;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.cache.DashboardLeaderHandler;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.service.helper.ZkClusterMappingUtils;
import com.vip.saturn.job.console.utils.*;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import com.vip.saturn.job.integrate.service.UpdateJobConfigService;
import com.vip.saturn.job.sharding.NamespaceShardingManager;
import com.vip.saturn.job.sharding.listener.AbstractConnectionListener;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegistryCenterServiceImpl implements RegistryCenterService {

	protected static final String DEFAULT_CONSOLE_CLUSTER_ID = "default";

	protected static final String NAMESPACE_CREATOR_NAME = "REST_API";

	protected static final String ERR_MSG_TEMPLATE_FAIL_TO_CREATE = "Fail to create new namespace {%s} for reason {%s}";

	protected static final String ERR_MSG_NS_NOT_FOUND = "The namespace does not exists.";

	protected static final String ERR_MSG_NS_ALREADY_EXIST = "Invalid request. Namespace: {%s} already existed";

	private static final Logger log = LoggerFactory.getLogger(RegistryCenterServiceImpl.class);

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private ReportAlarmService reportAlarmService;

	@Resource
	private UpdateJobConfigService updateJobConfigService;

	@Resource
	private ZkClusterInfoService zkClusterInfoService;

	@Resource
	private SystemConfigService systemConfigService;

	@Resource
	private NamespaceZkClusterMapping4SqlService namespaceZkClusterMapping4SqlService;

	@Resource
	private NamespaceInfoService namespaceInfoService;

	/**
	 * 为保证values有序
	 **/
	private LinkedHashMap<String, ZkCluster> zkClusterMap = new LinkedHashMap<>();

	private ConcurrentHashMap<String, DashboardLeaderHandler> dashboardLeaderTreeCacheMap = new ConcurrentHashMap<>();

	// namespace is unique in all zkClusters
	private ConcurrentHashMap<String /** nns */
			, RegistryCenterClient> registryCenterClientMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Object> nnsLock = new ConcurrentHashMap<>(); // maybe could remove in right time

	// namespace is unique in all zkClusters
	private ConcurrentHashMap<String /** nns **/
			, NamespaceShardingManager> namespaceShardingListenerManagerMap = new ConcurrentHashMap<>();

	private List<String> allOnlineNamespaces = new ArrayList<>();

	private String consoleClusterId;

	private Set<String> restrictComputeZkClusterKeys = Sets.newHashSet();

	private Timer localRefreshTimer = null;

	private Timer localRefreshIfNecessaryTimer = null;

	private ExecutorService localRefreshThreadPool = null;

	@PostConstruct
	public void init() {
		getConsoleClusterId();
		localRefresh();
		initLocalRefreshThreadPool();
		startLocalRefreshTimer();
		startLocalRefreshIfNecessaryTimer();
	}

	private void getConsoleClusterId() {
		if (StringUtils.isBlank(SaturnEnvProperties.VIP_SATURN_CONSOLE_CLUSTER_ID)) {
			log.info(
					"No environment variable or system property of [VIP_SATURN_CONSOLE_CLUSTER] is set. Use the default Id");
			consoleClusterId = DEFAULT_CONSOLE_CLUSTER_ID;
		} else {
			consoleClusterId = SaturnEnvProperties.VIP_SATURN_CONSOLE_CLUSTER_ID;
		}
	}

	@PreDestroy
	public void destroy() {
		Iterator<Entry<String, ZkCluster>> iterator = zkClusterMap.entrySet().iterator();
		while (iterator.hasNext()) {
			closeZkCluster(iterator.next().getValue());
		}
		if (localRefreshTimer != null) {
			localRefreshTimer.cancel();
		}
		if (localRefreshIfNecessaryTimer != null) {
			localRefreshIfNecessaryTimer.cancel();
		}
		if (localRefreshThreadPool != null) {
			localRefreshThreadPool.shutdownNow();
		}
	}

	private void initLocalRefreshThreadPool() {
		localRefreshThreadPool = Executors
				.newSingleThreadExecutor(new ConsoleThreadFactory("refresh-RegCenter-thread", false));
	}

	private void startLocalRefreshTimer() {
		localRefreshTimer = new Timer("refresh-RegCenter-timer", true);
		// 每隔5分钟刷新一次
		localRefreshTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					localRefreshThreadPool.submit(new Runnable() {
						@Override
						public void run() {
							try {
								localRefresh();
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							}
						}
					});
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}, 1000L * 60 * 5, 1000L * 60 * 5);
	}

	private void startLocalRefreshIfNecessaryTimer() {
		localRefreshIfNecessaryTimer = new Timer("refresh-RegCenter-if-necessary-timer", true);
		localRefreshIfNecessaryTimer.schedule(new TimerTask() {

			private String lastUuid = null;

			@Override
			public void run() {
				try {
					String uuid = systemConfigService
							.getValueDirectly(SystemConfigProperties.REFRESH_REGISTRY_CENTER_UUID);
					if (StringUtils.isBlank(uuid)) {
						notifyRefreshRegCenter();
					} else if (!uuid.equals(lastUuid)) {
						lastUuid = uuid;
						localRefreshThreadPool.submit(new Runnable() {
							@Override
							public void run() {
								try {
									localRefresh();
								} catch (Exception e) {
									log.error(e.getMessage(), e);
								}
							}
						});
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}, 1000, 1000);
	}

	private synchronized void localRefresh() {
		try {
			log.info("Start refresh RegCenter");
			refreshRestrictComputeZkClusters();
			if (restrictComputeZkClusterKeys.isEmpty()) {
				log.warn("根据Console的集群ID:" + consoleClusterId + ",找不到配置可以参与Sharding和Dashboard计算的zk集群");
				return;
			}
			refreshRegistryCenter();
			refreshDashboardLeaderTreeCache();
			refreshNamespaceShardingListenerManagerMap();
			long startTime = System.currentTimeMillis();
			log.info("End refresh RegCenter, cost {}ms", System.currentTimeMillis() - startTime);
		} catch (Exception e) {
			log.error("refresh RegCenter error", e);
		}
	}

	/**
	 * 解析Console集群和zk的映射关系 数据库中配置的例子如下： CONSOLE-1:/saturn,/forVdos;CONSOLE-2:/zk3; 如果不存在此配置项，则可以计算所有zk集群；
	 */
	private void refreshRestrictComputeZkClusters() throws SaturnJobConsoleException {
		// clear 当前可计算的zkCluster集群列表
		restrictComputeZkClusterKeys.clear();

		String allMappingStr = systemConfigService.getValueDirectly(SystemConfigProperties.CONSOLE_ZK_CLUSTER_MAPPING);
		if (StringUtils.isBlank(allMappingStr)) {
			log.info(
					"CONSOLE_ZK_CLUSTER_MAPPING is not configured in sys_config, so all zk clusters can be computed by this console");
			restrictComputeZkClusterKeys.addAll(getZkClusterKeys());
			return;
		}

		allMappingStr = StringUtils.deleteWhitespace(allMappingStr);
		String[] singleConsoleMappingArray = allMappingStr.split(";");
		for (String singleConsoleMappingStr : singleConsoleMappingArray) {
			String[] consoleAndClusterKeyArray = singleConsoleMappingStr.split(":");
			if (consoleAndClusterKeyArray.length != 2) {
				throw new SaturnJobConsoleException("the CONSOLE_ZK_CLUSTER_MAPPING(" + Arrays.toString(consoleAndClusterKeyArray)
						+ ") format is not correct, should be like console_cluster_id:zk_cluster_id");
			}
			String tempConsoleClusterId = consoleAndClusterKeyArray[0];
			String zkClusterKeyStr = consoleAndClusterKeyArray[1];
			if (consoleClusterId.equals(tempConsoleClusterId)) {
				String[] zkClusterKeyArray = zkClusterKeyStr.trim().split(",");
				restrictComputeZkClusterKeys.addAll(Arrays.asList(zkClusterKeyArray));
				log.info("the current console cluster:{} can do sharding and dashboard to zk clusters:{}",
						consoleClusterId, restrictComputeZkClusterKeys);
				return;
			}
		}
	}

	/**
	 * 判断该集群是否能被本Console计算
	 */
	private boolean isZKClusterCanBeComputed(String clusterKey) {
		if (CollectionUtils.isEmpty(restrictComputeZkClusterKeys)) {
			return false;
		}
		return restrictComputeZkClusterKeys.contains(clusterKey);
	}

	/**
	 * 判断是否同机房
	 */
	private boolean isCurrentConsoleInTheSameIdc(String clusterKey) {
		return ZkClusterMappingUtils.isCurrentConsoleInTheSameIdc(systemConfigService, clusterKey);
	}

	private String generateShardingLeadershipHostValue() {
		return LocalHostService.cachedIpAddress + "-" + UUID.randomUUID().toString();
	}

	/**
	 * 创建或者移除namespaceShardingManager.
	 */
	private void refreshNamespaceShardingListenerManagerMap() {
		Iterator<Entry<String, ZkCluster>> iterator = zkClusterMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, ZkCluster> next = iterator.next();
			ZkCluster zkCluster = next.getValue();
			ArrayList<RegistryCenterConfiguration> regCenterConfList = zkCluster.getRegCenterConfList();
			if (regCenterConfList == null) {
				continue;
			}

			for (RegistryCenterConfiguration conf : regCenterConfList) {
				String nns = conf.getNameAndNamespace();
				if (!namespaceShardingListenerManagerMap.containsKey(nns)) {
					if (isZKClusterCanBeComputed(conf.getZkClusterKey())) {
						createNamespaceShardingManager(conf, nns);
					}
				} else {
					NamespaceShardingManager namespaceShardingManager = namespaceShardingListenerManagerMap.get(nns);
					if (!isZKClusterCanBeComputed(conf.getZkClusterKey())) {
						namespaceShardingManager.stopWithCurator();
						namespaceShardingListenerManagerMap.remove(nns);
					}
				}
			}
		}
	}

	private void createNamespaceShardingManager(RegistryCenterConfiguration conf, String nns) {
		try {
			log.info("Start NamespaceShardingManager {}", nns);
			String namespace = conf.getNamespace();
			String digest = conf.getDigest();
			CuratorFramework client = curatorRepository.connect(conf.getZkAddressList(), namespace, digest);

			if (client == null) {
				log.warn("fail to connect to zk during create NamespaceShardingManager");
				return;
			}

			NamespaceShardingManager namespaceShardingManager = null;
			try {
				namespaceShardingManager = new NamespaceShardingManager(client, namespace,
						generateShardingLeadershipHostValue(), reportAlarmService, updateJobConfigService);
				namespaceShardingManager.start();
				if (namespaceShardingListenerManagerMap.putIfAbsent(nns, namespaceShardingManager) != null) {
					// 已经存在，则关闭当前的client
					try {
						namespaceShardingManager.stopWithCurator();
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				} else {
					log.info("Done starting NamespaceShardingManager {}", nns);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				if (namespaceShardingManager != null) {
					try {
						namespaceShardingManager.stop();
					} catch (Exception e2) {
						log.error(e2.getMessage(), e2);
					}
				}
				client.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void refreshRegistryCenter() {
		List<String> allOnlineNamespacesTemp = new ArrayList<>();
		// 获取新的zkClusters
		Map<String, ZkCluster> newClusterMap = getZkClusterInfo();

		// 对比旧的。不包含的，关闭操作；包含的，检查属性是否相同，如果相同，则直接赋值，否则，关闭旧的
		closeInvalidZkClient(newClusterMap);

		// 完善curatorFramework。如果没有，则新建
		connectToZkClusterIfPossible(newClusterMap);

		// 完善ZkCluster中的注册中心信息，关闭迁移了的域，新建迁移过来的域
		for (Map.Entry<String, ZkCluster> zkClusterEntry : newClusterMap.entrySet()) {

			ZkCluster zkCluster = zkClusterEntry.getValue();
			List<NamespaceZkClusterMapping> nsZkClusterMappingList = namespaceZkClusterMapping4SqlService
					.getAllMappingsOfCluster(zkClusterEntry.getKey());
			// zkCluster对应的namespace列表
			List<RegistryCenterConfiguration> regCenterConfList = zkCluster.getRegCenterConfList();

			closeMoveOutNamespace(zkClusterEntry.getKey(), nsZkClusterMappingList, regCenterConfList);

			initMoveInNamespace(allOnlineNamespacesTemp, zkClusterEntry.getKey(), zkCluster, nsZkClusterMappingList,
					regCenterConfList);
		}
		// 直接赋值新的
		zkClusterMap = (LinkedHashMap<String, ZkCluster>) newClusterMap;
		allOnlineNamespaces = allOnlineNamespacesTemp;
	}

	private void initMoveInNamespace(List<String> allOnlineNamespacesTemp, String zkClusterKey, ZkCluster zkCluster,
			List<NamespaceZkClusterMapping> nsZkClusterMappingList, List<RegistryCenterConfiguration> regCenterConfList) {
		if (nsZkClusterMappingList == null || zkCluster.isOffline()) {
			return;
		}

		for (NamespaceZkClusterMapping mapping : nsZkClusterMappingList) {
			String namespace = mapping.getNamespace();
			String name = StringUtils.deleteWhitespace(mapping.getName());
			if (SaturnSelfNodePath.ROOT_NAME.equals(namespace)) {
				log.error("The namespace cannot be {}", SaturnSelfNodePath.ROOT_NAME);
				continue;
			}
			boolean include = false;
			if (regCenterConfList != null) {
				for (RegistryCenterConfiguration conf : regCenterConfList) {
					if (!namespace.equals(conf.getNamespace())) {
						continue;
					}

					include = true;
					String nnsOld = conf.getNameAndNamespace();
					// update name
					conf.setName(name);
					conf.initNameAndNamespace();
					String nnsNew = conf.getNameAndNamespace();
					if (!nnsOld.equals(nnsNew)) {
						synchronized (getNnsLock(nnsOld)) {
							closeNamespace(nnsOld);
							log.info("closed the namespace info because it's nns is changed, namespace is {}", namespace);
						}
					}
					break;
				}
			}
			if (!include) {
				CuratorFramework curatorFramework = zkCluster.getCuratorFramework();
				initNamespaceZkNodeIfNecessary(namespace, curatorFramework);
				RegistryCenterConfiguration conf = new RegistryCenterConfiguration(name, namespace, zkCluster.getZkAddr());
				conf.setZkClusterKey(zkClusterKey);
				conf.setVersion(getVersion(namespace, curatorFramework));
				conf.setZkAlias(zkCluster.getZkAlias());
				NamespaceInfo namespaceInfo = getNamespaceInfo(namespace);
				if (namespaceInfo != null) {
					postConstructRegistryCenterConfiguration(conf, namespaceInfo.getContent());
				}
				zkCluster.getRegCenterConfList().add(conf);
			}
			if (!allOnlineNamespacesTemp.contains(namespace)) {
				allOnlineNamespacesTemp.add(namespace);
			}
		}
	}

	private NamespaceInfo getNamespaceInfo(String namespace) {
		try {
			return namespaceInfoService.selectByNamespace(namespace);
		} catch (Exception e) {
			log.error("fail to get namespaceInfo:{} from DB", namespace);
			return null;
		}
	}

	// For subclass override
	protected void postConstructRegistryCenterConfiguration(RegistryCenterConfiguration conf, String content) {
		// do nothing here
	}

	private void closeMoveOutNamespace(String zkClusterKey, List<NamespaceZkClusterMapping> nsZkClusterMappingList,
			List<RegistryCenterConfiguration> regCenterConfList) {
		if (regCenterConfList == null) {
			return;
		}

		Iterator<RegistryCenterConfiguration> regIter = regCenterConfList.iterator();
		while (regIter.hasNext()) {
			RegistryCenterConfiguration conf = regIter.next();
			String namespace = conf.getNamespace();
			String nns = conf.getNameAndNamespace();
			boolean include = false;
			if (nsZkClusterMappingList != null) {
				for (NamespaceZkClusterMapping mapping : nsZkClusterMappingList) {
					if (namespace.equals(mapping.getNamespace())) {
						include = true;
						break;
					}
				}
			}
			if (!include) {
				synchronized (getNnsLock(nns)) {
					regIter.remove();
					closeNamespace(nns);
					log.info("closed the moved namespace info, namespace is {}, old zkClusterKey is {}", namespace,
							zkClusterKey);
				}
			}
		}
	}

	private void closeInvalidZkClient(Map<String, ZkCluster> newClusterMap) {
		Iterator<Entry<String, ZkCluster>> iterator = zkClusterMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, ZkCluster> next = iterator.next();
			String zkClusterKey = next.getKey();
			ZkCluster zkCluster = next.getValue();
			if (!newClusterMap.containsKey(zkClusterKey)) {
				iterator.remove();
				closeZkCluster(zkCluster);
			} else {
				ZkCluster newZkCluster = newClusterMap.get(zkClusterKey);
				if (zkCluster.equals(newZkCluster)) {
					newClusterMap.put(zkClusterKey, zkCluster);
				} else {
					iterator.remove();
					closeZkCluster(zkCluster);
				}
			}
		}
	}

	private Map<String, ZkCluster> getZkClusterInfo() {
		LinkedHashMap<String, ZkCluster> newClusterMap = new LinkedHashMap<>();
		List<ZkClusterInfo> allZkClusterInfoList = zkClusterInfoService.getAllZkClusterInfo();
		if (allZkClusterInfoList != null) {
			for (ZkClusterInfo zkClusterInfo : allZkClusterInfoList) {
				ZkCluster zkCluster = new ZkCluster();
				zkCluster.setZkClusterKey(zkClusterInfo.getZkClusterKey());
				zkCluster.setZkAlias(zkClusterInfo.getAlias());
				zkCluster.setZkAddr(zkClusterInfo.getConnectString());
				newClusterMap.put(zkClusterInfo.getZkClusterKey(), zkCluster);
			}
		}

		return newClusterMap;
	}

	private void connectToZkClusterIfPossible(Map<String, ZkCluster> newClusterMap) {
		Iterator<Entry<String, ZkCluster>> iterator = newClusterMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, ZkCluster> next = iterator.next();
			ZkCluster zkCluster = next.getValue();
			CuratorFramework curatorFramework = zkCluster.getCuratorFramework();
			if (curatorFramework == null) {
				createNewConnect(zkCluster);
			}
		}
	}

	private Object getNnsLock(String nns) {
		Object lock = nnsLock.get(nns);
		if (lock == null) {
			lock = new Object();
			Object pre = nnsLock.putIfAbsent(nns, lock);
			if (pre != null) {
				lock = pre;
			}
		}
		return lock;
	}

	private void closeNamespace(String nns) {
		try {
			RegistryCenterClient registryCenterClient = registryCenterClientMap.remove(nns);
			if (registryCenterClient != null) {
				registryCenterClient.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		try {
			NamespaceShardingManager namespaceShardingManager = namespaceShardingListenerManagerMap.remove(nns);
			if (namespaceShardingManager != null) {
				namespaceShardingManager.stopWithCurator();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void refreshDashboardLeaderTreeCache() {
		closeDeprecatedDashboardLeaderTreeCache();
		Iterator<Entry<String, ZkCluster>> iterator = zkClusterMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, ZkCluster> next = iterator.next();
			String zkClusterKey = next.getKey();
			ZkCluster zkCluster = next.getValue();
			if (needToRefreshDashboardTreeCache(zkCluster, zkClusterKey)) {
				DashboardLeaderHandler dashboardLeaderHandler = null;
				try {
					dashboardLeaderHandler = new DashboardLeaderHandler(zkCluster.getZkAlias(),
							zkCluster.getCuratorFramework());
					dashboardLeaderHandler.start();
					dashboardLeaderTreeCacheMap.put(zkClusterKey, dashboardLeaderHandler);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					if (dashboardLeaderHandler != null) {
						dashboardLeaderHandler.shutdown();
					}
				}
			}
		}
	}

	private boolean needToRefreshDashboardTreeCache(ZkCluster zkCluster, String zkClusterKey) {
		if (zkCluster.isOffline()) {
			return false;
		}

		if (dashboardLeaderTreeCacheMap.containsKey(zkClusterKey)) {
			return false;
		}

		return isZKClusterCanBeComputed(zkClusterKey) && isCurrentConsoleInTheSameIdc(zkClusterKey);
	}

	/**
	 * 将不在本console服务器中进行Dashboard计算的DashboardLeaderTreeCache关闭
	 */
	private void closeDeprecatedDashboardLeaderTreeCache() {
		if (dashboardLeaderTreeCacheMap == null || dashboardLeaderTreeCacheMap.isEmpty()) {
			return;
		}
		for (String zkClusterKey : dashboardLeaderTreeCacheMap.keySet()) {
			if (!isZKClusterCanBeComputed(zkClusterKey) || !isCurrentConsoleInTheSameIdc(zkClusterKey)) {
				log.info("close the deprecated dashboard leader tree Cache, {}", zkClusterKey);
				DashboardLeaderHandler oldDashboardLeaderHandler = dashboardLeaderTreeCacheMap.remove(zkClusterKey);
				if (oldDashboardLeaderHandler != null) {
					oldDashboardLeaderHandler.shutdown();
				}
			}
		}
	}

	/**
	 * Close dashboardLeaderTreeCache, registryCenterClient, namespaceShardingListenerManager with this zkCluster
	 */
	private void closeZkCluster(ZkCluster zkCluster) {
		try {
			try {
				DashboardLeaderHandler dashboardLeaderHandler = dashboardLeaderTreeCacheMap
						.remove(zkCluster.getZkClusterKey());
				if (dashboardLeaderHandler != null) {
					dashboardLeaderHandler.shutdown();
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			ArrayList<RegistryCenterConfiguration> regCenterConfList = zkCluster.getRegCenterConfList();
			if (regCenterConfList != null) {
				for (RegistryCenterConfiguration conf : regCenterConfList) {
					String nns = conf.getNameAndNamespace();
					synchronized (getNnsLock(nns)) {
						closeNamespace(nns);
					}
				}
			}
			if (zkCluster.getConnectionListener() != null) {
				zkCluster.getConnectionListener().shutdownNowUntilTerminated();
				zkCluster.setConnectionListener(null);
			}
			if (zkCluster.getCuratorFramework() != null) {
				zkCluster.getCuratorFramework().close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void createNewConnect(final ZkCluster zkCluster) {
		String zkAddr = zkCluster.getZkAddr();
		try {
			final CuratorFramework curatorFramework = curatorRepository.connect(zkAddr, null, zkCluster.getDigest());
			if (curatorFramework == null) {
				log.error("found an offline zkCluster, zkAddr is {}", zkAddr);
				zkCluster.setCuratorFramework(null);
				zkCluster.setConnectionListener(null);
				zkCluster.setOffline(true);
			} else {
				AbstractConnectionListener connectionListener = new AbstractConnectionListener(
						"zk-connectionListener-thread-for-zkCluster-" + zkCluster.getZkAlias()) {
					@Override
					public void stop() {
						zkCluster.setOffline(true);
					}

					@Override
					public void restart() {
						try {
							zkCluster.setOffline(false);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
				};
				zkCluster.setCuratorFramework(curatorFramework);
				zkCluster.setConnectionListener(connectionListener);
				zkCluster.setOffline(false);
				curatorFramework.getConnectionStateListenable().addListener(connectionListener);
			}
		} catch (Exception e) {
			log.error("found an offline zkCluster, zkAddr is {}", zkAddr);
			log.error(e.getMessage(), e);
			zkCluster.setCuratorFramework(null);
			zkCluster.setConnectionListener(null);
			zkCluster.setOffline(true);
		}
	}

	private void initNamespaceZkNodeIfNecessary(String namespace, CuratorFramework curatorFramework) {
		try {
			String executorsNodePath = "/" + namespace + ExecutorNodePath.get$ExecutorNodePath();
			if (curatorFramework.checkExists().forPath(executorsNodePath) == null) {
				curatorFramework.create().creatingParentsIfNeeded().forPath(executorsNodePath);
			}
			String jobsNodePath = "/" + namespace + JobNodePath.get$JobsNodePath();
			if (curatorFramework.checkExists().forPath(jobsNodePath) == null) {
				curatorFramework.create().creatingParentsIfNeeded().forPath(jobsNodePath);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private String getVersion(String namespace, CuratorFramework curatorFramework) {
		try {
			List<String> versionList = new ArrayList<>();
			String executorsPath = "/" + namespace + ExecutorNodePath.getExecutorNodePath();

			if (curatorFramework.checkExists().forPath(executorsPath) == null) {
				return getVersionStrFromList(versionList);
			}

			List<String> executors = curatorFramework.getChildren().forPath(executorsPath);
			if (executors == null || executors.isEmpty()) {
				return getVersionStrFromList(versionList);
			}

			for (String exe : executors) {
				String versionPath = executorsPath + "/" + exe + "/version";
				if (curatorFramework.checkExists().forPath(versionPath) == null) {
					continue;
				}

				byte[] bs = curatorFramework.getData().forPath(versionPath);
				if (bs == null) {
					continue;
				}

				String version = new String(bs, "UTF-8");
				if (!version.trim().isEmpty()) {
					String tmp = version.trim();
					if (!versionList.contains(tmp)) {
						versionList.add(tmp);
					}
				}
			}

			return getVersionStrFromList(versionList);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "";
		}
	}

	private String getVersionStrFromList(List<String> versionList) {
		Collections.sort(versionList);
		StringBuilder versionSb = new StringBuilder();
		versionSb.append("");
		for (int i = 0; i < versionList.size(); i++) {
			versionSb.append(versionList.get(i));
			if (i < versionList.size() - 1) {
				versionSb.append(", ");
			}
		}
		return versionSb.toString();
	}

	@Override
	public RegistryCenterClient connect(final String nameAndNameSpace) {
		final RegistryCenterClient registryCenterClient = new RegistryCenterClient();
		registryCenterClient.setNameAndNamespace(nameAndNameSpace);
		if (nameAndNameSpace == null) {
			return registryCenterClient;
		}
		synchronized (getNnsLock(nameAndNameSpace)) {
			if (!registryCenterClientMap.containsKey(nameAndNameSpace)) {
				RegistryCenterConfiguration registryCenterConfiguration = findConfig(nameAndNameSpace);
				if (registryCenterConfiguration == null) {
					return registryCenterClient;
				}
				String zkAddressList = registryCenterConfiguration.getZkAddressList();
				String namespace = registryCenterConfiguration.getNamespace();
				String digest = registryCenterConfiguration.getDigest();
				registryCenterClient.setZkAddr(zkAddressList);

				CuratorFramework client = curatorRepository.connect(zkAddressList, namespace, digest);
				if (client == null) {
					return registryCenterClient;
				}
				registryCenterClient.setConnected(client.getZookeeperClient().isConnected());
				registryCenterClient.setCuratorClient(client);
				registryCenterClientMap.put(nameAndNameSpace, registryCenterClient);
				return registryCenterClient;
			} else {
				RegistryCenterClient registryCenterClient2 = registryCenterClientMap.get(nameAndNameSpace);
				if (registryCenterClient2 != null) {
					if (registryCenterClient2.getCuratorClient() != null) {
						registryCenterClient2.setConnected(
								registryCenterClient2.getCuratorClient().getZookeeperClient().isConnected());
					} else {
						registryCenterClient2.setConnected(false);
					}
					return registryCenterClient2;
				}
				return registryCenterClient;
			}
		}
	}

	@Override
	public RegistryCenterClient connectByNamespace(String namespace) {
		RegistryCenterConfiguration registryCenterConfiguration = findConfigByNamespace(namespace);
		if (registryCenterConfiguration == null) {
			return new RegistryCenterClient();
		}
		String nns = registryCenterConfiguration.getNameAndNamespace();
		if (nns == null) {
			return new RegistryCenterClient();
		}
		String zkAddressList = registryCenterConfiguration.getZkAddressList();
		String digest = registryCenterConfiguration.getDigest();
		synchronized (getNnsLock(nns)) {
			if (!registryCenterClientMap.containsKey(nns)) {
				final RegistryCenterClient registryCenterClient = new RegistryCenterClient();
				registryCenterClient.setNameAndNamespace(nns);
				registryCenterClient.setZkAddr(zkAddressList);
				CuratorFramework client = curatorRepository.connect(zkAddressList, namespace, digest);
				if (client == null) {
					return registryCenterClient;
				}
				registryCenterClient.setConnected(client.getZookeeperClient().isConnected());
				registryCenterClient.setCuratorClient(client);
				registryCenterClientMap.put(nns, registryCenterClient);
				return registryCenterClient;
			} else {
				RegistryCenterClient registryCenterClient = registryCenterClientMap.get(nns);
				if (registryCenterClient == null) {
					registryCenterClient = new RegistryCenterClient();
					registryCenterClient.setNameAndNamespace(namespace);
					registryCenterClient.setZkAddr(zkAddressList);
				} else {
					if (registryCenterClient.getCuratorClient() != null) {
						registryCenterClient.setConnected(
								registryCenterClient.getCuratorClient().getZookeeperClient().isConnected());
					} else {
						registryCenterClient.setConnected(false);
					}
				}
				return registryCenterClient;
			}
		}
	}

	@Override
	public RegistryCenterConfiguration findConfig(String nameAndNamespace) {
		if (Strings.isNullOrEmpty(nameAndNamespace)) {
			return null;
		}
		Collection<ZkCluster> zkClusters = zkClusterMap.values();
		for (ZkCluster zkCluster : zkClusters) {
			for (RegistryCenterConfiguration each : zkCluster.getRegCenterConfList()) {
				if (each != null && nameAndNamespace.equals(each.getNameAndNamespace())) {
					return each;
				}
			}
		}
		return null;
	}

	@Override
	public RegistryCenterConfiguration findConfigByNamespace(String namespace) {
		if (Strings.isNullOrEmpty(namespace)) {
			return null;
		}
		Collection<ZkCluster> zkClusters = zkClusterMap.values();
		for (ZkCluster zkCluster : zkClusters) {
			for (RegistryCenterConfiguration each : zkCluster.getRegCenterConfList()) {
				if (each != null && namespace.equals(each.getNamespace())) {
					return each;
				}
			}
		}
		return null;
	}

	@Override
	public CuratorRepository.CuratorFrameworkOp connectOnly(String zkAddr, String namespace)
			throws SaturnJobConsoleException {
		CuratorFramework curatorFramework = curatorRepository.connect(zkAddr, namespace, null);
		if (curatorFramework != null) {
			return curatorRepository.newCuratorFrameworkOp(curatorFramework);
		}
		return null;
	}

	@Override
	public void notifyRefreshRegCenter() throws SaturnJobConsoleException {
		SystemConfig systemConfig = new SystemConfig();
		systemConfig.setProperty(SystemConfigProperties.REFRESH_REGISTRY_CENTER_UUID);
		systemConfig.setValue(UUID.randomUUID().toString());
		systemConfigService.insertOrUpdate(systemConfig);
	}

	@Override
	public RegistryCenterClient getCuratorByNameAndNamespace(String nameAndNamespace) {
		return registryCenterClientMap.get(nameAndNamespace);
	}

	@Override
	public boolean isDashboardLeader(String zkClusterKey) {
		DashboardLeaderHandler dashboardLeaderHandler = dashboardLeaderTreeCacheMap.get(zkClusterKey);
		if (dashboardLeaderHandler != null) {
			return dashboardLeaderHandler.isLeader();
		}
		return false;
	}

	@Override
	public ZkCluster getZkCluster(String zkClusterKey) {
		return zkClusterMap.get(zkClusterKey);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void createZkCluster(String zkClusterKey, String alias, String connectString)
			throws SaturnJobConsoleException {
		zkClusterInfoService.createZkCluster(zkClusterKey, alias, connectString, "");
		notifyRefreshRegCenter();
	}

	@Override
	public Collection<ZkCluster> getZkClusterList() {
		return zkClusterMap.values();
	}

	@Override
	public List<ZkCluster> getOnlineZkClusterList() {
		List<ZkCluster> onlineClusters = Lists.newArrayList();
		for (ZkCluster zkCluster : zkClusterMap.values()) {
			if (!zkCluster.isOffline()) {
				onlineClusters.add(zkCluster);
			}
		}
		return onlineClusters;
	}

	private List<String> getZkClusterKeys() {
		Collection<ZkCluster> zkClusters = getZkClusterList();
		List<String> zkClusterKeys = Lists.newArrayList();
		for (ZkCluster zkCluster : zkClusters) {
			zkClusterKeys.add(zkCluster.getZkClusterKey());
		}
		return zkClusterKeys;
	}

	@Override
	public int domainCount(String zkClusterKey) {
		ZkCluster zkCluster = zkClusterMap.get(zkClusterKey);
		if (zkCluster != null) {
			ArrayList<RegistryCenterConfiguration> regList = zkCluster.getRegCenterConfList();
			if (regList != null) {
				return regList.size();
			}
		}
		return 0;
	}

	@Override
	public boolean namespaceIsCorrect(String namespace, CuratorFramework curatorFramework)
			throws SaturnJobConsoleException {
		if (SaturnSelfNodePath.ROOT_NAME.equals(namespace)) {
			return false;
		}
		try {
			String executorsPath = "/" + namespace + ExecutorNodePath.getExecutorNodePath();
			if (curatorFramework.checkExists().forPath(executorsPath) != null) {
				return true;
			}
			String jobsPath = "/" + namespace + JobNodePath.get$JobsNodePath();
			return curatorFramework.checkExists().forPath(jobsPath) != null ? true : false;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public List<String> getNamespaces() throws SaturnJobConsoleException {
		return allOnlineNamespaces;
	}

	@Override
	public File exportNamespaceInfo(List<String> namespaceList) throws SaturnJobConsoleException {
		Set<String> targetNamespaceSet = null;
		if (namespaceList != null) {
			targetNamespaceSet = Sets.newHashSet(namespaceList);
		}

		List<RegistryCenterConfiguration> namespaceInfoList = Lists.newLinkedList();
		Collection<ZkCluster> zkClusterList = getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			List<RegistryCenterConfiguration> namespacesOfZkCluster = zkCluster.getRegCenterConfList();
			for (RegistryCenterConfiguration ns : namespacesOfZkCluster) {
				if (targetNamespaceSet == null || targetNamespaceSet.contains(ns.getNamespace())) {
					namespaceInfoList.add(ns);
				}
			}
		}

		return exportNamespaceInfo2Excel(namespaceInfoList);
	}

	/**
	 * Export namespac
	 */
	private File exportNamespaceInfo2Excel(List<RegistryCenterConfiguration> namespaceInfoList)
			throws SaturnJobConsoleException {
		try {
			File tmpFile = SaturnConsoleUtils.createTmpFile();
			WritableWorkbook writableWorkbook = Workbook.createWorkbook(tmpFile);
			WritableSheet sheet1 = writableWorkbook.createSheet("ns", 0);
			sheet1.addCell(new Label(0, 0, "域名"));
			sheet1.addCell(new Label(1, 0, "描述"));
			sheet1.addCell(new Label(2, 0, "重要等级"));
			sheet1.addCell(new Label(3, 0, "Executor版本"));
			sheet1.addCell(new Label(4, 0, "ZK集群"));

			for (int i = 0; i < namespaceInfoList.size(); i++) {
				RegistryCenterConfiguration namespaceInfo = namespaceInfoList.get(i);
				sheet1.addCell(new Label(0, i + 1, namespaceInfo.getNamespace()));
				sheet1.addCell(new Label(1, i + 1, namespaceInfo.getName()));
				sheet1.addCell(new Label(2, i + 1, namespaceInfo.getDegree()));
				sheet1.addCell(new Label(3, i + 1, namespaceInfo.getVersion()));
				sheet1.addCell(new Label(4, i + 1, namespaceInfo.getZkAlias()));
			}

			writableWorkbook.write();
			writableWorkbook.close();

			return tmpFile;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public void createNamespace(NamespaceDomainInfo namespaceDomainInfo) throws SaturnJobConsoleException {
		String namespace = namespaceDomainInfo.getNamespace();
		String zkClusterKey = namespaceDomainInfo.getZkCluster();
		ZkCluster currentCluster = getZkCluster(zkClusterKey);

		if (currentCluster == null) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(ERR_MSG_TEMPLATE_FAIL_TO_CREATE, namespace, "not found zkcluster" + zkClusterKey));
		}

		if (checkNamespaceExists(namespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(ERR_MSG_NS_ALREADY_EXIST, namespace));
		}

		try {
			// 创建 namespaceInfo
			NamespaceInfo namespaceInfo = constructNamespaceInfo(namespaceDomainInfo);
			namespaceInfoService.create(namespaceInfo);
			// 创建 zkcluster 和 namespaceInfo 关系
			namespaceZkClusterMapping4SqlService.insert(namespace, "", zkClusterKey, NAMESPACE_CREATOR_NAME);
			// refresh
			notifyRefreshRegCenter();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					String.format(ERR_MSG_TEMPLATE_FAIL_TO_CREATE, namespace, e.getMessage()));
		}
	}

	@Override
	public void updateNamespace(NamespaceDomainInfo namespaceDomainInfo) throws SaturnJobConsoleException {
		String namespace = namespaceDomainInfo.getNamespace();

		if (!checkNamespaceExists(namespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), ERR_MSG_NS_NOT_FOUND);
		}

		try {
			// 创建 namespaceInfo
			NamespaceInfo namespaceInfo = constructNamespaceInfo(namespaceDomainInfo);
			namespaceInfoService.update(namespaceInfo);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					String.format(ERR_MSG_TEMPLATE_FAIL_TO_CREATE, namespace, e.getMessage()));
		}
	}

	@Override
	public NamespaceDomainInfo getNamespace(String namespace) throws SaturnJobConsoleException {
		if (namespaceInfoService.selectByNamespace(namespace) == null) {
			throw new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(), ERR_MSG_NS_NOT_FOUND);
		}

		String zkClusterKey = namespaceZkClusterMapping4SqlService.getZkClusterKey(namespace);
		if (StringUtils.isBlank(zkClusterKey)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(), ERR_MSG_NS_NOT_FOUND);
		}

		NamespaceDomainInfo namespaceDomainInfo = new NamespaceDomainInfo();
		namespaceDomainInfo.setNamespace(namespace);
		namespaceDomainInfo.setZkCluster(zkClusterKey);

		return namespaceDomainInfo;
	}

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public void bindNamespaceAndZkCluster(String namespace, String zkClusterKey, String updatedBy) throws SaturnJobConsoleException {
		ZkCluster currentCluster = getZkCluster(zkClusterKey);

		if (currentCluster == null) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(ERR_MSG_TEMPLATE_FAIL_TO_CREATE, namespace, "not found zkcluster" + zkClusterKey));
		}

		// namespace必须要存在
		if (!checkNamespaceExists(namespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), ERR_MSG_NS_NOT_FOUND);
		}

		// 判断其它集群是否有该域
		String zkClusterKeyOther = namespaceZkClusterMapping4SqlService.getZkClusterKey(namespace);
		if (zkClusterKeyOther != null) {
			ZkCluster zkClusterOther = getZkCluster(zkClusterKeyOther);
			if (zkClusterOther == null) {
				throw new SaturnJobConsoleException("zk cluster 不存在：" + zkClusterKeyOther);
			}

			if (zkClusterOther.getZkClusterKey().equals(zkClusterKey)) {
				throw new SaturnJobConsoleException("Namespace已经存在于此zk集群，不能重复添加");
			} else {
				throw new SaturnJobConsoleException(
						"Namespace存在于另外的zk集群：" + zkClusterOther.getZkClusterKey() + "，不能重复添加");
			}
		}

		try {
			namespaceZkClusterMapping4SqlService.insert(namespace, "", zkClusterKey, updatedBy);
			postBindNamespaceAndZkCluster(namespace, currentCluster);
			// refresh
			notifyRefreshRegCenter();
		} catch (Exception e) {
			namespaceZkClusterMapping4SqlService.remove(namespace, updatedBy);
			throw new SaturnJobConsoleException(e.getMessage());
		}
	}

	protected void postBindNamespaceAndZkCluster(String namespace, ZkCluster currentCluster) throws SaturnJobConsoleException {
		// for subclass implement
	}

	private boolean checkNamespaceExists(String namespace) {
		if (namespaceInfoService.selectByNamespace(namespace) != null) {
			return true;
		}

		// 判断其它集群是否有该域
		String zkClusterKeyOther = namespaceZkClusterMapping4SqlService.getZkClusterKey(namespace);
		return zkClusterKeyOther != null ? true : false;
	}

	private NamespaceInfo constructNamespaceInfo(NamespaceDomainInfo namespaceDomainInfo) {
		NamespaceInfo namespaceInfo = new NamespaceInfo();
		namespaceInfo.setCreatedBy(NAMESPACE_CREATOR_NAME);
		namespaceInfo.setCreateTime(new Date());
		namespaceInfo.setIsDeleted(0);
		namespaceInfo.setLastUpdatedBy(NAMESPACE_CREATOR_NAME);
		namespaceInfo.setLastUpdateTime(new Date());
		namespaceInfo.setNamespace(namespaceDomainInfo.getNamespace());
		namespaceInfo.setContent(namespaceDomainInfo.getContent());

		return namespaceInfo;
	}

	@Override
	public CuratorRepository.CuratorFrameworkOp getCuratorFrameworkOp(String namespace)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = null;
		try {
			RegistryCenterConfiguration registryCenterConfiguration = findConfigByNamespace(namespace);
			if (registryCenterConfiguration == null) {
				throw new SaturnJobConsoleException("Connect zookeeper failed");
			}

			String nns = registryCenterConfiguration.getNameAndNamespace();
			if (nns == null) {
				throw new SaturnJobConsoleException("Connect zookeeper failed");
			}

			String zkAddressList = registryCenterConfiguration.getZkAddressList();
			String digest = registryCenterConfiguration.getDigest();
			synchronized (getNnsLock(nns)) {
				if (!registryCenterClientMap.containsKey(nns)) {
					final RegistryCenterClient registryCenterClient = new RegistryCenterClient();
					registryCenterClient.setNameAndNamespace(nns);
					registryCenterClient.setZkAddr(zkAddressList);
					CuratorFramework curatorFramework = curatorRepository.connect(zkAddressList, namespace, digest);
					if (curatorFramework != null) {
						registryCenterClient.setConnected(curatorFramework.getZookeeperClient().isConnected());
						registryCenterClient.setCuratorClient(curatorFramework);
						registryCenterClientMap.put(nns, registryCenterClient);
						curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorFramework);
					}
				} else {
					RegistryCenterClient registryCenterClient = registryCenterClientMap.get(nns);
					if (registryCenterClient != null) {
						CuratorFramework curatorFramework = registryCenterClient.getCuratorClient();
						if (curatorFramework != null) {
							registryCenterClient.setConnected(curatorFramework.getZookeeperClient().isConnected());
							curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorFramework);
						}
					}
				}
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
		if (curatorFrameworkOp == null) {
			throw new SaturnJobConsoleException("Connect zookeeper failed");
		}
		return curatorFrameworkOp;
	}

}
