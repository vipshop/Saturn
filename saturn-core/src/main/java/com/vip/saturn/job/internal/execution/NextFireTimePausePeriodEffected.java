package com.vip.saturn.job.internal.execution;

import java.util.Date;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class NextFireTimePausePeriodEffected {

	private Date nextFireTime;
	
	private boolean pausePeriodEffected;

	public Date getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(Date nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	public boolean isPausePeriodEffected() {
		return pausePeriodEffected;
	}

	public void setPausePeriodEffected(boolean pausePeriodEffected) {
		this.pausePeriodEffected = pausePeriodEffected;
	}
	
	
}
