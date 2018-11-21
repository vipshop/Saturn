package com.vip.saturn.it.job;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LongtimeJavaJob extends AbstractSaturnJavaJob {
	public static class JobStatus {
		public int runningCount;
		public int sleepSeconds;
		public boolean running;
		public boolean finished;
		public boolean timeout;
		public boolean beforeTimeout;
		public volatile int beforeKilled = 0;
		public volatile int killed = 0;
		public boolean interrupted = false;

		public volatile int killCount = 0;
	}

	public static Map<String, JobStatus> statusMap = new HashMap<String, JobStatus>();

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		String key = jobName + "_" + shardItem;
		JobStatus status = statusMap.get(key);
		status.running = true;
		System.out.println(new Date() + " running:" + jobName + "; " + shardItem + ";" + shardParam + ";finished:"
				+ status.finished);
		try {
			Thread.sleep(status.sleepSeconds * 1000);
			status.runningCount++;
		} catch (InterruptedException e) {
			status.interrupted = true;
			System.out.println("i am terminating..");
		} finally {
			status.finished = true;
		}
		return new SaturnJobReturn();
	}

	@Override
	public void beforeTimeout(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		String key = jobName + "_" + shardItem;
		JobStatus status = statusMap.get(key);
		status.beforeTimeout = true;
		System.out.println(new Date() + "before timeout:" + jobName + "; " + shardItem + ";" + shardParam);
	}

	@Override
	public void onTimeout(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		String key = jobName + "_" + shardItem;
		JobStatus status = statusMap.get(key);
		status.timeout = true;
		System.out.println(new Date() + "run timeout:" + jobName + "; " + shardItem + ";" + shardParam);
	}

	@Override
	public void beforeForceStop(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		String key = jobName + "_" + shardItem;
		JobStatus status = statusMap.get(key);
		status.beforeKilled = ++status.killCount;
		System.out.println(new Date() + "before killed:" + jobName + "; " + shardItem + ";" + shardParam);
	}

	@Override
	public void postForceStop(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		String key = jobName + "_" + shardItem;
		JobStatus status = statusMap.get(key);
		status.killed = ++status.killCount;
		System.out.println(new Date() + "runing process killed:" + jobName + "; " + shardItem + ";" + shardParam);
	}
}
