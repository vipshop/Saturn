/**
 * vips Inc. Copyright (c) 2016 All Rights Reserved.
 */
package com.vip.saturn.job.console.domain;

import java.io.Serializable;
import java.util.Collection;

/**
 * 项目名称：saturn-job-console 创建时间：2016年7月26日 下午7:02:29
 * @author yangjuanying
 * @version 1.0
 * @since JDK 1.7.0_05 文件名称：JobInfoObject.java 类说明：
 */
public class JobInfoObject implements Serializable {

	private static final long serialVersionUID = -5131341906163122876L;

	/**
	 * 判断是新域还是旧域（新域是有version节点的，即1.1.0及其之后的版本）
	 */
	int isNewSaturn;

	/**
	 * 当前域的所有Executor版本和指定的版本进行比较
	 */
	int compareSpecifiedVersion;

	Collection<JobBriefInfo> jobsBriefInfos;

	public JobInfoObject() {
	}

	public JobInfoObject(int isNewSaturn, int compareSpecifiedVersion, Collection<JobBriefInfo> jobsBriefInfos) {
		this.isNewSaturn = isNewSaturn;
		this.compareSpecifiedVersion = compareSpecifiedVersion;
		this.jobsBriefInfos = jobsBriefInfos;
	}

	public int getIsNewSaturn() {
		return isNewSaturn;
	}

	public void setIsNewSaturn(int isNewSaturn) {
		this.isNewSaturn = isNewSaturn;
	}

	public int getCompareSpecifiedVersion() {
		return compareSpecifiedVersion;
	}

	public void setCompareSpecifiedVersion(int compareSpecifiedVersion) {
		this.compareSpecifiedVersion = compareSpecifiedVersion;
	}

	public Collection<JobBriefInfo> getJobsBriefInfos() {
		return jobsBriefInfos;
	}

	public void setJobsBriefInfos(Collection<JobBriefInfo> jobsBriefInfos) {
		this.jobsBriefInfos = jobsBriefInfos;
	}

}
