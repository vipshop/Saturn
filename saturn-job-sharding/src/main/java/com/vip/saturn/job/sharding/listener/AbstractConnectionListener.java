package com.vip.saturn.job.sharding.listener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hebelala
 */
public abstract class AbstractConnectionListener implements ConnectionStateListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConnectionListener.class);

	private AtomicBoolean isShutdown = new AtomicBoolean(false);

	private String threadName;
	private ExecutorService executor;

	private AtomicBoolean connected = new AtomicBoolean(false);
	private AtomicBoolean stopped = new AtomicBoolean(false);

	public AbstractConnectionListener(final String threadName) {
		this.threadName = threadName;
		this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
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
		});
	}

	private long getSessionId(CuratorFramework client) {
		long sessionId;
		try {
			sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
		} catch (Exception e) {// NOSONAR
			return -1;
		}
		return sessionId;
	}

	public abstract void stop();

	public abstract void restart();

	@Override
	public void stateChanged(final CuratorFramework client, final ConnectionState newState) {
		// 使用single thread executor严格保证ZK事件执行的顺序性，避免并发性问题
		if (ConnectionState.SUSPENDED == newState) {
			connected.set(false);
			final long sessionId = getSessionId(client);
			executor.submit(new Runnable() {
				@Override
				public void run() {
					do {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						if (isShutdown.get()) {
							return;
						}
						long newSessionId = getSessionId(client);
						if (sessionId != newSessionId) {
							LOGGER.info("try to stop for zk lost");
							stop();
							stopped.set(true);
							return;
						}
					} while (!isShutdown.get() && !connected.get());
				}
			});
		} else if (ConnectionState.RECONNECTED == newState) {
			connected.set(true);
			executor.submit(new Runnable() {
				@Override
				public void run() {
					if (stopped.compareAndSet(true, false)) {
						LOGGER.info("try to restart for zk reconnected");
						restart();
					}
				}
			});
		}
	}

	public void shutdownNowUntilTerminated() throws InterruptedException {
		isShutdown.set(true);
		while (true) {
			executor.shutdownNow();
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				if (!executor.isTerminated()) {
					LOGGER.error("shutdownNowUntilTerminated is interrupted, but the {} is not terminated", threadName);
				}
				throw e;
			}
			if (executor.isTerminated()) {
				return;
			}
		}
	}

}
