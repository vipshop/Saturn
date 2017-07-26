package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
public class MoveNamespaceBatchStatus {

	private boolean finished;
	private int successCount;
	private int failCount;
	private int ignoreCount;
	private int unDoCount;
	private int totalCount;
	private String moving = "";
	private List<String> failList = new ArrayList<>();
	private List<String> ignoreList = new ArrayList<>();

	public MoveNamespaceBatchStatus(int size) {
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

	public List<String> getFailList() {
		return failList;
	}

	public void setFailList(List<String> failList) {
		this.failList = failList;
	}

	public List<String> getIgnoreList() {
		return ignoreList;
	}

	public void setIgnoreList(List<String> ignoreList) {
		this.ignoreList = ignoreList;
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
