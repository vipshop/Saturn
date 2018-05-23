/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at <p>
 * http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License. </p>
 */
package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.NamespaceDomainInfo;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import java.io.File;
import org.apache.curator.framework.CuratorFramework;

import java.util.Collection;
import java.util.List;

public interface RegistryCenterService {

	void notifyRefreshRegCenter() throws SaturnJobConsoleException;

	void init() throws Exception;

	RegistryCenterClient connect(String name);

	RegistryCenterClient connectByNamespace(String namespace);

	RegistryCenterClient getCuratorByNameAndNamespace(String nameAndNamespace);

	RegistryCenterConfiguration findConfig(String nameAndNamespace);

	RegistryCenterConfiguration findConfigByNamespace(String namespace);

	List<RegistryCenterConfiguration> findConfigsByZkCluster(ZkCluster zkCluster);

	CuratorRepository.CuratorFrameworkOp connectOnly(String zkAddr, String namespace) throws SaturnJobConsoleException;

	boolean isDashboardLeader(String key);

	ZkCluster getZkCluster(String key);

	void createZkCluster(String zkClusterKey, String alias, String connectString) throws SaturnJobConsoleException;

	void updateZkCluster(String zkClusterKey, String connectString) throws SaturnJobConsoleException;

	Collection<ZkCluster> getZkClusterList();

	List<ZkCluster> getOnlineZkClusterList();

	int domainCount(String key);

	boolean namespaceIsCorrect(String namespace, CuratorFramework curatorFramework) throws SaturnJobConsoleException;

	List<String> getNamespaces() throws SaturnJobConsoleException;

	File exportNamespaceInfo(List<String> namespaces) throws SaturnJobConsoleException;

	/**
	 * Create Namespace.
	 */
	void createNamespace(NamespaceDomainInfo namespaceDomainInfo) throws SaturnJobConsoleException;

	/**
	 * Update Namespace.
	 */
	void updateNamespace(NamespaceDomainInfo namespaceDomainInfo) throws SaturnJobConsoleException;

	/**
	 * Get namespace by key.
	 */
	NamespaceDomainInfo getNamespace(String namespace) throws SaturnJobConsoleException;

	/**
	 * Bind the namespace and zkCluster. Namespace should be ensured existed.
	 */
	void bindNamespaceAndZkCluster(String namespace, String zkClusterKey, String updatedBy) throws SaturnJobConsoleException;

	CuratorRepository.CuratorFrameworkOp getCuratorFrameworkOp(String namespace) throws SaturnJobConsoleException;

}
