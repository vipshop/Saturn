package com.vip.saturn.it.job.InitMsgJobFail;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

public class InitFailOfRuntimeExceptionJob extends AbstractSaturnJavaJob {

	public static Object getObject() {
		throw new RuntimeException("RuntimeException!!!");
	}

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		return new SaturnJobReturn();
	}

}
