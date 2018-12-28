package com.vip.saturn.job.trigger;

import com.vip.saturn.job.exception.JobException;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.TriggerBuilder;

import java.text.ParseException;

/**
 * @author hebelala
 */
public class CronTrigger extends AbstractTrigger {

	@Override
	public org.quartz.Trigger createQuartzTrigger() {
		return createTrigger();
	}

	private org.quartz.Trigger createTrigger() {
		String cron = job.getConfigService().getCron();
		if (StringUtils.isBlank(cron)) {
			cron = "* * * * * ? 2099";
		} else {
			cron = cron.trim();
			validateCron(cron);
		}
		CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
		cronScheduleBuilder = cronScheduleBuilder.inTimeZone(job.getConfigService().getTimeZone())
				.withMisfireHandlingInstructionDoNothing();
		org.quartz.CronTrigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(job.getExecutorName() + "_" + job.getJobName()).withSchedule(cronScheduleBuilder).build();
		if (trigger instanceof org.quartz.spi.MutableTrigger) {
			((org.quartz.spi.MutableTrigger) trigger)
					.setMisfireInstruction(org.quartz.CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
		}
		return trigger;
	}

	private void validateCron(String cron) {
		try {
			CronExpression.validateExpression(cron);
		} catch (ParseException e) {
			log.error("validate cron failed", e);
			throw new JobException(e);
		}
	}

	@Override
	public boolean isInitialTriggered() {
		return false;
	}

	@Override
	public void enableJob() {
		boolean shouldReschedule = false;
		String timeZoneFromZk = job.getConfigService().getTimeZoneStr();
		String timeZone = job.getJobScheduler().getPreviousConf().getTimeZone();
		// timeZoneFromZk is non null
		if (!timeZoneFromZk.equals(timeZone)) {
			shouldReschedule = true;
			job.getJobScheduler().getPreviousConf().setTimeZone(timeZoneFromZk);
		}
		String cronFromZk = job.getConfigService().getCron();
		String cron = job.getJobScheduler().getPreviousConf().getCron();
		if ((cronFromZk != null && !cronFromZk.equals(cron)) || (cronFromZk == null && cron != null)) {
			shouldReschedule = true;
			job.getJobScheduler().getPreviousConf().setCron(cronFromZk);
		}
		if (shouldReschedule) {
			job.getJobScheduler().reInitializeTrigger();
		}

		// if PausePeriodDatePath or pausePeriodTime changed.
		String prePauseDate = job.getJobScheduler().getPreviousConf().getPausePeriodDate();
		String prePauseTime = job.getJobScheduler().getPreviousConf().getPausePeriodTime();
		String pauseDate = job.getConfigService().getJobConfiguration().getPausePeriodDate();
		String pauseTime = job.getConfigService().getJobConfiguration().getPausePeriodTime();

		boolean shouldSetPausePeriodDate = shouldSetPausePeriodDate(prePauseDate, pauseDate);
		boolean shouldSetPausePeriodTime = shouldSetPausePeriodTime(prePauseTime, pauseTime);

		if (shouldReschedule || shouldSetPausePeriodDate || shouldSetPausePeriodTime) {
			job.getExecutionService().updateNextFireTime(job.getExecutionContextService().getShardingItems());
		}
		if (shouldSetPausePeriodDate) {
			job.getJobScheduler().getPreviousConf().setPausePeriodDate(pauseDate);
		}
		if (shouldSetPausePeriodTime) {
			job.getJobScheduler().getPreviousConf().setPausePeriodTime(pauseTime);
		}

		int countTime = job.getConfigService().getJobConfiguration().getProcessCountIntervalSeconds();
		if (job.getJobScheduler().getPreviousConf().getProcessCountIntervalSeconds() != countTime) {
			job.getJobScheduler().getPreviousConf().setProcessCountIntervalSeconds(countTime);
			job.getJobScheduler().rescheduleProcessCountJob();
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

	@Override
	public boolean isFailoverSupported() {
		// 如果上报运行状态，则支持failover；否则，不支持failover
		return job.getConfigService().isEnabledReport();
	}
}
