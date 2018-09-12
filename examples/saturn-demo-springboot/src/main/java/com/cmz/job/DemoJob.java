package com.cmz.job;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

public class DemoJob extends AbstractSaturnJavaJob {

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		// TODO Auto-generated method stub
		return new SaturnJobReturn("I am the return result of shard item " + shardItem);
	}

}
