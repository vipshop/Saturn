package com.vip.saturn.job.console.domain;

import java.util.List;

/**
 * @author Ray Leung
 * @date 2018/4/22
 */
public class NamespaceAndJobNameInfo {

	private String namespace;

	private List<String> jobs;

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public List<String> getJobs() {
		return jobs;
	}

	public void setJobs(List<String> jobs) {
		this.jobs = jobs;
	}
}
