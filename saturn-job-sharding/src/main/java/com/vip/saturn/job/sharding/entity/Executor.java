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

package com.vip.saturn.job.sharding.entity;

import java.util.List;

/**
 * @author hebelala
 */
public class Executor {

	private String executorName;
	private String ip;
	private boolean noTraffic;
	private List<String> jobNameList; // the job list supported by the executor
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
	
	public boolean isNoTraffic() {
		return noTraffic;
	}

	public void setNoTraffic(boolean noTraffic) {
		this.noTraffic = noTraffic;
	}

	public List<String> getJobNameList() {
		return jobNameList;
	}

	public void setJobNameList(List<String> jobNameList) {
		this.jobNameList = jobNameList;
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
