package com.vip.saturn.it.job.InitMsgJobFail;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

public class InitFailOfDefaultConstructorJob extends AbstractSaturnJavaJob {

	public InitFailOfDefaultConstructorJob() {
		int a = 1 / 0;
	}

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		return new SaturnJobReturn();
	}

}
