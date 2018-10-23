package com.vip.saturn.demo.job;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

public class DemoJob extends AbstractSaturnJavaJob {

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		System.out.println(jobName + " is running, item is " + shardItem);
		return new SaturnJobReturn();
	}
}
