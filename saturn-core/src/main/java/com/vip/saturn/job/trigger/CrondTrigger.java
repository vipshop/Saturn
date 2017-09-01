package com.vip.saturn.job.trigger;

import java.text.ParseException;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.triggers.AbstractTrigger;

import com.vip.saturn.job.basic.AbstractElasticJob;
import com.vip.saturn.job.exception.JobException;

public class CrondTrigger implements SaturnTrigger {

	/**
	 * 验证cron表达式的合法性
	 */
	private static void validateCron(String cron) {
		if (cron != null && !cron.trim().isEmpty()) {
			try {
				CronExpression.validateExpression(cron.trim());
			} catch (ParseException e) {
				throw new JobException(e);
			}
		}
	}

	public Trigger createTrigger(AbstractElasticJob job) {
		String cron = job.getConfigService().getCron();
		validateCron(cron);
		CronScheduleBuilder cronScheduleBuilder;
		if (cron != null && !cron.trim().isEmpty()) {
			cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cron.trim());
		} else {
			cronScheduleBuilder = CronScheduleBuilder.cronSchedule("* * * * * ? 2099");
		}
		cronScheduleBuilder = cronScheduleBuilder.inTimeZone(job.getConfigService().getTimeZone())
				.withMisfireHandlingInstructionDoNothing();
		Trigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(job.getExecutorName() + "_" + job.getJobName())
				.withSchedule(cronScheduleBuilder).build();
		((AbstractTrigger<CronTrigger>) cronTrigger).setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);

		return cronTrigger;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SaturnScheduler build(AbstractElasticJob job) throws SchedulerException {
		SaturnScheduler scheduler = new SaturnScheduler(job, createTrigger(job));
		scheduler.start();
		return scheduler;
	}

	@Override
	public void retrigger(SaturnScheduler scheduler, AbstractElasticJob job) throws SchedulerException {
		scheduler.rescheduleJob(createTrigger(job));
	}
}
