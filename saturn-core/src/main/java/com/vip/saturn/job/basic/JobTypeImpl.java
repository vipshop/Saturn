package com.vip.saturn.job.basic;

import com.vip.saturn.job.trigger.Trigger;

public class JobTypeImpl implements JobType {

	private String name;
	private Class<? extends Trigger> triggerClass;
	private Class<? extends AbstractElasticJob> handlerClass;

	private boolean isCron;
	private boolean isPassive;
	private boolean isJava;
	private boolean isShell;
	private boolean isAllowedShutdownGracefully;

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setTriggerClass(Class<? extends Trigger> triggerClass) {
		this.triggerClass = triggerClass;
	}

	@Override
	public Class<? extends Trigger> getTriggerClass() {
		return triggerClass;
	}

	public void setHandlerClass(Class<? extends AbstractElasticJob> handlerClass) {
		this.handlerClass = handlerClass;
	}

	@Override
	public Class<? extends AbstractElasticJob> getHandlerClass() {
		return handlerClass;
	}

	public void setCron(boolean cron) {
		isCron = cron;
	}

	@Override
	public boolean isCron() {
		return isCron;
	}

	public void setPassive(boolean passive) {
		isPassive = passive;
	}

	@Override
	public boolean isPassive() {
		return isPassive;
	}

	public void setJava(boolean java) {
		isJava = java;
	}

	@Override
	public boolean isJava() {
		return isJava;
	}

	public void setShell(boolean shell) {
		isShell = shell;
	}

	@Override
	public boolean isShell() {
		return isShell;
	}

	public void setAllowedShutdownGracefully(boolean allowedShutdownGracefully) {
		isAllowedShutdownGracefully = allowedShutdownGracefully;
	}

	@Override
	public boolean isAllowedShutdownGracefully() {
		return isAllowedShutdownGracefully;
	}
}
