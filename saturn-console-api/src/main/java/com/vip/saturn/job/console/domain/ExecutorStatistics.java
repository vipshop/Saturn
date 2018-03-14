/**
 *
 */
package com.vip.saturn.job.console.domain;

import java.io.Serializable;

/**
 * @author chembo.huang
 */
public class ExecutorStatistics implements Serializable {

	private static final long serialVersionUID = 1L;

	private String executorName;

	private String domain;

	private int loadLevel;

	private String nns;

	private String ip;

	private boolean runInDocker = false;

	private int processCountOfTheDay = 0;

	private int failureCountOfTheDay = 0;

	/**
	 * e.g. job1:1,3;job2:0,4,6;job3:0
	 */
	private String jobAndShardings;

	private float failureRateOfTheDay;

	public ExecutorStatistics() {
	}

	public ExecutorStatistics(String executorName, String domain) {
		this.executorName = executorName;
		this.domain = domain;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public int getLoadLevel() {
		return loadLevel;
	}

	public void setLoadLevel(int loadLevel) {
		this.loadLevel = loadLevel;
	}

	public String getNns() {
		return nns;
	}

	public void setNns(String nns) {
		this.nns = nns;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public boolean isRunInDocker() {
		return runInDocker;
	}

	public void setRunInDocker(boolean runInDocker) {
		this.runInDocker = runInDocker;
	}

	public int getProcessCountOfTheDay() {
		return processCountOfTheDay;
	}

	public void setProcessCountOfTheDay(int processCountOfTheDay) {
		this.processCountOfTheDay = processCountOfTheDay;
	}

	public int getFailureCountOfTheDay() {
		return failureCountOfTheDay;
	}

	public void setFailureCountOfTheDay(int failureCountOfTheDay) {
		this.failureCountOfTheDay = failureCountOfTheDay;
	}

	public String getJobAndShardings() {
		return jobAndShardings;
	}

	public void setJobAndShardings(String jobAndShardings) {
		this.jobAndShardings = jobAndShardings;
	}

	public float getFailureRateOfTheDay() {
		if (processCountOfTheDay == 0) {
			return 0;
		}
		double rate = (double) failureCountOfTheDay / processCountOfTheDay;
		return (float) (Math.floor(rate * 10000) / 10000.0);
	}

	public void setFailureRateOfTheDay(float failureRateOfTheDay) {
		this.failureRateOfTheDay = failureRateOfTheDay;
	}
}
