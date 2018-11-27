/**
 *
 */
package com.vip.saturn.job.console.domain;

import java.io.Serializable;

/**
 * @author chembo.huang
 */
public class JobStatistics implements Serializable {

	private static final long serialVersionUID = 1L;

	private long processCountOfAllTime;
	private long errorCountOfAllTime;
	private long processCountOfTheDay;
	private long failureCountOfTheDay;
	private int totalLoadLevel;
	private int jobDegree;
	private String jobName;
	private String domainName;
	private String nns;
	/**
	 * e.g. exe01:1,3;exe02:0,2
	 */
	private String executorsAndShards;

	public JobStatistics() {
	}

	public JobStatistics(String jobName, String domainName, String nns) {
		this.jobName = jobName;
		this.domainName = domainName;
		this.nns = nns;
	}

	public long getProcessCountOfAllTime() {
		return processCountOfAllTime;
	}

	public void setProcessCountOfAllTime(long processCountOfAllTime) {
		this.processCountOfAllTime = processCountOfAllTime;
	}

	public long getErrorCountOfAllTime() {
		return errorCountOfAllTime;
	}

	public void setErrorCountOfAllTime(long errorCountOfAllTime) {
		this.errorCountOfAllTime = errorCountOfAllTime;
	}

	public long getProcessCountOfTheDay() {
		return processCountOfTheDay;
	}

	public void setProcessCountOfTheDay(long processCountOfTheDay) {
		this.processCountOfTheDay = processCountOfTheDay;
	}

	public synchronized void incrProcessCountOfTheDay(long processCount) {
		this.processCountOfTheDay += processCount;
	}

	public long getFailureCountOfTheDay() {
		return failureCountOfTheDay;
	}

	public void setFailureCountOfTheDay(long failureCountOfTheDay) {
		this.failureCountOfTheDay = failureCountOfTheDay;
	}

	public synchronized void incrFailureCountOfTheDay(long failureCount) {
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
		if (processCountOfAllTime == 0) {
			return 0;
		}
		double rate = (double) errorCountOfAllTime / processCountOfAllTime;
		return (float) (Math.floor(rate * 10000) / 10000.0);
	}


}
