package com.vip.saturn.job.console.mybatis.entity;

/**
 * @author hebelala
 */
public class TemporarySharedStatus {

	private long id;
	private String statusKey;
	private String statusValue;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStatusKey() {
		return statusKey;
	}

	public void setStatusKey(String statusKey) {
		this.statusKey = statusKey;
	}

	public String getStatusValue() {
		return statusValue;
	}

	public void setStatusValue(String statusValue) {
		this.statusValue = statusValue;
	}

}
