package com.vip.saturn.job.console.domain;

/**
 * @author xiaopeng.he
 */
public class AbnormalShardingState {

	private final long alertTime;

	private final int zkNodeCVersion;

	public AbnormalShardingState(long alertTime, int zkNodeCVersion) {
		this.alertTime = alertTime;
		this.zkNodeCVersion = zkNodeCVersion;
	}

	public long getAlertTime() {
		return alertTime;
	}

	public int getZkNodeCVersion() {
		return zkNodeCVersion;
	}

	@Override
	public String toString() {
		return "AbnormalShardingState [alertTime=" + alertTime + ", zkNodeCVersion=" + zkNodeCVersion + "]";
	}
}
