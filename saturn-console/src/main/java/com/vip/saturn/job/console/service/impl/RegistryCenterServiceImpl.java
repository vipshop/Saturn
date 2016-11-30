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

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.InitRegistryCenterService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.LocalHostService;
import com.vip.saturn.job.sharding.NamespaceShardingManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RegistryCenterServiceImpl implements RegistryCenterService {

	protected static Logger log = LoggerFactory.getLogger(RegistryCenterServiceImpl.class);
	@Resource
	private CuratorRepository curatorRepository;

	public static ConcurrentHashMap<String, RegistryCenterClient> CURATOR_CLIENT_MAP = new ConcurrentHashMap<>();
	
	public static CuratorFramework CURRENT_ROOT_ZK_CLIENT;
	
	public static Set<RegistryCenterConfiguration> registryCenterConfiguration = new LinkedHashSet<>();
	
	private final AtomicBoolean refreshingRegCenter = new AtomicBoolean(false);
	
	private ConcurrentHashMap<String, NamespaceShardingManager> namespaceShardingListenerManagerMap = new ConcurrentHashMap<String, NamespaceShardingManager>();

	@PostConstruct
	public void init() throws Exception {
		// 优先使用reg.center.property的值
		refreshRegistryCenterFromPropertyOrJsonFile();
		refreshNamespaceShardingListenerManagerMap();
		// TODO
		//		InitRegistryCenterService.initTreeJson(registryCenterConfiguration);
	}

	private String generateShardingLeadershipHostValue() {
		return LocalHostService.cachedIpAddress + "-" + UUID.randomUUID().toString();
	}

	private void refreshNamespaceShardingListenerManagerMap() {
		for(RegistryCenterConfiguration conf : registryCenterConfiguration) {
			String namespace = conf.getNamespace();
			if(!namespaceShardingListenerManagerMap.containsKey(namespace)) {
				CuratorFramework client = curatorRepository.connect(conf.getZkAddressList(), namespace, null);
				NamespaceShardingManager newObj = new NamespaceShardingManager(client, namespace, generateShardingLeadershipHostValue());
				if(namespaceShardingListenerManagerMap.putIfAbsent(namespace, newObj) == null) {
					try {
						log.info("start NamespaceShardingManager {}", namespace);
						newObj.start();
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				} else {
					client.close();
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
			for(RegistryCenterConfiguration conf : registryCenterConfiguration) {
				if(conf.getNamespace().equals(namespace)) {
					find = true;
					break;
				}
			}
			if(!find) {
				namespaceShardingManager.stop();
				iterator.remove();
			}
		}
	}
	
	private void refreshRegistryCenterFromPropertyOrJsonFile() {
		// 优先使用reg.center.property的值
		ArrayList<RegistryCenterConfiguration> list = new ArrayList<>();
		if (StringUtils.isNotBlank(SaturnEnvProperties.REG_CENTER_VALUE)) {
			list = (ArrayList<RegistryCenterConfiguration>) JSON.parseArray(SaturnEnvProperties.REG_CENTER_VALUE, RegistryCenterConfiguration.class);
		} else {
			try {
				String json = FileUtils.readFileToString(new File(SaturnEnvProperties.REG_CENTER_JSON_FILE), StandardCharsets.UTF_8);
				list = (ArrayList<RegistryCenterConfiguration>) JSON.parseArray(json, RegistryCenterConfiguration.class);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		registryCenterConfiguration.clear();
		for (RegistryCenterConfiguration conf: list) {
			conf.initNameAndNamespace(conf.getNameAndNamespace());
			registryCenterConfiguration.add(conf);
		}
	}
	
	@Override
	public Set<RegistryCenterConfiguration> getConfigs() {
		return registryCenterConfiguration;
	}
	
	@Override
	public void add(final RegistryCenterConfiguration config) {
		registryCenterConfiguration.add(config);
	}
	
	@Override
	public RegistryCenterClient connect(final String nameAndNameSpace) {
		RegistryCenterClient result = new RegistryCenterClient(nameAndNameSpace);
		RegistryCenterConfiguration toBeConnectedConfig = findConfig(nameAndNameSpace);
		RegistryCenterClient clientInCache = findInCache(nameAndNameSpace);
		if (null != clientInCache) {
			return clientInCache;
		}
		CuratorFramework client = curatorRepository.connect(toBeConnectedConfig.getZkAddressList(),
				toBeConnectedConfig.getNamespace(), toBeConnectedConfig.getDigest());
		if (null == client) {
			return result;
		}
		setRegistryCenterClient(result, nameAndNameSpace, client);
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
		if (null == client) {
			return result;
		}
		setRegistryCenterClient(result, registryCenterConfiguration.getNameAndNamespace(), client);
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

	private void setRegistryCenterClient(final RegistryCenterClient registryCenterClient, final String nameAndNameSpace,
			final CuratorFramework client) {
		registryCenterClient.setNameAndNamespace(nameAndNameSpace);
		registryCenterClient.setConnected(true);
		registryCenterClient.setCuratorClient(client);
		CURATOR_CLIENT_MAP.putIfAbsent(nameAndNameSpace, registryCenterClient);
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
		for (RegistryCenterConfiguration each : registryCenterConfiguration) {
			if (each != null && nameAndNamespace.equals(each.getNameAndNamespace())) {
				return each;
			}
		}
		return null;
	}

	@Override
	public RegistryCenterConfiguration findConfigByNamespace(String namespace) {
		if(Strings.isNullOrEmpty(namespace)){
			return null;
		}
		for (RegistryCenterConfiguration each : registryCenterConfiguration) {
			if (each != null && namespace.equals(each.getNamespace())) {
				return each;
			}
		}
		return null;
	}

	@Override
	public RequestResult refreshRegCenter() {
		RequestResult result = new RequestResult();
		if(refreshingRegCenter.compareAndSet(false, true)) {
			try {
				refreshRegistryCenterFromPropertyOrJsonFile();
				refreshNamespaceShardingListenerManagerMap();
				InitRegistryCenterService.initTreeJson(registryCenterConfiguration);
				result.setSuccess(true);
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
				result.setSuccess(false);
				result.setMessage(t.getMessage());
			} finally {
				refreshingRegCenter.set(false);
			}
		} else {
			result.setSuccess(false);
			result.setMessage("refreshing, retry later if necessary!");
		}
		return result;
	}
	
	public static RegistryCenterClient getCuratorByNameAndNamespace(String nameAndNamespace) {
		return CURATOR_CLIENT_MAP.get(nameAndNamespace);
	}

}
