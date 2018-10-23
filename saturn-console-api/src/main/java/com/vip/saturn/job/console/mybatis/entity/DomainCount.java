package com.vip.saturn.job.console.mybatis.entity;

import java.util.Date;

/**
 * @author Ray Leung
 */
public class DomainCount {

	private int id;
	private String zkCluster;
	private int successCount;
	private int failCount;
	private Date recordDate;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public int getFailCount() {
		return failCount;
	}

	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}

	public String getZkCluster() {
		return zkCluster;
	}

	public void setZkCluster(String zkCluster) {
		this.zkCluster = zkCluster;
	}

	public Date getRecordDate() {
		return recordDate;
	}

	public void setRecordDate(Date recordDate) {
		this.recordDate = recordDate;
	}
}
