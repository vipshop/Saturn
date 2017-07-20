package com.vip.saturn.job.console.mybatis.entity;

/**
 * 
 * @author hebelala
 *
 */
public class ZkClusterInfo {

	private long id;
	private String key;
	private String alias;
	private String connectString;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
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
