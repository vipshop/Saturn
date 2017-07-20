package com.vip.saturn.job.console.mybatis.entity;

/**
 * 
 * @author hebelala
 *
 */
public class NamespaceZkClusterMapping {

	private long id;
	private String namespace;
	private String name;
	private String clusterKey;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClusterKey() {
		return clusterKey;
	}

	public void setClusterKey(String clusterKey) {
		this.clusterKey = clusterKey;
	}

}
