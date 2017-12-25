/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.domain;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * @author chembo.huang
 *
 */
public final class JobServer implements Serializable {

	private static final long serialVersionUID = -7862835299298383387L;

	private String jobName;

	private String ip;

	private String executorName;

	private ServerStatus status;

	private int processSuccessCount;

	private int processFailureCount;

	private String sharding;

	private boolean leader;

	private String version;

	private String jobVersion;

	private boolean leaderStopped;

	private JobStatus jobStatus;

	public String getPercentage() {
		int count = processSuccessCount;
		int total = processSuccessCount + processFailureCount;
		if (total == 0) {
			return "";
		}
		NumberFormat numberFormat = NumberFormat.getInstance();
		// 设置精确到小数点后2位
		numberFormat.setMaximumFractionDigits(2);
		numberFormat.setRoundingMode(RoundingMode.FLOOR);
		String result = numberFormat.format((double) count / (double) total * 100);
		return result + "%";
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public ServerStatus getStatus() {
		return status;
	}

	public void setStatus(ServerStatus status) {
		this.status = status;
	}

	public int getProcessSuccessCount() {
		return processSuccessCount;
	}

	public void setProcessSuccessCount(int processSuccessCount) {
		this.processSuccessCount = processSuccessCount;
	}

	public int getProcessFailureCount() {
		return processFailureCount;
	}

	public void setProcessFailureCount(int processFailureCount) {
		this.processFailureCount = processFailureCount;
	}

	public String getSharding() {
		return sharding;
	}

	public void setSharding(String sharding) {
		this.sharding = sharding;
	}

	public boolean isLeader() {
		return leader;
	}

	public void setLeader(boolean leader) {
		this.leader = leader;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isLeaderStopped() {
		return leaderStopped;
	}

	public void setLeaderStopped(boolean leaderStopped) {
		this.leaderStopped = leaderStopped;
	}

	public JobStatus getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(JobStatus jobStatus) {
		this.jobStatus = jobStatus;
	}

	public String getJobVersion() {
		return jobVersion;
	}

	public void setJobVersion(String jobVersion) {
		this.jobVersion = jobVersion;
	}
}
