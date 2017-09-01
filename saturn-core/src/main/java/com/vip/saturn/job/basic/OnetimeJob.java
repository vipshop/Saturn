package com.vip.saturn.job.basic;

import com.vip.saturn.job.trigger.OnetimeTrigger;
import com.vip.saturn.job.trigger.SaturnTrigger;

public abstract class OnetimeJob extends AbstractSaturnJob {

	@Override
	public SaturnTrigger getTrigger() {
		return new OnetimeTrigger();
	}

	@Override
	public boolean isFailoverSupported() {
		return false;
	}

	@Override
	public void enableJob() {
		scheduler.triggerJob();
	}

	@Override
	public void disableJob() {
		stop();
	}

}
