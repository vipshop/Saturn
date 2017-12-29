package com.vip.saturn.job.console.domain;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;

/**
 * Job allocation information for the executor.
 *
 * @author kfchu
 */
public class ServerAllocationInfo {

	private String executorName;

	private int totalLoadLevel;

	// key为jobName，value是分片item号列表
	private Map<String, String> allocationMap = Maps.newHashMap();

	public ServerAllocationInfo(String executorName) {
		this.executorName = executorName;
	}

	public int getTotalLoadLevel() {
		return totalLoadLevel;
	}

	public void setTotalLoadLevel(int totalLoadLevel) {
		this.totalLoadLevel = totalLoadLevel;
	}

	public Map<String, String> getAllocationMap() {
		return allocationMap;
	}

	public void setAllocationMap(Map<String, String> allocationMap) {
		this.allocationMap = allocationMap;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}
}
