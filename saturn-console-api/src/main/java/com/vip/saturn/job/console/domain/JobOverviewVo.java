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
