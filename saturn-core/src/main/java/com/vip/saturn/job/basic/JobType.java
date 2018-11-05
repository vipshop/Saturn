package com.vip.saturn.job.basic;

import com.vip.saturn.job.trigger.Trigger;

public interface JobType {

	String getName();

	Class<? extends Trigger> getTriggerClass();

	Class<? extends AbstractElasticJob> getHandlerClass();

	boolean isCron();

	boolean isPassive();

	boolean isJava();

	boolean isShell();

	boolean isAllowedShutdownGracefully();

}
