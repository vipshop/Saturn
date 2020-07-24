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

package com.vip.saturn.job.console.domain.container.vo;

import java.util.List;

/**
 * @author hebelala
 */
public class ContainerVo {

	private String taskId;
	private List<ContainerExecutorVo> containerExecutorVos;
	private String bindingJobNames;
	private String containerStatus;
	private String containerConfig;
	private String createTime;
	private List<ContainerScaleJobVo> containerScaleJobVos;
	private String instancesConfigured;

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public List<ContainerExecutorVo> getContainerExecutorVos() {
		return containerExecutorVos;
	}

	public void setContainerExecutorVos(List<ContainerExecutorVo> containerExecutorVos) {
		this.containerExecutorVos = containerExecutorVos;
	}

	public String getBindingJobNames() {
		return bindingJobNames;
	}

	public void setBindingJobNames(String bindingJobNames) {
		this.bindingJobNames = bindingJobNames;
	}

	public String getContainerStatus() {
		return containerStatus;
	}

	public void setContainerStatus(String containerStatus) {
		this.containerStatus = containerStatus;
	}

	public String getContainerConfig() {
		return containerConfig;
	}

	public void setContainerConfig(String containerConfig) {
		this.containerConfig = containerConfig;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public List<ContainerScaleJobVo> getContainerScaleJobVos() {
		return containerScaleJobVos;
	}

	public void setContainerScaleJobVos(List<ContainerScaleJobVo> containerScaleJobVos) {
		this.containerScaleJobVos = containerScaleJobVos;
	}

	public String getInstancesConfigured() {
		return instancesConfigured;
	}

	public void setInstancesConfigured(String instancesConfigured) {
		this.instancesConfigured = instancesConfigured;
	}
}
