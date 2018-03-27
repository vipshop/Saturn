package com.vip.saturn.job.basic;

import com.vip.saturn.job.trigger.CrondTrigger;
import com.vip.saturn.job.trigger.SaturnTrigger;

public abstract class CrondJob extends AbstractSaturnJob {

	@Override
	public SaturnTrigger getTrigger() {
		return new CrondTrigger();
	}

	// 如果上报运行状态，则支持failover；否则，不支持failover；
	@Override
	public boolean isFailoverSupported() {
		return configService.isEnabledReport();
	}

	@Override
	public void enableJob() {
		boolean shouldReschedule = false;
		String timeZoneFromZk = configService.getTimeZoneStr();
		String timeZone = jobScheduler.getPreviousConf().getTimeZone();
		// timeZoneFromZk is non null
		if (!timeZoneFromZk.equals(timeZone)) {
			shouldReschedule = true;
			jobScheduler.getPreviousConf().setTimeZone(timeZoneFromZk);
		}
		String cronFromZk = configService.getCron();
		String cron = jobScheduler.getPreviousConf().getCron();
		if ((cronFromZk != null && !cronFromZk.equals(cron)) || (cronFromZk == null && cron != null)) {
			shouldReschedule = true;
			jobScheduler.getPreviousConf().setCron(cronFromZk);
		}
		if (shouldReschedule) {
			jobScheduler.rescheduleJob(cronFromZk);
		}

		// if PausePeriodDatePath or pausePeriodTime changed.
		String prePauseDate = jobScheduler.getPreviousConf().getPausePeriodDate();
		String prePauseTime = jobScheduler.getPreviousConf().getPausePeriodTime();
		String pauseDate = configService.getJobConfiguration().getPausePeriodDate();
		String pauseTime = configService.getJobConfiguration().getPausePeriodTime();

		boolean shouldSetPausePeriodDate = shouldSetPausePeriodDate(prePauseDate, pauseDate);
		boolean shouldSetPausePeriodTime = shouldSetPausePeriodTime(prePauseTime, pauseTime);

		if (shouldReschedule || shouldSetPausePeriodDate || shouldSetPausePeriodTime) {
			executionService.updateNextFireTime(executionContextService.getShardingItems());
		}
		if (shouldSetPausePeriodDate) {
			jobScheduler.getPreviousConf().setPausePeriodDate(pauseDate);
		}
		if (shouldSetPausePeriodTime) {
			jobScheduler.getPreviousConf().setPausePeriodTime(pauseTime);
		}

		int countTime = configService.getJobConfiguration().getProcessCountIntervalSeconds();
		if (jobScheduler.getPreviousConf().getProcessCountIntervalSeconds() != countTime) {
			jobScheduler.getPreviousConf().setProcessCountIntervalSeconds(countTime);
			jobScheduler.rescheduleProcessCountJob();
		}
	}

	private boolean shouldSetPausePeriodDate(String prePauseDate, String pauseDate) {
		boolean updatePauseConditionFirst = (prePauseDate != null && !prePauseDate.equals(pauseDate));
		boolean updatePauseConditionSecond = (prePauseDate == null && pauseDate != null);

		return updatePauseConditionFirst || updatePauseConditionSecond;
	}

	private boolean shouldSetPausePeriodTime(String prePauseTime, String pauseTime) {
		boolean updatePauseConditionThird = (prePauseTime != null && !prePauseTime.equals(pauseTime));
		boolean updatePauseConditionFourth = (prePauseTime == null && pauseTime != null);

		return updatePauseConditionThird || updatePauseConditionFourth;
	}

	@Override
	public void disableJob() {
	}

	@Override
	public void onResharding() {
	}

}
