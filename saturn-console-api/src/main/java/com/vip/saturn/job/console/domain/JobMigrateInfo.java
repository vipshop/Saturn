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

import java.util.List;

/**
 * @author hebelala
 */
public class JobMigrateInfo {

	private String jobName;

	private List<String> tasksOld;

	private List<String> tasksMigrateEnabled;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public List<String> getTasksOld() {
		return tasksOld;
	}

	public void setTasksOld(List<String> tasksOld) {
		this.tasksOld = tasksOld;
	}

	public List<String> getTasksMigrateEnabled() {
		return tasksMigrateEnabled;
	}

	public void setTasksMigrateEnabled(List<String> tasksMigrateEnabled) {
		this.tasksMigrateEnabled = tasksMigrateEnabled;
	}
}
