package com.vip.saturn.job.trigger;

import org.quartz.SchedulerException;

import com.vip.saturn.job.basic.AbstractElasticJob;

public class OnetimeTrigger implements SaturnTrigger {

	@Override
	public void retrigger(SaturnScheduler scheduler, AbstractElasticJob job) {
	}

	@Override
	public SaturnScheduler build(AbstractElasticJob job) throws SchedulerException {
		SaturnScheduler scheduler = new SaturnScheduler(job, null);
		scheduler.start();
		scheduler.triggerJob();
		return scheduler;
	}
}
