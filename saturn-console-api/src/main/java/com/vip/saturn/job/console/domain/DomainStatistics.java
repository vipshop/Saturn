/**
 *
 */
package com.vip.saturn.job.console.domain;

/**
 * @author chembo.huang
 */
public class DomainStatistics {

	private static final long serialVersionUID = 1L;

	private long processCountOfAllTime;

	private long errorCountOfAllTime;

	private long processCountOfTheDay;

	private long errorCountOfTheDay;

	private String domainName;

	private int shardingCount;

	private String zkList;

	/**
	 * name & namespace
	 */
	private String nns;

	private float failureRateOfAllTime;

	public DomainStatistics() {
	}

	public DomainStatistics(String domainName, String zkList, String nns) {
		this.domainName = domainName;
		this.zkList = zkList;
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

	public long getErrorCountOfTheDay() {
		return errorCountOfTheDay;
	}

	public void setErrorCountOfTheDay(long errorCountOfTheDay) {
		this.errorCountOfTheDay = errorCountOfTheDay;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public int getShardingCount() {
		return shardingCount;
	}

	public void setShardingCount(int shardingCount) {
		this.shardingCount = shardingCount;
	}

	public String getZkList() {
		return zkList;
	}

	public void setZkList(String zkList) {
		this.zkList = zkList;
	}

	public String getNns() {
		return nns;
	}

	public void setNns(String nns) {
		this.nns = nns;
	}

	public float getFailureRateOfAllTime() {
		if (processCountOfAllTime == 0) {
			return 0;
		}
		double rate = (double) errorCountOfAllTime / processCountOfAllTime;
		return (float) (Math.floor(rate * 10000) / 10000.0);
	}

	public void setFailureRateOfAllTime(float failureRateOfAllTime) {
		this.failureRateOfAllTime = failureRateOfAllTime;
	}
}
