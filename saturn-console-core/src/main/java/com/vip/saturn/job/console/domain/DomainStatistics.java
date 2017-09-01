/**
 * 
 */
package com.vip.saturn.job.console.domain;

/**
 * @author chembo.huang
 *
 */
public class DomainStatistics {

	private static final long serialVersionUID = 1L;

	private int processCountOfAllTime;

	private int errorCountOfAllTime;

	private int processCountOfTheDay;

	private int errorCountOfTheDay;

	private final String domainName;

	private int shardingCount;

	private final String zkList;

	/** name & namespace */
	private final String nns;

	private float failureRateOfAllTime;

	public DomainStatistics(String domainName, String zkList, String nns) {
		this.domainName = domainName;
		this.zkList = zkList;
		this.nns = nns;
	}

	public float getFailureRateOfAllTime() {
		if (processCountOfAllTime == 0) {
			return 0;
		}
		float rate = (float) errorCountOfAllTime / processCountOfAllTime;
		return (float) (Math.floor(rate * 10000) / 10000.0);
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

	public void setProcessCountOfTheDay(int processCountOfTheDay) {
		this.processCountOfTheDay = processCountOfTheDay;
	}

	public int getErrorCountOfTheDay() {
		return errorCountOfTheDay;
	}

	public void setErrorCountOfTheDay(int errorCountOfTheDay) {
		this.errorCountOfTheDay = errorCountOfTheDay;
	}

	public int getShardingCount() {
		return shardingCount;
	}

	public void setShardingCount(int shardingCount) {
		this.shardingCount = shardingCount;
	}

	public String getDomainName() {
		return domainName;
	}

	public String getZkList() {
		return zkList;
	}

	public String getNns() {
		return nns;
	}

	public void setFailureRateOfAllTime(float failureRateOfAllTime) {
		this.failureRateOfAllTime = failureRateOfAllTime;
	}

	@Override
	public String toString() {
		return "DomainStatistics [processCountOfAllTime=" + processCountOfAllTime + ", errorCountOfAllTime="
				+ errorCountOfAllTime + ", processCountOfTheDay=" + processCountOfTheDay + ", errorCountOfTheDay="
				+ errorCountOfTheDay + ", domainName=" + domainName + ", shardingCount=" + shardingCount + ", zkList="
				+ zkList + ", nns=" + nns + ", failureRateOfAllTime=" + failureRateOfAllTime + "]";
	}

}
