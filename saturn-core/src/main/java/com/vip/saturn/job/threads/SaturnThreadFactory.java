package com.vip.saturn.job.threads;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Saturn的作业执行Factory
 * 
 * @author linzhaoming
 *
 */
public class SaturnThreadFactory implements ThreadFactory {
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private AtomicInteger threadNumber = new AtomicInteger(1);
	private boolean isMultiple = true;
	private String threadName;

	public SaturnThreadFactory(String threadName) {
		this.threadName = "Saturn-" + threadName + "-" + poolNumber.getAndIncrement() + "-thread-";
	}

	public SaturnThreadFactory(String threadName, boolean isMultiple) {
		this.isMultiple = isMultiple;
		this.threadName = threadName;
	}

	@Override
	public Thread newThread(Runnable r) {
		String name = isMultiple ? threadName + threadNumber.getAndIncrement() : threadName;
		Thread t = new Thread(r, name);
		if (t.isDaemon()) {
			t.setDaemon(false);
		}
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		return t;
	}
}
