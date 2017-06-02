package com.vip.saturn.job.sharding;

import java.util.concurrent.ThreadFactory;

/**
 * zk treecache的线程Factory
 * 
 * @author chembo.huang
 *
 */
public class TreeCacheThreadFactory implements ThreadFactory {

	private String threadName;

	public TreeCacheThreadFactory(String threadName) {
		this.threadName = "treecache-for-" + threadName;
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
