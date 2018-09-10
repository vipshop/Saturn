package com.vip.saturn.job.trigger;

import com.vip.saturn.job.basic.AbstractElasticJob;

public interface SaturnTrigger {
	SaturnScheduler build(AbstractElasticJob job);

	void retrigger(SaturnScheduler scheduler, AbstractElasticJob job);
}
