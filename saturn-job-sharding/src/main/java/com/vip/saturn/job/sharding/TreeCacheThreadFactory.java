package com.vip.saturn.job.sharding;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * zk treecache的线程Factory
 * 
 * @author chembo.huang
 *
 */
public class TreeCacheThreadFactory implements ThreadFactory {
	private static AtomicInteger threadNumber = new AtomicInteger(1);
	private String threadName;

	public TreeCacheThreadFactory(String path, int depth) {
		this.threadName = "treecache-for-" + path + "-" + depth + "-" + threadNumber;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, threadName);
		if (t.isDaemon()) {
			t.setDaemon(false);
		}
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		return t;
	}
}
