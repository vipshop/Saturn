package com.vip.saturn.it.job;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleJavaJob extends AbstractSaturnJavaJob {
	public static Map<String, Integer> statusMap = new HashMap<String, Integer>();

	@Override
	public String getJobVersion() {
		return "2.2.1";
	}

	public static AtomicBoolean enabled = new AtomicBoolean(false);

	public static AtomicBoolean lock = new AtomicBoolean(false);

	private static synchronized void countInc(String key) {
		Integer status = statusMap.get(key);
		int count = 0;
		if (status != null) {
			count = status;
		}
		count++;
		statusMap.put(key, count);
	}

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		String key = jobName + "_" + shardItem;
		System.out.println(new Date() + " running:" + jobName + "; " + shardItem + ";" + shardParam);
		countInc(key);
		return new SaturnJobReturn(" result:" + jobName + "; " + shardItem + ";" + shardParam);
	}

	@Override
	public void onEnabled(String jobName) {
		enabled.set(true);
		if (lock.get()) {
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onDisabled(String jobName) {
		enabled.set(false);
	}
}
