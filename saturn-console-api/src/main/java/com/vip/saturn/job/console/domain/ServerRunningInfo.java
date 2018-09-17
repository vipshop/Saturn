package com.vip.saturn.job.console.domain;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 记录指定executor上的所有正在运行/潜在运行的分片信息
 *
 * @author kfchu
 */
public class ServerRunningInfo {

	private String executorName;

	/**
	 * 包括正常运行中，停止中，failover中的作业分片信息;
	 * key为作业名，value为分片信息, 多个分片用逗号分隔
	 */
	private Map<String, String> runningJobItems = Maps.newHashMap();

	/**
	 * 包括不上报作业状态的作业分片信息(如秒级作业)
	 * key为作业名，value为分片信息, 多个分片用逗号分隔
	 */
	private Map<String, String> potentialRunningJobItems = Maps.newHashMap();

	public ServerRunningInfo(String executorName) {
		this.executorName = executorName;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public Map<String, String> getRunningJobItems() {
		return runningJobItems;
	}

	public void setRunningJobItems(Map<String, String> runningJobItems) {
		this.runningJobItems = runningJobItems;
	}

	public Map<String, String> getPotentialRunningJobItems() {
		return potentialRunningJobItems;
	}

	public void setPotentialRunningJobItems(Map<String, String> potentialRunningJobItems) {
		this.potentialRunningJobItems = potentialRunningJobItems;
	}
}
