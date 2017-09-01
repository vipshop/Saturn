package com.vip.saturn.job.basic;

import com.vip.saturn.job.trigger.CrondTrigger;
import com.vip.saturn.job.trigger.SaturnTrigger;

public abstract class CrondJob extends AbstractSaturnJob {

	@Override
	public SaturnTrigger getTrigger() {
		return new CrondTrigger();
	}

	@Override
	public boolean isFailoverSupported() {
		return true;
	}

	@Override
	public void enableJob() {
		boolean shouldReschedule = false;
		String timeZoneFromZk = configService.getTimeZoneStr();
		String timeZone = jobScheduler.getPreviousConf().getTimeZone();
		if (timeZoneFromZk != null && !timeZoneFromZk.equals(timeZone) || timeZoneFromZk == null && timeZone != null) {
			shouldReschedule = true;
			jobScheduler.getPreviousConf().setTimeZone(timeZoneFromZk);
		}
		String cronFromZk = configService.getCron();
		String cron = jobScheduler.getPreviousConf().getCron();
		if (cronFromZk != null && !cronFromZk.equals(cron) || cronFromZk == null && cron != null) {
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
		boolean updatePauseConditionFirst = (prePauseDate != null && !prePauseDate.equals(pauseDate));
		boolean updatePauseConditionSecond = (prePauseDate == null && pauseDate != null);
		boolean updatePauseConditionThird = (prePauseTime != null && !prePauseTime.equals(pauseTime));
		boolean updatePauseConditionFourth = (prePauseTime == null && pauseTime != null);
		if (shouldReschedule || updatePauseConditionFirst || updatePauseConditionSecond || updatePauseConditionThird
				|| updatePauseConditionFourth) {
			executionService.updateNextFireTime(executionContextService.getShardingItems());
		}
		if (updatePauseConditionFirst || updatePauseConditionSecond) {
			jobScheduler.getPreviousConf().setPausePeriodDate(pauseDate);
		}
		if (updatePauseConditionThird || updatePauseConditionFourth) {
			jobScheduler.getPreviousConf().setPausePeriodTime(pauseTime);
		}

		int countTime = configService.getJobConfiguration().getProcessCountIntervalSeconds();
		if (jobScheduler.getPreviousConf().getProcessCountIntervalSeconds() != countTime) {
			jobScheduler.getPreviousConf().setProcessCountIntervalSeconds(countTime);
			jobScheduler.rescheduleProcessCountJob();
		}
	}

	@Override
	public void disableJob() {
	}

	@Override
	public void onResharding() {
	}

}
