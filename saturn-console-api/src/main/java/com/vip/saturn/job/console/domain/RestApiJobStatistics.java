package com.vip.saturn.job.console.domain;

/**
 * @author xiaopeng.he
 */
public class RestApiJobStatistics {

	private Long lastBeginTime;

	private Long lastCompleteTime;

	private Long nextFireTime;

	private Long processCount;

	private Long processErrorCount;

	public Long getLastBeginTime() {
		return lastBeginTime;
	}

	public void setLastBeginTime(Long lastBeginTime) {
		this.lastBeginTime = lastBeginTime;
	}

	public Long getLastCompleteTime() {
		return lastCompleteTime;
	}

	public void setLastCompleteTime(Long lastCompleteTime) {
		this.lastCompleteTime = lastCompleteTime;
	}

	public Long getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(Long nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	public Long getProcessCount() {
		return processCount;
	}

	public void setProcessCount(Long processCount) {
		this.processCount = processCount;
	}

	public Long getProcessErrorCount() {
		return processErrorCount;
	}

	public void setProcessErrorCount(Long processErrorCount) {
		this.processErrorCount = processErrorCount;
	}
}
