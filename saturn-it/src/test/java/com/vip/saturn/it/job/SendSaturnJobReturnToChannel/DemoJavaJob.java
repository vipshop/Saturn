package com.vip.saturn.it.job.SendSaturnJobReturnToChannel;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

/**
 * @author dylan.xue
 */
public class DemoJavaJob extends AbstractSaturnJavaJob {

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		switch (shardItem) {
		case 0:
			return new SaturnJobReturn("find you ok");
		case 1:
			return new SaturnJobReturn(5001, "find you failed", 500);
		case 2:
			int a = 1 / 0;
		case 3:
			Thread.sleep(5000);
		case 4:
			return null;
		default:
			return new SaturnJobReturn("DemoMsgJob the item is not handled");
		}
	}

}
