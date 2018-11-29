package com.vip.saturn.it.job.SendSaturnJobReturnToChannel;

import com.vip.saturn.job.AbstractSaturnMsgJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.msg.MsgHolder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xiaopeng.he on 2016/8/19.
 */
public class DemoMsgJob extends AbstractSaturnMsgJob {

	public static AtomicInteger okCount = new AtomicInteger(0);
	public static AtomicInteger failCount = new AtomicInteger(0);

	@Override
	public SaturnJobReturn handleMsgJob(String jobName, Integer shardItem, String shardParam, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		switch (shardItem) {
		case 0:
			okCount.incrementAndGet();
			return new SaturnJobReturn("find you ok");
		case 1:
			failCount.incrementAndGet();
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
