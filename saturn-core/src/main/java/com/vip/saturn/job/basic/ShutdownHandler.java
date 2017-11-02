package com.vip.saturn.job.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Saturn优雅退出: 退出时清理信息
 * @author dylan.xue
 */
@SuppressWarnings("restriction")
public class ShutdownHandler implements SignalHandler {
	private static Logger log = LoggerFactory.getLogger(ShutdownHandler.class);

	private static ConcurrentHashMap<String, List<Runnable>> listeners = new ConcurrentHashMap<>();
	private static List<Runnable> globalListeners = new ArrayList<>();

	private static ShutdownHandler handler;
	private static volatile boolean exit = true;

	private static final AtomicBoolean isHandling = new AtomicBoolean(false);

	static {
		handler = new ShutdownHandler();
		Signal.handle(new Signal("TERM"), handler); // 相当于kill -15
		Signal.handle(new Signal("INT"), handler); // 相当于Ctrl+C
	}

	public static void addShutdownCallback(Runnable c) {
		if (isHandling.get()) {
			return;
		}
		globalListeners.add(c);
	}

	public static void addShutdownCallback(String executorName, Runnable c) {
		if (isHandling.get()) {
			return;
		}
		if (!listeners.containsKey(executorName)) {
			listeners.putIfAbsent(executorName, new ArrayList<Runnable>());
		}
		listeners.get(executorName).add(c);
	}

	public static void removeShutdownCallback(String executorName) {
		if (isHandling.get()) {
			return;
		}
		listeners.remove(executorName);
	}

	public static void exitAfterHandler(boolean exit) {
		ShutdownHandler.exit = exit;
	}

	@Override
	public void handle(Signal sn) {
		if (isHandling.compareAndSet(false, true)) {
			try {
				doHandle(sn);
			} finally {
				isHandling.set(false);
			}
		} else {
			log.info("shutdown is handling");
		}
	}

	private void doHandle(Signal sn) {
		log.info("msg=Received the kill command");

		Iterator<Entry<String, List<Runnable>>> iterator = listeners.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, List<Runnable>> next = iterator.next();
			List<Runnable> value = next.getValue();
			for (Runnable runnable : value) {
				try {
					if (runnable != null) {
						runnable.run();
					}
				} catch (Exception e) {
					log.error("msg=" + e.getMessage(), e);
				}
			}
		}
		listeners.clear();

		for (Runnable runnable : globalListeners) {
			try {
				if (runnable != null) {
					runnable.run();
				}
			} catch (Exception e) {
				log.error("msg=" + e.getMessage(), e);
			}
		}
		globalListeners.clear();

		log.info("msg=Saturn executor is closed");
		if (exit) {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			loggerContext.stop();

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace(); // NOSONAR
			}

			System.exit(-1);
		}
	}
}
