/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.service.helper.DashboardLeaderTreeCache;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.SaturnSelfNodePath;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import com.vip.saturn.job.sharding.listener.AbstractConnectionListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.InitRegistryCenterService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.LocalHostService;
import com.vip.saturn.job.sharding.NamespaceShardingManager;

@Service
public class RegistryCenterServiceImpl implements RegistryCenterService {

	protected static Logger log = LoggerFactory.getLogger(RegistryCenterServiceImpl.class);

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private ReportAlarmService reportAlarmService;

	private final AtomicBoolean refreshingRegCenter = new AtomicBoolean(false);

	/** 为保证values有序 **/
	private LinkedHashMap<String/** zkAddr **/, ZkCluster> zkClusterMap = new LinkedHashMap<>();

	private ConcurrentHashMap<String /** zkAddr **/, DashboardLeaderTreeCache> dashboardLeaderTreeCacheMap = new ConcurrentHashMap<>();

	// namespace is unique in all zkClusters
	private ConcurrentHashMap<String /** nns */, RegistryCenterClient> registryCenterClientMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Object> registryCenterClientNnsLock = new ConcurrentHashMap<>();

	// namespace is unique in all zkClusters
	private ConcurrentHashMap<String /** nns **/, NamespaceShardingManager> namespaceShardingListenerManagerMap = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() throws Exception {
		refreshAll();
	}

	private void refreshAll() throws IOException {
		refreshRegistryCenterFromJsonFile();
		refreshDashboardLeaderTreeCache();
		refreshNamespaceShardingListenerManagerMap();
		refreshTreeData();
	}

	private String generateShardingLeadershipHostValue() {
		return LocalHostService.cachedIpAddress + "-" + UUID.randomUUID().toString();
	}

	private void refreshNamespaceShardingListenerManagerMap() {
		Iterator<Entry<String, ZkCluster>> iterator = zkClusterMap.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, ZkCluster> next = iterator.next();
			ZkCluster zkCluster = next.getValue();
			ArrayList<RegistryCenterConfiguration> regCenterConfList = zkCluster.getRegCenterConfList();
			if(regCenterConfList != null) {
				for(RegistryCenterConfiguration conf : regCenterConfList) {
					String nns = conf.getNameAndNamespace();
					if(!namespaceShardingListenerManagerMap.containsKey(nns)) {
						try {
							log.info("Start NamespaceShardingManager {}", nns);
							String namespace = conf.getNamespace();
							String digest = conf.getDigest();
							CuratorFramework client = curatorRepository.connect(conf.getZkAddressList(), namespace, digest);
							if(client != null) {
								NamespaceShardingManager namespaceShardingManager = null;
								try {
									namespaceShardingManager = new NamespaceShardingManager(client, namespace, generateShardingLeadershipHostValue(), reportAlarmService);
									namespaceShardingManager.start();
									if (namespaceShardingListenerManagerMap.putIfAbsent(nns, namespaceShardingManager) != null) {
										try {
											namespaceShardingManager.stop();
										} catch (Exception e) {
											log.error(e.getMessage(), e);
										}
										client.close();
									} else {
										log.info("Done starting NamespaceShardingManager {}", nns);
									}
								} catch (Exception e) {
									log.error(e.getMessage(), e);
									if(namespaceShardingManager != null) {
										try {
											namespaceShardingManager.stop();
										} catch (Exception e2) {
											log.error(e.getMessage(), e);
										}
									}
									if(client != null) {
										client.close();
									}
								}
							}
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
				}
			}
		}
	}

	private void refreshRegistryCenterFromJsonFile() throws IOException {
		LinkedHashMap<String/** zkAddr **/, ZkCluster> newClusterMap = new LinkedHashMap<>();
		ArrayList<RegistryCenterConfiguration> list = null;
		if (SaturnEnvProperties.REG_CENTER_JSON_FILE != null) {
			String json = FileUtils.readFileToString(new File(SaturnEnvProperties.REG_CENTER_JSON_FILE), StandardCharsets.UTF_8);
			list = (ArrayList<RegistryCenterConfiguration>) JSON.parseArray(json, RegistryCenterConfiguration.class);
		}
		// 获取新的zkClusters
		if (list != null) {
			for (RegistryCenterConfiguration conf : list) {
				if (conf.getZkAlias() == null) {
					conf.setZkAlias(conf.getZkAddressList());
				}
				if (conf.getBootstrapKey() == null) {
					conf.setBootstrapKey(conf.getZkAddressList());
				}
				String zkAddressList = conf.getZkAddressList();
				if (zkAddressList != null) {
					if (!newClusterMap.containsKey(zkAddressList)) {
						ZkCluster zkCluster = new ZkCluster();
						zkCluster.setZkAlias(conf.getZkAlias());
						zkCluster.setZkAddr(conf.getZkAddressList());
						zkCluster.setDigest(conf.getDigest());
						newClusterMap.put(zkAddressList, zkCluster);
					}
				}
			}
		}
		// 对比旧的。不包含的，关闭操作；包含的，拿取旧的curatorFramework
		Iterator<Entry<String, ZkCluster>> iterator = zkClusterMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, ZkCluster> next = iterator.next();
			String zkAddr = next.getKey();
			ZkCluster zkCluster = next.getValue();
			if (!newClusterMap.containsKey(zkAddr)) {
				iterator.remove();
				closeZkCluster(zkCluster);
			} else {
				ZkCluster newZkCluster = newClusterMap.get(zkAddr);
				newZkCluster.setCuratorFramework(zkCluster.getCuratorFramework());
				newZkCluster.setConnectionListener(zkCluster.getConnectionListener());
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
		// 完善ZkCluster中的注册中心信息
		if (list != null) {
			for (RegistryCenterConfiguration conf : list) {
				ZkCluster zkCluster = newClusterMap.get(conf.getZkAddressList());
				if (!zkCluster.isOffline()) {
					conf.initNameAndNamespace(conf.getNameAndNamespace());
					if (SaturnSelfNodePath.ROOT_NAME.equals(conf.getNamespace())) {
						log.error("The namespace cannot be {} in zkCluster {}", SaturnSelfNodePath.ROOT_NAME, zkCluster.getZkAlias());
						continue;
					}
					conf.setVersion(getVersion(conf.getNamespace(), zkCluster.getCuratorFramework()));
					zkCluster.getRegCenterConfList().add(conf);
				}
			}
		}
		// 直接赋值新的
		zkClusterMap = newClusterMap;
	}

	private void refreshDashboardLeaderTreeCache() {
		Iterator<Entry<String, ZkCluster>> iterator = zkClusterMap.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, ZkCluster> next = iterator.next();
			String zkAddr = next.getKey();
			ZkCluster zkCluster = next.getValue();
			if(!zkCluster.isOffline() && !dashboardLeaderTreeCacheMap.containsKey(zkAddr)) {
				DashboardLeaderTreeCache dashboardLeaderTreeCache = null;
				try {
					dashboardLeaderTreeCache = new DashboardLeaderTreeCache(zkCluster.getZkAlias(), zkCluster.getCuratorFramework());
					dashboardLeaderTreeCache.start();
					dashboardLeaderTreeCacheMap.put(zkAddr, dashboardLeaderTreeCache);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					if(dashboardLeaderTreeCache != null) {
						dashboardLeaderTreeCache.shutdown();
					}
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
				DashboardLeaderTreeCache dashboardLeaderTreeCache = dashboardLeaderTreeCacheMap.remove(zkCluster.getZkAddr());
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
					synchronized (getRegistryCenterClientNnsLock(nns)) {
						try {
							RegistryCenterClient registryCenterClient = registryCenterClientMap.remove(nns);
							if (registryCenterClient != null) {
								registryCenterClient.close();
							}
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
					try {
						NamespaceShardingManager namespaceShardingManager = namespaceShardingListenerManagerMap.remove(nns);
						if (namespaceShardingManager != null) {
							namespaceShardingManager.stop();
							namespaceShardingManager.getCuratorFramework().close();
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			if(zkCluster.getConnectionListener() != null) {
				zkCluster.getConnectionListener().shutdownNowUntilTerminated();
				zkCluster.setConnectionListener(null);
			}
			if(zkCluster.getCuratorFramework() != null) {
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
				AbstractConnectionListener connectionListener = new AbstractConnectionListener("zk-connectionListener-thread-for-zkCluster-" + zkCluster.getZkAlias()) {
					@Override
					public void stop() {
						zkCluster.setOffline(true);
					}
					@Override
					public void restart() {
						zkCluster.setOffline(false);
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

	private void refreshTreeData() {
		// clear removed zkCluster treeData
		Iterator<Entry<String, TreeNode>> iterator = InitRegistryCenterService.ZKBSKEY_TO_TREENODE_MAP.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, TreeNode> next = iterator.next();
			String key = next.getKey();
			if(!zkClusterMap.containsKey(key)) {
				iterator.remove();
			}
		}
		// refresh online zkCluster treeData
		Collection<ZkCluster> zkClusters = zkClusterMap.values();
		for (ZkCluster zkCluster : zkClusters) {
			if(!zkCluster.isOffline()) {
				InitRegistryCenterService.initTreeJson(zkCluster.getRegCenterConfList(), zkCluster.getZkAddr());
			}
		}
	}

	private Object getRegistryCenterClientNnsLock(String nns) {
		Object lock = new Object();
		Object pre = registryCenterClientNnsLock.putIfAbsent(nns, lock);
		if(pre != null) {
			return pre;
		} else {
			return lock;
		}
	}

	@Override
	public RegistryCenterClient connect(final String nameAndNameSpace) {
        final RegistryCenterClient registryCenterClient = new RegistryCenterClient();
		registryCenterClient.setNameAndNamespace(nameAndNameSpace);
		if(nameAndNameSpace == null) {
			return registryCenterClient;
		}
		synchronized (getRegistryCenterClientNnsLock(nameAndNameSpace)) {
			if (!registryCenterClientMap.containsKey(nameAndNameSpace)) {
				RegistryCenterConfiguration registryCenterConfiguration = findConfig(nameAndNameSpace);
				if(registryCenterConfiguration == null) {
					return registryCenterClient;
				}
				String zkAddressList = registryCenterConfiguration.getZkAddressList();
				String namespace = registryCenterConfiguration.getNamespace();
				String digest = registryCenterConfiguration.getDigest();

				CuratorFramework client = curatorRepository.connect(zkAddressList, namespace, digest);
				if (client == null) {
					return registryCenterClient;
				}
				AbstractConnectionListener connectionListener = new AbstractConnectionListener("zk-connectionListener-thread-for-registryCenterClient-" + nameAndNameSpace) {
					@Override
					public void stop() {
						registryCenterClient.setConnected(false);
					}

					@Override
					public void restart() {
						registryCenterClient.setConnected(true);
					}
				};
				registryCenterClient.setConnected(true);
				registryCenterClient.setCuratorClient(client);
				registryCenterClient.setConnectionListener(connectionListener);
				client.getConnectionStateListenable().addListener(connectionListener);
				registryCenterClientMap.put(nameAndNameSpace, registryCenterClient);
				return registryCenterClient;
			} else {
				RegistryCenterClient registryCenterClient2 = registryCenterClientMap.get(nameAndNameSpace);
				if(registryCenterClient2 != null) {
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
		if(nns == null) {
			return new RegistryCenterClient();
		}
		String zkAddressList = registryCenterConfiguration.getZkAddressList();
		String digest = registryCenterConfiguration.getDigest();
		synchronized (getRegistryCenterClientNnsLock(nns)) {
			if (!registryCenterClientMap.containsKey(nns)) {
				final RegistryCenterClient registryCenterClient = new RegistryCenterClient();
				registryCenterClient.setNameAndNamespace(nns);
				CuratorFramework client = curatorRepository.connect(zkAddressList, namespace, digest);
				if (client == null) {
					return registryCenterClient;
				}
				AbstractConnectionListener connectionListener = new AbstractConnectionListener("zk-connectionListener-thread-for-registryCenterClient-" + nns) {
					@Override
					public void stop() {
						registryCenterClient.setConnected(false);
					}

					@Override
					public void restart() {
						registryCenterClient.setConnected(true);
					}
				};
				registryCenterClient.setConnected(true);
				registryCenterClient.setCuratorClient(client);
				registryCenterClient.setConnectionListener(connectionListener);
				client.getConnectionStateListenable().addListener(connectionListener);
				registryCenterClientMap.put(nns, registryCenterClient);
				return registryCenterClient;
			} else {
				RegistryCenterClient registryCenterClient = registryCenterClientMap.get(nns);
				if (registryCenterClient == null) {
					registryCenterClient = new RegistryCenterClient();
					registryCenterClient.setNameAndNamespace(namespace);
				}
				return registryCenterClient;
			}
		}
	}

	@Override
	public RegistryCenterConfiguration findConfig(String nameAndNamespace) {
		if(Strings.isNullOrEmpty(nameAndNamespace)){
			return null;
		}
		Collection<ZkCluster> zkClusters = zkClusterMap.values();
		for (ZkCluster zkCluster: zkClusters) {
			for(RegistryCenterConfiguration each: zkCluster.getRegCenterConfList()) {
				if (each != null && nameAndNamespace.equals(each.getNameAndNamespace())) {
					return each;
				}
			}
		}
		return null;
	}

	@Override
	public RegistryCenterConfiguration findConfigByNamespace(String namespace) {
		if(Strings.isNullOrEmpty(namespace)){
			return null;
		}
		Collection<ZkCluster> zkClusters = zkClusterMap.values();
		for (ZkCluster zkCluster: zkClusters) {
			for(RegistryCenterConfiguration each: zkCluster.getRegCenterConfList()) {
				if (each != null && namespace.equals(each.getNamespace())) {
					return each;
				}
			}
		}
		return null;
	}

	@Override
	public RequestResult refreshRegCenter() {
		RequestResult result = new RequestResult();
		if(refreshingRegCenter.compareAndSet(false, true)) {
			try {
				refreshAll();
				result.setSuccess(true);
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
				result.setSuccess(false);
				result.setMessage(ExceptionUtils.getMessage(t));
			} finally {
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
	public boolean isDashboardLeader(String zkList) {
		DashboardLeaderTreeCache dashboardLeaderTreeCache = dashboardLeaderTreeCacheMap.get(zkList);
		if(dashboardLeaderTreeCache != null) {
			return dashboardLeaderTreeCache.isLeader();
		}
		return false;
	}

	@Override
	public ZkCluster getZkCluster(String zkList) {
		return zkClusterMap.get(zkList);
	}

	@Override
	public Collection<ZkCluster> getZkClusterList() {
		return zkClusterMap.values();
	}

	@Override
	public int domainCount(String zkList) {
		ZkCluster zkCluster = zkClusterMap.get(zkList);
		if(zkCluster != null) {
			ArrayList<RegistryCenterConfiguration> regList = zkCluster.getRegCenterConfList();
			if (regList != null) {
				return regList.size();
			}
		}
		return 0;
	}

}
