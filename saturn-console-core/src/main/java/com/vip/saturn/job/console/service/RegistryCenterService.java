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

package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import org.apache.curator.framework.CuratorFramework;

import java.util.Collection;
import java.util.List;

public interface RegistryCenterService {

	RequestResult refreshNamespaceFromCmdb(String userName);

	RequestResult refreshRegCenter();

	void init() throws Exception;

	RegistryCenterClient connect(String name);

	RegistryCenterClient connectByNamespace(String namespace);

	RegistryCenterClient getCuratorByNameAndNamespace(String nameAndNamespace);

	RegistryCenterConfiguration findConfig(String nameAndNamespace);

	RegistryCenterConfiguration findConfigByNamespace(String namespace);

	CuratorRepository.CuratorFrameworkOp connectOnly(String zkAddr, String namespace) throws SaturnJobConsoleException;

	boolean isDashboardLeader(String key);

	ZkCluster getZkCluster(String key);

	Collection<ZkCluster> getZkClusterList();

	int domainCount(String key);

	boolean namespaceIsCorrect(String namespace, CuratorFramework curatorFramework) throws SaturnJobConsoleException;

}
