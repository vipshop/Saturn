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

package com.vip.saturn.job.console.domain;

/**
 * @author xiaopeng.he
 */
public class RestApiJobInfo {

	private String jobName;

	private String description;

	private Boolean enabled;

	private String runningStatus;

	private RestApiJobConfig jobConfig;

	private RestApiJobStatistics statistics;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getRunningStatus() {
		return runningStatus;
	}

	public void setRunningStatus(String runningStatus) {
		this.runningStatus = runningStatus;
	}

	public RestApiJobConfig getJobConfig() {
		return jobConfig;
	}

	public void setJobConfig(RestApiJobConfig jobConfig) {
		this.jobConfig = jobConfig;
	}

	public RestApiJobStatistics getStatistics() {
		return statistics;
	}

	public void setStatistics(RestApiJobStatistics statistics) {
		this.statistics = statistics;
	}
}
