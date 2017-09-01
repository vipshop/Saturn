package com.vip.saturn.job.trigger;

import org.quartz.SchedulerException;

import com.vip.saturn.job.basic.AbstractElasticJob;

public interface SaturnTrigger {
	SaturnScheduler build(AbstractElasticJob job) throws SchedulerException;

	void retrigger(SaturnScheduler scheduler, AbstractElasticJob job) throws SchedulerException;
}
