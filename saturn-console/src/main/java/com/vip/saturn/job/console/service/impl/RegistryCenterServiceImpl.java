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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;
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

	public static ConcurrentHashMap<String, RegistryCenterClient> CURATOR_CLIENT_MAP = new ConcurrentHashMap<>();
	
	public static HashMap<String/** zkAddr **/, ArrayList<RegistryCenterConfiguration>> REGISTRY_CENTER_CONFIGURATION_MAP = new HashMap<>();

	/** 为保证values有序 **/
	public static LinkedHashMap<String/** zkAddr **/, ZkCluster> ZKADDR_TO_ZKCLUSTER_MAP = new LinkedHashMap<>();
	
	
	private final AtomicBoolean refreshingRegCenter = new AtomicBoolean(false);
	
	private ConcurrentHashMap<String, NamespaceShardingManager> namespaceShardingListenerManagerMap = new ConcurrentHashMap<String, NamespaceShardingManager>();

	@PostConstruct
	public void init() throws Exception {
		refreshAll();
	}

	private String generateShardingLeadershipHostValue() {
		return LocalHostService.cachedIpAddress + "-" + UUID.randomUUID().toString();
	}

	private void refreshNamespaceShardingListenerManagerMap() {
		Collection<ZkCluster> zkClusters = RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.values();
		for (ZkCluster zkCluster: zkClusters) {
			for(RegistryCenterConfiguration conf: REGISTRY_CENTER_CONFIGURATION_MAP.get(zkCluster.getZkAddr())) {
				String namespace = conf.getNamespace();
				if(!namespaceShardingListenerManagerMap.containsKey(namespace)) {
					// client 从缓存取，不再新建也就不需要关闭
					try {
						CuratorFramework client = connect(conf.getNameAndNamespace()).getCuratorClient();
						NamespaceShardingManager newObj = new NamespaceShardingManager(client, namespace, generateShardingLeadershipHostValue());
						if (namespaceShardingListenerManagerMap.putIfAbsent(namespace, newObj) == null) {
							log.info("start NamespaceShardingManager {}", namespace);
							newObj.start();
							log.info("done starting NamespaceShardingManager {}", namespace);
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		}
		// 关闭无用的
		Iterator<Entry<String, NamespaceShardingManager>> iterator = namespaceShardingListenerManagerMap.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, NamespaceShardingManager> next = iterator.next();
			String namespace = next.getKey();
			NamespaceShardingManager namespaceShardingManager = next.getValue();
			boolean find = false;
			for (ZkCluster zkCluster: zkClusters) {
				for(RegistryCenterConfiguration conf: REGISTRY_CENTER_CONFIGURATION_MAP.get(zkCluster.getZkAddr())) {
					if(conf.getNamespace().equals(namespace)) {
						find = true;
						break;
					}
				}
			}
			if(!find) {
				namespaceShardingManager.stop();
				iterator.remove();
			}
		}
	}
	private void refreshRegistryCenterFromJsonFile() throws IOException {
		ArrayList<RegistryCenterConfiguration> list = new ArrayList<>();
		String json = FileUtils.readFileToString(new File(SaturnEnvProperties.REG_CENTER_JSON_FILE), StandardCharsets.UTF_8);
		list = (ArrayList<RegistryCenterConfiguration>) JSON.parseArray(json, RegistryCenterConfiguration.class);
		HashMap<String/** bsKey **/, ArrayList<RegistryCenterConfiguration>> newRegMap = new HashMap<>();
		for (RegistryCenterConfiguration conf: list) {
			try {
				conf.initNameAndNamespace(conf.getNameAndNamespace());
				if (conf.getZkAlias() == null) {
					conf.setZkAlias(conf.getZkAddressList());
				}
				ZkCluster cluster = ZKADDR_TO_ZKCLUSTER_MAP.get(conf.getZkAddressList());
				if (cluster == null) {
					CuratorFramework curatorFramework = curatorRepository.connect(conf.getZkAddressList(), "", conf.getDigest());
					cluster = new ZkCluster(conf.getZkAlias(), conf.getZkAddressList(), curatorFramework);
					ZKADDR_TO_ZKCLUSTER_MAP.put(conf.getZkAddressList(), cluster);
				} else if (cluster.getCuratorFramework() == null || (cluster.getCuratorFramework() !=null && !cluster.getCuratorFramework().getZookeeperClient().isConnected())) {
					CuratorFramework curatorFramework = curatorRepository.connect(conf.getZkAddressList(), "", conf.getDigest());
					cluster.setCuratorFramework(curatorFramework);
				}
				if (cluster.getCuratorFramework() == null) {
					throw new IllegalArgumentException();
				}
				cluster.setOffline(false);
				ArrayList<RegistryCenterConfiguration> confList = newRegMap.get(conf.getZkAddressList());
				if (confList == null) {
					confList = new ArrayList<>();
				}
				confList.add(conf);
				newRegMap.put(conf.getZkAddressList(), confList);
			} catch (Exception e) {
				log.error("found an offline zkCluster: {}", conf);
				log.error(e.getMessage(), e);
				ZkCluster cluster = new ZkCluster(conf.getZkAlias(), conf.getZkAddressList(), null);
				cluster.setOffline(true);
				ZKADDR_TO_ZKCLUSTER_MAP.put(conf.getZkAddressList(), cluster);
				newRegMap.put(conf.getZkAddressList(), new ArrayList<RegistryCenterConfiguration>());
			}
		}
		REGISTRY_CENTER_CONFIGURATION_MAP = newRegMap;
		CURATOR_CLIENT_MAP.clear();
	}
	
	private void refreshTreeData() {
		Collection<ZkCluster> zkClusters = RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.values();
		for (ZkCluster zkCluster : zkClusters) {
			InitRegistryCenterService.initTreeJson(REGISTRY_CENTER_CONFIGURATION_MAP.get(zkCluster.getZkAddr()), zkCluster.getZkAddr());
		}
	}
	
	@Override
	public RegistryCenterClient connect(final String nameAndNameSpace) {
		RegistryCenterClient clientInCache = findInCache(nameAndNameSpace);
		if (null != clientInCache) {
			return clientInCache;
		}
		RegistryCenterConfiguration toBeConnectedConfig = findConfig(nameAndNameSpace);
		CuratorFramework client = curatorRepository.connect(toBeConnectedConfig.getZkAddressList(),
				toBeConnectedConfig.getNamespace(), toBeConnectedConfig.getDigest());

		RegistryCenterClient result = new RegistryCenterClient(nameAndNameSpace);

		if (null == client) {
			return result;
		}
		setRegistryCenterClient(result, client);
		return result;
	}

	@Override
	public RegistryCenterClient connectByNamespace(String namespace) {
		RegistryCenterClient result = new RegistryCenterClient();
		RegistryCenterConfiguration registryCenterConfiguration = findConfigByNamespace(namespace);
		if(registryCenterConfiguration == null) {
			return result;
		}
		RegistryCenterClient clientInCache = findInCache(registryCenterConfiguration.getNameAndNamespace());
		if (null != clientInCache) {
			return clientInCache;
		}
		CuratorFramework client = curatorRepository.connect(registryCenterConfiguration.getZkAddressList(),
				registryCenterConfiguration.getNamespace(), registryCenterConfiguration.getDigest());
		result.setNameAndNamespace(registryCenterConfiguration.getNameAndNamespace());
		if (null == client) {
			return result;
		}
		setRegistryCenterClient(result, client);
		return result;
	}

	private RegistryCenterClient findInCache(final String nameAndNameSpace) {
		if (CURATOR_CLIENT_MAP.containsKey(nameAndNameSpace)) {
			if (CURATOR_CLIENT_MAP.get(nameAndNameSpace).isConnected()) {
				return CURATOR_CLIENT_MAP.get(nameAndNameSpace);
			}
			CURATOR_CLIENT_MAP.remove(nameAndNameSpace);
		}
		return null;
	}


	private void setRegistryCenterClient(final RegistryCenterClient registryCenterClient, final CuratorFramework client) {
		registryCenterClient.setConnected(true);
		registryCenterClient.setCuratorClient(client);
		CURATOR_CLIENT_MAP.putIfAbsent(registryCenterClient.getNameAndNamespace(), registryCenterClient);
	}
	@Override
	public RegistryCenterConfiguration findActivatedConfig(HttpSession session) {
		RegistryCenterConfiguration reg = (RegistryCenterConfiguration) session.getAttribute(AbstractController.ACTIVATED_CONFIG_SESSION_KEY);
		RegistryCenterClient client = RegistryCenterServiceImpl.getCuratorByNameAndNamespace(reg.getNameAndNamespace());
		if (null == client || !client.isConnected()) {
			return null;
		}
		return findConfig(client.getNameAndNamespace());
	}

	@Override
	public RegistryCenterConfiguration findConfig(String nameAndNamespace) {
		if(Strings.isNullOrEmpty(nameAndNamespace)){
			return null;
		}
		Collection<ZkCluster> zkClusters = RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.values();
		for (ZkCluster zkCluster: zkClusters) {
			for(RegistryCenterConfiguration each: REGISTRY_CENTER_CONFIGURATION_MAP.get(zkCluster.getZkAddr())) {
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
		Collection<ZkCluster> zkClusters = RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.values();
		for (ZkCluster zkCluster: zkClusters) {
			for(RegistryCenterConfiguration each: REGISTRY_CENTER_CONFIGURATION_MAP.get(zkCluster.getZkAddr())) {
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

	private void refreshAll() throws IOException {
		refreshRegistryCenterFromJsonFile();
		refreshNamespaceShardingListenerManagerMap();
		refreshTreeData();
	}
	
	public static RegistryCenterClient getCuratorByNameAndNamespace(String nameAndNamespace) {
		return CURATOR_CLIENT_MAP.get(nameAndNamespace);
	}

}
