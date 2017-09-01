package com.vip.saturn.job.sharding.entity;

/**
 * Created by xiaopeng.he on 2016/7/8.
 */
public class Shard {

	private String jobName;
	private int item;
	private int loadLevel;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public int getItem() {
		return item;
	}

	public void setItem(int item) {
		this.item = item;
	}

	public int getLoadLevel() {
		return loadLevel;
	}

	public void setLoadLevel(int loadLevel) {
		this.loadLevel = loadLevel;
	}

}
