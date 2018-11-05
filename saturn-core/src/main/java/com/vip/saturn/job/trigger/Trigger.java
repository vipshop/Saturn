package com.vip.saturn.job.trigger;

import com.vip.saturn.job.basic.AbstractElasticJob;

public interface Trigger {

	void init(AbstractElasticJob job);

	org.quartz.Trigger createQuartzTrigger();

	boolean isInitialTriggered();

	void enableJob();

	void disableJob();

	void onResharding();

	boolean isFailoverSupported();

}
