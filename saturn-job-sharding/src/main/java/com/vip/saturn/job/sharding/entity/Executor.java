package com.vip.saturn.job.sharding.entity;

import java.util.List;

/**
 * Created by xiaopeng.he on 2016/7/8.
 */
public class Executor {

    private String executorName;
    private String ip;
    private List<Shard> shardList;
    private int totalLoadLevel;
	public String getExecutorName() {
		return executorName;
	}
	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public List<Shard> getShardList() {
		return shardList;
	}
	public void setShardList(List<Shard> shardList) {
		this.shardList = shardList;
	}
	public int getTotalLoadLevel() {
		return totalLoadLevel;
	}
	public void setTotalLoadLevel(int totalLoadLevel) {
		this.totalLoadLevel = totalLoadLevel;
	}

    
}
