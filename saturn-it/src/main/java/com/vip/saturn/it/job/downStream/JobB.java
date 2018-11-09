package com.vip.saturn.it.job.downStream;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

public class JobB extends AbstractSaturnJavaJob {

	public static volatile int count = 0;

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		count++;
		return new SaturnJobReturn();
	}

}
