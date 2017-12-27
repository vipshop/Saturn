package com.vip.saturn.job.console.domain;

import java.util.Objects;

public class NamespaceDomainInfo {

	private String namespace;

	private String content;

	private String zkCluster;

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getZkCluster() {
		return zkCluster;
	}

	public void setZkCluster(String zkCluster) {
		this.zkCluster = zkCluster;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NamespaceDomainInfo that = (NamespaceDomainInfo) o;
		return Objects.equals(namespace, that.namespace) &&
				Objects.equals(content, that.content) &&
				Objects.equals(zkCluster, that.zkCluster);
	}

	@Override
	public int hashCode() {
		return Objects.hash(namespace, content, zkCluster);
	}
}
