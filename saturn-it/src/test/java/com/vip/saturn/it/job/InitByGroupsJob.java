package com.vip.saturn.it.job;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

/**
 * @author hebelala
 */
public class InitByGroupsJob extends AbstractSaturnJavaJob {

	public static boolean inited = false;

	public InitByGroupsJob() {
		inited = true;
	}

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		return new SaturnJobReturn();
	}

}
