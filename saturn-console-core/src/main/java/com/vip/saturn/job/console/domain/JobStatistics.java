/**
 * 
 */
package com.vip.saturn.job.console.domain;

import java.io.Serializable;

/**
 * @author chembo.huang
 *
 */
public class JobStatistics implements Serializable {

	private static final long serialVersionUID = 1L;

	private int processCountOfAllTime;
	private int errorCountOfAllTime;
	private int processCountOfTheDay;
	private int failureCountOfTheDay;
	private int totalLoadLevel;
	private int jobDegree;
	private String jobName;
	private String domainName;
	private String nns;
	/** e.g. exe01:1,3;exe02:0,2 */
	private String executorsAndShards;

	public JobStatistics() {
	}

	public JobStatistics(String jobName, String domainName, String nns) {
		this.jobName = jobName;
		this.domainName = domainName;
		this.nns = nns;
	}

	public int getProcessCountOfAllTime() {
		return processCountOfAllTime;
	}

	public void setProcessCountOfAllTime(int processCountOfAllTime) {
		this.processCountOfAllTime = processCountOfAllTime;
	}

	public int getErrorCountOfAllTime() {
		return errorCountOfAllTime;
	}

	public void setErrorCountOfAllTime(int errorCountOfAllTime) {
		this.errorCountOfAllTime = errorCountOfAllTime;
	}

	public int getProcessCountOfTheDay() {
		return processCountOfTheDay;
	}

	public synchronized void incrProcessCountOfTheDay(int processCount) {
		this.processCountOfTheDay+=processCount;
	}

	public int getFailureCountOfTheDay() {
		return failureCountOfTheDay;
	}

	public synchronized void incrFailureCountOfTheDay(int failureCount) {
		this.failureCountOfTheDay += failureCount;
	}

	public int getTotalLoadLevel() {
		return totalLoadLevel;
	}

	public void setTotalLoadLevel(int totalLoadLevel) {
		this.totalLoadLevel = totalLoadLevel;
	}

	public int getJobDegree() {
		return jobDegree;
	}

	public void setJobDegree(int jobDegree) {
		this.jobDegree = jobDegree;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getNns() {
		return nns;
	}

	public void setNns(String nns) {
		this.nns = nns;
	}

	public String getExecutorsAndShards() {
		return executorsAndShards;
	}

	public void setExecutorsAndShards(String executorsAndShards) {
		this.executorsAndShards = executorsAndShards;
	}

	public float getFailureRateOfAllTime() {
		if (processCountOfAllTime == 0)
			return 0;
		float rate = (float) errorCountOfAllTime / processCountOfAllTime;
		return (float) (Math.floor(rate * 10000) / 10000.0);
	}

	public void setProcessCountOfTheDay(int processCountOfTheDay) {
		this.processCountOfTheDay = processCountOfTheDay;
	}

	public void setFailureCountOfTheDay(int failureCountOfTheDay) {
		this.failureCountOfTheDay = failureCountOfTheDay;
	}
	
	

}
