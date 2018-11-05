package com.vip.saturn.job.basic;

import com.vip.saturn.job.trigger.Trigger;

public class JobTypeBuilder {

	public static JobTypeBuilder newBuilder() {
		return new JobTypeBuilder();
	}

	private JobTypeImpl jobType;

	public JobTypeBuilder() {
		this.jobType = new JobTypeImpl();
	}

	public JobTypeBuilder name(String name) {
		jobType.setName(name);
		return this;
	}

	public JobTypeBuilder triggerClass(Class<? extends Trigger> triggerClass) {
		jobType.setTriggerClass(triggerClass);
		return this;
	}

	public JobTypeBuilder handlerClass(Class<? extends AbstractElasticJob> handlerClass) {
		jobType.setHandlerClass(handlerClass);
		return this;
	}

	public JobTypeBuilder cron() {
		jobType.setCron(true);
		return this;
	}

	public JobTypeBuilder passive() {
		jobType.setPassive(true);
		return this;
	}

	public JobTypeBuilder java() {
		jobType.setJava(true);
		return this;
	}

	public JobTypeBuilder shell() {
		jobType.setShell(true);
		return this;
	}

	public JobTypeBuilder allowedShutdownGracefully() {
		jobType.setAllowedShutdownGracefully(true);
		return this;
	}

	public JobType build() {
		return jobType;
	}

}
