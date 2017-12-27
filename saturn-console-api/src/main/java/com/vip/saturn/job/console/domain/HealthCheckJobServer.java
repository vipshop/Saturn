/**
 * vips Inc. Copyright (c) 2016 All Rights Reserved.
 */
package com.vip.saturn.job.console.domain;

import java.io.Serializable;

/**
 * 项目名称：saturn-job-console 创建时间：2016年5月13日 下午6:26:52
 *
 * @author yangjuanying
 * @version 1.0
 * @since JDK 1.7.0_05 文件名称：HealthCheckJobServer.java 类说明： 健康检查信息类
 */
public class HealthCheckJobServer implements Serializable {

	private static final long serialVersionUID = -1250144872651729666L;

	private String jobName;

	private String executorName;

	private String version;

	private String namespace;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

}
