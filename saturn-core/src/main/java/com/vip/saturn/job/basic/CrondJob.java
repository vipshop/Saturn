package com.vip.saturn.job.basic;

import com.vip.saturn.job.trigger.CrondTrigger;
import com.vip.saturn.job.trigger.SaturnTrigger;

public abstract class CrondJob extends AbstractSaturnJob{

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
		String cronFromZk = configService.getCron();
		if (jobScheduler.getPreviousConf().getCron() != null && !jobScheduler.getPreviousConf().getCron().equals(cronFromZk)) {
			jobScheduler.getPreviousConf().setCron(cronFromZk);
			jobScheduler.rescheduleJob(cronFromZk);
			executionService.updateNextFireTime(executionContextService.getShardingItems());
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
		if (updatePauseConditionFirst || updatePauseConditionSecond || updatePauseConditionThird || updatePauseConditionFourth) {
			executionService.updateNextFireTime(executionContextService.getShardingItems());
		}

	    int countTime =  configService.getJobConfiguration().getProcessCountIntervalSeconds();
	    if(jobScheduler.getPreviousConf().getProcessCountIntervalSeconds() != countTime) {
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
