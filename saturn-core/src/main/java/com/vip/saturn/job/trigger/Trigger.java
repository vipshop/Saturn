package com.vip.saturn.job.trigger;

import com.vip.saturn.job.basic.AbstractElasticJob;

/**
 * @author hebelala
 */
public interface Trigger {

	void init(AbstractElasticJob job);

	org.quartz.Trigger createQuartzTrigger();

	boolean isInitialTriggered();

	void enableJob();

	void disableJob();

	void onResharding();

	boolean isFailoverSupported();

	Triggered createTriggered(boolean yes, String upStreamDataStr);

	String serializeDownStreamData(Triggered triggered);

}
