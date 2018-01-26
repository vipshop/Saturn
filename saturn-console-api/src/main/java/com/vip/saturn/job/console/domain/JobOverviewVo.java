package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
public class JobOverviewVo {

	private List<JobOverviewJobVo> jobs = new ArrayList<>();
	private int enabledNumber;
	private int totalNumber;
	private int abnormalNumber;

	public List<JobOverviewJobVo> getJobs() {
		return jobs;
	}

	public void setJobs(List<JobOverviewJobVo> jobs) {
		this.jobs = jobs;
	}

	public int getEnabledNumber() {
		return enabledNumber;
	}

	public void setEnabledNumber(int enabledNumber) {
		this.enabledNumber = enabledNumber;
	}

	public int getTotalNumber() {
		return totalNumber;
	}

	public void setTotalNumber(int totalNumber) {
		this.totalNumber = totalNumber;
	}

	public int getAbnormalNumber() {
		return abnormalNumber;
	}

	public void setAbnormalNumber(int abnormalNumber) {
		this.abnormalNumber = abnormalNumber;
	}
}
