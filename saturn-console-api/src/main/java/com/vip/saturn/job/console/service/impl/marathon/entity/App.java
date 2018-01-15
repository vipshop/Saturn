package com.vip.saturn.job.console.service.impl.marathon.entity;

/**
 * @author hebelala
 */
public class App {

	private String id;
	private Integer instances;
	private Integer tasksStaged;
	private Integer tasksRunning;
	private Integer tasksHealthy;
	private Integer tasksUnhealthy;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getInstances() {
		return instances;
	}

	public void setInstances(Integer instances) {
		this.instances = instances;
	}

	public Integer getTasksStaged() {
		return tasksStaged;
	}

	public void setTasksStaged(Integer tasksStaged) {
		this.tasksStaged = tasksStaged;
	}

	public Integer getTasksRunning() {
		return tasksRunning;
	}

	public void setTasksRunning(Integer tasksRunning) {
		this.tasksRunning = tasksRunning;
	}

	public Integer getTasksHealthy() {
		return tasksHealthy;
	}

	public void setTasksHealthy(Integer tasksHealthy) {
		this.tasksHealthy = tasksHealthy;
	}

	public Integer getTasksUnhealthy() {
		return tasksUnhealthy;
	}

	public void setTasksUnhealthy(Integer tasksUnhealthy) {
		this.tasksUnhealthy = tasksUnhealthy;
	}
}
