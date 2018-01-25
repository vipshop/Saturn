package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public class NamespaceMigrationOverallStatus {

	private boolean finished;
	private int successCount;
	private int failCount;
	private int ignoreCount;
	private int unDoCount;
	private int totalCount;
	private String moving = "";

	public NamespaceMigrationOverallStatus() {

	}

	public NamespaceMigrationOverallStatus(int size) {
		this.unDoCount = size;
		this.totalCount = size;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
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

	public int getIgnoreCount() {
		return ignoreCount;
	}

	public void setIgnoreCount(int ignoreCount) {
		this.ignoreCount = ignoreCount;
	}

	public int getUnDoCount() {
		return unDoCount;
	}

	public void setUnDoCount(int unDoCount) {
		this.unDoCount = unDoCount;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public String getMoving() {
		return moving;
	}

	public void setMoving(String moving) {
		this.moving = moving;
	}

	public void incrementSuccessCount() {
		successCount++;
	}

	public void incrementFailCount() {
		failCount++;
	}

	public void incrementIgnoreCount() {
		ignoreCount++;
	}

	public void decrementUnDoCount() {
		unDoCount--;
	}

}
