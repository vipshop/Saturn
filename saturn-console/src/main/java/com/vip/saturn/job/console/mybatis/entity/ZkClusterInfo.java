package com.vip.saturn.job.console.mybatis.entity;

/**
 * 
 * @author hebelala
 *
 */
public class ZkClusterInfo {

	private long id;
	private String clusterKey;
	private String alias;
	private String connectString;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getClusterKey() {
		return clusterKey;
	}

	public void setClusterKey(String clusterKey) {
		this.clusterKey = clusterKey;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getConnectString() {
		return connectString;
	}

	public void setConnectString(String connectString) {
		this.connectString = connectString;
	}

}
