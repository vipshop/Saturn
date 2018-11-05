package com.vip.saturn.job.console.mybatis.entity;

import java.util.Date;

/**
 * @author Ray Leung
 */
public class DashboardHistory {

	private int id;
	private String zkCluster;
	private String type;
	private String topic;
	private String content;
	private Date recordDate;

	public DashboardHistory() {
	}

	public DashboardHistory(String zkCluster, String type, String topic, String content, Date recordDate) {
		this.zkCluster = zkCluster;
		this.type = type;
		this.topic = topic;
		this.content = content;
		this.recordDate = recordDate;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getZkCluster() {
		return zkCluster;
	}

	public void setZkCluster(String zkCluster) {
		this.zkCluster = zkCluster;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getRecordDate() {
		return recordDate;
	}

	public void setRecordDate(Date recordDate) {
		this.recordDate = recordDate;
	}
}
