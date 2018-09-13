package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Job allocation information for the executor.
 *
 * @author ray.leung
 */
public class ServerAllocationInfo {

	private String executorName;

	private List<Map<String, String>> jobStatus = new ArrayList<>();

	private int totalLoadLevel;

	public ServerAllocationInfo(String executorName) {
		this.executorName = executorName;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public List<Map<String, String>> getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(List<Map<String, String>> jobStatus) {
		this.jobStatus = jobStatus;
	}

	public int getTotalLoadLevel() {
		return totalLoadLevel;
	}

	public void setTotalLoadLevel(int totalLoadLevel) {
		this.totalLoadLevel = totalLoadLevel;
	}
}
