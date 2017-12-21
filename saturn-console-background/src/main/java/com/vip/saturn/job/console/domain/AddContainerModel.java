package com.vip.saturn.job.console.domain;

import com.vip.saturn.job.console.domain.container.ContainerToken;

import java.util.List;
import java.util.Map;

/**
 * @author hebelala
 */
public class AddContainerModel {
	private ContainerToken containerToken;
	private String taskId;
	private String cmd;
	private Float cpus;
	private Float mem;
	private Integer instances;
	private List<List<String>> constraints;
	private Map<String, String> env;
	private Boolean privileged;
	private Boolean forcePullImage;
	private List<Map<String, String>> parameters;
	private List<Map<String, String>> volumes;
	private String image;

	public ContainerToken getContainerToken() {
		return containerToken;
	}

	public void setContainerToken(ContainerToken containerToken) {
		this.containerToken = containerToken;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public Float getCpus() {
		return cpus;
	}

	public void setCpus(Float cpus) {
		this.cpus = cpus;
	}

	public Float getMem() {
		return mem;
	}

	public void setMem(Float mem) {
		this.mem = mem;
	}

	public Integer getInstances() {
		return instances;
	}

	public void setInstances(Integer instances) {
		this.instances = instances;
	}

	public List<List<String>> getConstraints() {
		return constraints;
	}

	public void setConstraints(List<List<String>> constraints) {
		this.constraints = constraints;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public Boolean getPrivileged() {
		return privileged;
	}

	public void setPrivileged(Boolean privileged) {
		this.privileged = privileged;
	}

	public Boolean getForcePullImage() {
		return forcePullImage;
	}

	public void setForcePullImage(Boolean forcePullImage) {
		this.forcePullImage = forcePullImage;
	}

	public List<Map<String, String>> getParameters() {
		return parameters;
	}

	public void setParameters(List<Map<String, String>> parameters) {
		this.parameters = parameters;
	}

	public List<Map<String, String>> getVolumes() {
		return volumes;
	}

	public void setVolumes(List<Map<String, String>> volumes) {
		this.volumes = volumes;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}
}
