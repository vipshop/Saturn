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
