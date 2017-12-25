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
