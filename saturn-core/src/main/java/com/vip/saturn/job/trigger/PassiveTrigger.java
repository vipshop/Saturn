package com.vip.saturn.job.trigger;

/**
 * @author hebelala
 */
public class PassiveTrigger extends AbstractTrigger {

	@Override
	public org.quartz.Trigger createQuartzTrigger() {
		return null;
	}

	@Override
	public boolean isInitialTriggered() {
		return false;
	}

	@Override
	public void enableJob() {
		int countTime = job.getConfigService().getJobConfiguration().getProcessCountIntervalSeconds();
		if (job.getJobScheduler().getPreviousConf().getProcessCountIntervalSeconds() != countTime) {
			job.getJobScheduler().getPreviousConf().setProcessCountIntervalSeconds(countTime);
			job.getJobScheduler().rescheduleProcessCountJob();
		}
	}

	@Override
	public void disableJob() {

	}

	@Override
	public void onResharding() {

	}

	@Override
	public boolean isFailoverSupported() {
		return job.getConfigService().isEnabledReport();
	}

}
