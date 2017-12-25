/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.TreeNode;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;
import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMapping4SqlService;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.InitRegistryCenterService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.DashboardLeaderTreeCache;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.service.helper.ZkClusterMappingUtils;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.LocalHostService;
import com.vip.saturn.job.console.utils.SaturnSelfNodePath;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import com.vip.saturn.job.integrate.service.UpdateJobConfigService;
import com.vip.saturn.job.sharding.NamespaceShardingManager;
import com.vip.saturn.job.sharding.listener.AbstractConnectionListener;

@Service
public class RegistryCenterServiceImpl implements RegistryCenterService {

	private static final Logger log = LoggerFactory.getLogger(RegistryCenterServiceImpl.class);

	private static final String DEFAULT_CONSOLE_CLUSTER_ID = "default";

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

	private final AtomicBoolean refreshingRegCenter = new AtomicBoolean(false);

	/** 为保证values有序 **/
	private LinkedHashMap<String, ZkCluster> zkClusterMap = new LinkedHashMap<>();

	private ConcurrentHashMap<String, DashboardLeaderTreeCache> dashboardLeaderTreeCacheMap = new ConcurrentHashMap<>();

	// namespace is unique in all zkClusters
	private ConcurrentHashMap<String /** nns */
			, RegistryCenterClient> registryCenterClientMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Object> nnsLock = new ConcurrentHashMap<>(); // maybe could remove in right time

	// namespace is unique in all zkClusters
	private ConcurrentHashMap<String /** nns **/
			, NamespaceShardingManager> namespaceShardingListenerManagerMap = new ConcurrentHashMap<>();
	
	private String consoleClusterId;

	private List<String> restrictComputeZkClusterKeys = new ArrayList<String>();

	private Timer refreshAllTimer = null;

	@PostConstruct
	public void init() throws Exception {
		if (StringUtils.isBlank(SaturnEnvProperties.VIP_SATURN_CONSOLE_CLUSTER_ID)) {
			String warnMsg = "No environment variable or system property of [VIP_SATURN_CONSOLE_CLUSTER] is set! Use the default Id";
			log.warn(warnMsg);
			consoleClusterId = DEFAULT_CONSOLE_CLUSTER_ID;
		} else {
			consoleClusterId = SaturnEnvProperties.VIP_SATURN_CONSOLE_CLUSTER_ID;
		}
		refreshRegCenter();
		startRefreshAllTimer();
	}

	@PreDestroy
	public void destroy() {
		Iterator<Entry<String, ZkCluster>> iterator = zkClusterMap.entrySet().iterator();
		while (iterator.hasNext()) {
			closeZkCluster(iterator.next().getValue());
		}
		if (refreshAllTimer != null) {
			refreshAllTimer.cancel();
		}
	}

	private void startRefreshAllTimer() {
		refreshAllTimer = new Timer("refresh-RegCenter-timer", true);
		// 每隔5分钟刷新一次
		refreshAllTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					refreshRegCenter();
				} catch (Throwable t) {
					log.error("refresh regCenter error", t);
				}
			}
		}, 1000 * 60 * 5, 1000 * 60 * 5);
	}

	private void refreshAll() throws Exception {
		refreshRestrictComputeZkClusters();
		refreshRegistryCenter();
		refreshDashboardLeaderTreeCache();
		refreshNamespaceShardingListenerManagerMap();
		refreshTreeData();
	}

	/**
	 * 解析Console集群和zk的映射关系
	 * 
	 * 数据库中配置的例子如下： CONSOLE-1:/saturn,/forVdos;CONSOLE-2:/zk3;
	 * 
	 * @throws SaturnJobConsoleException
	 */
	private void refreshRestrictComputeZkClusters() throws SaturnJobConsoleException {
		String allMappingStr = systemConfigService.getValueDirectly(SystemConfigProperties.CONSOLE_ZK_CLUSTER_MAPPING);
		if (StringUtils.isBlank(allMappingStr)) {
			throw new SaturnJobConsoleException("the CONSOLE_ZK_CLUSTER_MAPPING is not configured in sys_config");
		}

		allMappingStr = StringUtils.deleteWhitespace(allMappingStr);
		String[] singleConsoleMappingArray = allMappingStr.split(";");
		for (String singleConsoleMappingStr : singleConsoleMappingArray) {
			String[] consoleAndClusterKeyArray = singleConsoleMappingStr.split(":");
			if (consoleAndClusterKeyArray.length != 2) {
				throw new SaturnJobConsoleException("the CONSOLE_ZK_CLUSTER_MAPPING(" + consoleAndClusterKeyArray
						+ ") format is not correct, should be like console:zk1");
			}
			String tempConsoleClusterId = consoleAndClusterKeyArray[0];
			String zkClusterKeyStr = consoleAndClusterKeyArray[1];
			if (consoleClusterId.equals(tempConsoleClusterId)) {
				String[] zkClusterKeyArray = zkClusterKeyStr.trim().split(",");
				restrictComputeZkClusterKeys = Arrays.asList(zkClusterKeyArray);
				log.info("the current console {} can do sharding and dashboard to {}", consoleClusterId,
						restrictComputeZkClusterKeys);
				return;
			}
		}

		throw new SaturnJobConsoleException(
				"the console " + consoleClusterId + " cannot do sharding and dashboard to any cluster");
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

	private void refreshNamespaceShardingListenerManagerMap() {
		Iterator<Entry<String, ZkCluster>> iterator = zkClusterMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, ZkCluster> next = iterator.next();
			ZkCluster zkCluster = next.getValue();
			ArrayList<RegistryCenterConfiguration> regCenterConfList = zkCluster.getRegCenterConfList();
			if (regCenterConfList != null) {
				for (RegistryCenterConfiguration conf : regCenterConfList) {
					String nns = conf.getNameAndNamespace();
					if (!namespaceShardingListenerManagerMap.containsKey(nns)) {
						if (isZKClusterCanBeComputed(conf.getZkClusterKey())) {
							createNamespaceShardingManager(conf, nns);
						}
					} else {
						NamespaceShardingManager namespaceShardingManager = namespaceShardingListenerManagerMap
								.get(nns);
						if (!isZKClusterCanBeComputed(conf.getZkClusterKey())) {
							namespaceShardingManager.stopWithCurator();
							namespaceShardingListenerManagerMap.remove(nns);
						}
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
			if (client != null) {
				NamespaceShardingManager namespaceShardingManager = null;
				try {
					namespaceShardingManager = new NamespaceShardingManager(client, namespace,
							generateShardingLeadershipHostValue(), reportAlarmService, updateJobConfigService);
					namespaceShardingManager.start();
					if (namespaceShardingListenerManagerMap.putIfAbsent(nns, namespaceShardingManager) != null) {
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
							log.error(e.getMessage(), e);
						}
					}
					client.close();
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void refreshRegistryCenter() throws IOException {
		LinkedHashMap<String, ZkCluster> newClusterMap = new LinkedHashMap<>();
		// 获取新的zkClusters
		List<ZkClusterInfo> allZkClusterInfo = zkClusterInfoService.getAllZkClusterInfo();
		if (allZkClusterInfo != null) {
			for (ZkClusterInfo zkClusterInfo : allZkClusterInfo) {
				ZkCluster zkCluster = new ZkCluster();
				zkCluster.setZkClusterKey(zkClusterInfo.getZkClusterKey());
				zkCluster.setZkAlias(zkClusterInfo.getAlias());
				zkCluster.setZkAddr(zkClusterInfo.getConnectString());
				newClusterMap.put(zkClusterInfo.getZkClusterKey(), zkCluster);
			}
		}

		// 对比旧的。不包含的，关闭操作；包含的，检查属性是否相同，如果相同，则直接赋值，否则，关闭旧的
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
		// 完善curatorFramework。如果没有，则新建
		Iterator<Entry<String, ZkCluster>> iterator2 = newClusterMap.entrySet().iterator();
		while (iterator2.hasNext()) {
			Entry<String, ZkCluster> next = iterator2.next();
			ZkCluster zkCluster = next.getValue();
			CuratorFramework curatorFramework = zkCluster.getCuratorFramework();
			if (curatorFramework == null) {
				createNewConnect(zkCluster);
			}
		}
		// 完善ZkCluster中的注册中心信息，关闭迁移了的域，新建迁移过来的域
		Iterator<Entry<String, ZkCluster>> iterator3 = newClusterMap.entrySet().iterator();
		while (iterator3.hasNext()) {
			Entry<String, ZkCluster> next = iterator3.next();
			String zkClusterKey = next.getKey();
			ZkCluster zkCluster = next.getValue();
			List<NamespaceZkClusterMapping> allMappingsOfCluster = namespaceZkClusterMapping4SqlService
					.getAllMappingsOfCluster(zkClusterKey);
			ArrayList<RegistryCenterConfiguration> regCenterConfList = zkCluster.getRegCenterConfList();
			if (regCenterConfList != null) {
				Iterator<RegistryCenterConfiguration> regIter = regCenterConfList.iterator();
				while (regIter.hasNext()) {
					RegistryCenterConfiguration conf = regIter.next();
					String namespace = conf.getNamespace();
					String nns = conf.getNameAndNamespace();
					boolean include = false;
					if (allMappingsOfCluster != null) {
						for (NamespaceZkClusterMapping mapping : allMappingsOfCluster) {
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
							log.info("closed the moved namespace info, namespace is {}, old zkClusterKey is {}",
									namespace, zkClusterKey);
						}
					}
				}
			}
			if (allMappingsOfCluster != null && !zkCluster.isOffline()) {
				for (NamespaceZkClusterMapping mapping : allMappingsOfCluster) {
					String namespace = mapping.getNamespace();
					String name = StringUtils.deleteWhitespace(mapping.getName());
					if (SaturnSelfNodePath.ROOT_NAME.equals(namespace)) {
						log.error("The namespace cannot be {}", SaturnSelfNodePath.ROOT_NAME);
						continue;
					}
					boolean include = false;
					if (regCenterConfList != null) {
						for (RegistryCenterConfiguration conf : regCenterConfList) {
							if (namespace.equals(conf.getNamespace())) {
								include = true;
								String nnsOld = conf.getNameAndNamespace();
								// update name
								conf.setName(name);
								conf.initNameAndNamespace();
								String nnsNew = conf.getNameAndNamespace();
								if (!nnsOld.equals(nnsNew)) {
									synchronized (getNnsLock(nnsOld)) {
										closeNamespace(nnsOld);
										log.info(
												"closed the namespace info because it's nns is changed, namespace is {}",
												namespace);
									}
								}
								break;
							}
						}
					}
					if (!include) {
						CuratorFramework curatorFramework = zkCluster.getCuratorFramework();
						initNamespaceZkNodeIfNecessary(namespace, curatorFramework);
						RegistryCenterConfiguration conf = new RegistryCenterConfiguration(name, namespace,
								zkCluster.getZkAddr());
						conf.setZkClusterKey(zkClusterKey);
						conf.setVersion(getVersion(namespace, curatorFramework));
						conf.setZkAlias(zkCluster.getZkAlias());
						zkCluster.getRegCenterConfList().add(conf);
					}
                }
			}
		}
		// 直接赋值新的
		zkClusterMap = newClusterMap;
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
				DashboardLeaderTreeCache dashboardLeaderTreeCache = null;
				try {
					dashboardLeaderTreeCache = new DashboardLeaderTreeCache(zkCluster.getZkAlias(),
							zkCluster.getCuratorFramework());
					dashboardLeaderTreeCache.start();
					dashboardLeaderTreeCacheMap.put(zkClusterKey, dashboardLeaderTreeCache);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					if (dashboardLeaderTreeCache != null) {
						dashboardLeaderTreeCache.shutdown();
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
				DashboardLeaderTreeCache oldDashboardLeaderTreeCache = dashboardLeaderTreeCacheMap.remove(zkClusterKey);
				if (oldDashboardLeaderTreeCache != null) {
					oldDashboardLeaderTreeCache.shutdown();
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
				DashboardLeaderTreeCache dashboardLeaderTreeCache = dashboardLeaderTreeCacheMap
						.remove(zkCluster.getZkClusterKey());
				if (dashboardLeaderTreeCache != null) {
					dashboardLeaderTreeCache.shutdown();
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
			CuratorFramework tmp = curatorRepository.connect(zkAddr, null, zkCluster.getDigest());
			if (tmp == null) {
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
						refreshTreeData(zkCluster);
						InitRegistryCenterService.reloadDomainRootTreeNode();
					}

					@Override
					public void restart() {
						zkCluster.setOffline(false);
						refreshTreeData(zkCluster);
						InitRegistryCenterService.reloadDomainRootTreeNode();
					}
				};
				zkCluster.setCuratorFramework(tmp);
				zkCluster.setConnectionListener(connectionListener);
				zkCluster.setOffline(false);
				tmp.getConnectionStateListenable().addListener(connectionListener);
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
			if (curatorFramework.checkExists().forPath(executorsPath) != null) {
				List<String> executors = curatorFramework.getChildren().forPath(executorsPath);
				if (executors != null && !executors.isEmpty()) {
					for (String exe : executors) {
						String versionPath = executorsPath + "/" + exe + "/version";
						if (curatorFramework.checkExists().forPath(versionPath) != null) {
							byte[] bs = curatorFramework.getData().forPath(versionPath);
							if (bs != null) {
								String version = new String(bs, "UTF-8");
								if (version != null && !version.trim().isEmpty()) {
									String tmp = version.trim();
									if (!versionList.contains(tmp)) {
										versionList.add(tmp);
									}
								}
							}
						}
					}
				}
			}
			Collections.sort(versionList);
			String versionStr = "";
			for (int i = 0; i < versionList.size(); i++) {
				String version = versionList.get(i);
				versionStr = versionStr + version;
				if (i < versionList.size() - 1) {
					versionStr = versionStr + ", ";
				}
			}
			return versionStr;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "";
		}
	}

	private void refreshTreeData(ZkCluster zkCluster) {
		String zkClusterKey = zkCluster.getZkClusterKey();
		if (!zkCluster.isOffline()) {
			InitRegistryCenterService.initTreeJson(zkCluster.getRegCenterConfList(), zkClusterKey);
		} else {
			InitRegistryCenterService.ZKBSKEY_TO_TREENODE_MAP.remove(zkClusterKey);
		}
	}

	private void refreshTreeData() {
		// clear removed zkCluster treeData
		Iterator<Entry<String, TreeNode>> iterator = InitRegistryCenterService.ZKBSKEY_TO_TREENODE_MAP.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			Entry<String, TreeNode> next = iterator.next();
			String zkClusterKey = next.getKey();
			if (!zkClusterMap.containsKey(zkClusterKey)) {
				iterator.remove();
			}
		}
		// refresh online zkCluster treeData, clear offline zkCluster treeData
		Collection<ZkCluster> zkClusters = zkClusterMap.values();
		for (ZkCluster zkCluster : zkClusters) {
			refreshTreeData(zkCluster);
		}
		InitRegistryCenterService.reloadDomainRootTreeNode();
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
	public RequestResult refreshRegCenter() {
		RequestResult result = new RequestResult();
		if (refreshingRegCenter.compareAndSet(false, true)) {
			try {
				log.info("begin to refresh registry center");
				refreshAll();
				result.setSuccess(true);
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
				result.setSuccess(false);
				result.setMessage(ExceptionUtils.getMessage(t));
			} finally {
				log.info("end refresh registry center");
				refreshingRegCenter.set(false);
			}
		} else {
			result.setSuccess(false);
			result.setMessage("refreshing, try it later!");
		}
		return result;
	}

	@Override
	public RegistryCenterClient getCuratorByNameAndNamespace(String nameAndNamespace) {
		return registryCenterClientMap.get(nameAndNamespace);
	}

	@Override
	public boolean isDashboardLeader(String zkClusterKey) {
		DashboardLeaderTreeCache dashboardLeaderTreeCache = dashboardLeaderTreeCacheMap.get(zkClusterKey);
		if (dashboardLeaderTreeCache != null) {
			return dashboardLeaderTreeCache.isLeader();
		}
		return false;
	}

	@Override
	public ZkCluster getZkCluster(String zkClusterKey) {
		return zkClusterMap.get(zkClusterKey);
	}

	@Override
	public Collection<ZkCluster> getZkClusterList() {
		return zkClusterMap.values();
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
			if (curatorFramework.checkExists().forPath(jobsPath) != null) {
				return true;
			}
			return false;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public RequestResult refreshNamespaceFromCmdb(String userName) {
		// TODO Auto-generated method stub
		return null;
	}

}
