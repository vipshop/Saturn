package com.vip.saturn.job.console.marathon.entity;

import java.util.List;

/**
 * @author hebelala
 */
public class Task {

	private String appId;
	private String host;
	private String id;
	private List<Integer> ports;

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Integer> getPorts() {
		return ports;
	}

	public void setPorts(List<Integer> ports) {
		this.ports = ports;
	}
}
