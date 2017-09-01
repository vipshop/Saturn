package com.vip.saturn.job.console.domain.container;

/**
 * @author hebelala
 */
public class ContainerStatus {

	private Integer totalCount;
	private Integer healthyCount;
	private Integer unhealthyCount;
	private Integer stagedCount;
	private Integer runningCount;

	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Integer totalCount) {
		this.totalCount = totalCount;
	}

	public Integer getHealthyCount() {
		return healthyCount;
	}

	public void setHealthyCount(Integer healthyCount) {
		this.healthyCount = healthyCount;
	}

	public Integer getUnhealthyCount() {
		return unhealthyCount;
	}

	public void setUnhealthyCount(Integer unhealthyCount) {
		this.unhealthyCount = unhealthyCount;
	}

	public Integer getStagedCount() {
		return stagedCount;
	}

	public void setStagedCount(Integer stagedCount) {
		this.stagedCount = stagedCount;
	}

	public Integer getRunningCount() {
		return runningCount;
	}

	public void setRunningCount(Integer runningCount) {
		this.runningCount = runningCount;
	}
}
