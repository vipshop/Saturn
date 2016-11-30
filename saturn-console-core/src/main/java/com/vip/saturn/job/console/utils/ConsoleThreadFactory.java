package com.vip.saturn.job.console.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Saturn的作业执行Factory
 * 
 * @author chembo.huang
 *
 */
public class ConsoleThreadFactory implements ThreadFactory {
	private static AtomicInteger threadNumber = new AtomicInteger(1);
	private boolean isMultiple = false;
	private String threadName;
	

	public ConsoleThreadFactory(String threadName) {
		this.threadName = threadName;
		
	}
	public ConsoleThreadFactory(String threadName, boolean isMultiple) {
		this.isMultiple = isMultiple;
		this.threadName = threadName;
	}

	@Override
	public Thread newThread(Runnable r) {
		String name = isMultiple ? threadName + "-" + threadNumber.getAndIncrement() : threadName;
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
