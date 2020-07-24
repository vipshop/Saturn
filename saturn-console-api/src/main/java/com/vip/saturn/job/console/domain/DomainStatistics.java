/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
