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

package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.container.ContainerConfig;
import com.vip.saturn.job.console.domain.container.ContainerToken;
import com.vip.saturn.job.console.domain.container.vo.ContainerScaleJobVo;
import com.vip.saturn.job.console.domain.container.vo.ContainerVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.util.List;

/**
 * @author hebelala
 */
public interface MarathonService {

	ContainerToken getContainerToken(String namespace) throws SaturnJobConsoleException;

	void saveContainerToken(String namespace, ContainerToken containerToken) throws SaturnJobConsoleException;

	List<ContainerVo> getContainerVos(String namespace) throws SaturnJobConsoleException;

	void checkContainerTokenNotNull(String namespace, ContainerToken containerToken) throws SaturnJobConsoleException;

	void saveOrUpdateContainerTokenIfNecessary(String namespace, ContainerToken containerToken) throws SaturnJobConsoleException;

	void addContainer(String namespace, ContainerConfig containerConfig) throws SaturnJobConsoleException;

	void updateContainerInstances(String namespace, String taskId, int instances) throws SaturnJobConsoleException;

	void removeContainer(String namespace, String taskId) throws SaturnJobConsoleException;

	String getContainerDetail(String namespace, String taskId) throws SaturnJobConsoleException;

	String getRegistryCatalog(String namespace) throws SaturnJobConsoleException;

	String getRegistryRepositoryTags(String namespace, String repository) throws SaturnJobConsoleException;

	void addContainerScaleJob(String namespace, String taskId, String jobDesc, int instances, String timeZone,
			String cron) throws SaturnJobConsoleException;

	ContainerScaleJobVo getContainerScaleJobVo(String namespace, String taskId, String jobName) throws SaturnJobConsoleException;

	void enableContainerScaleJob(String namespace, String jobName, boolean flag) throws SaturnJobConsoleException;

	void deleteContainerScaleJob(String namespace, String taskId, String jobName) throws SaturnJobConsoleException;

	int getContainerRunningInstances(String namespace, String taskId) throws SaturnJobConsoleException;
}
