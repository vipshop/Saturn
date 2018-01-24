package com.vip.saturn.job.console.domain;

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

		if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) {
			return false;
		}
		if (content != null ? !content.equals(that.content) : that.content != null) {
			return false;
		}
		return zkCluster != null ? zkCluster.equals(that.zkCluster) : that.zkCluster == null;
	}

	@Override
	public int hashCode() {
		int result = namespace != null ? namespace.hashCode() : 0;
		result = 31 * result + (content != null ? content.hashCode() : 0);
		result = 31 * result + (zkCluster != null ? zkCluster.hashCode() : 0);
		return result;
	}
}
