package com.vip.saturn.job.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Saturn优雅退出: 退出时清理信息
 * @author dylan.xue
 */
@SuppressWarnings("restriction")
public class ShutdownHandler implements SignalHandler {
	private static Logger log = LoggerFactory.getLogger(ShutdownHandler.class);

	private static ConcurrentHashMap<String, List<Runnable>> executorListeners = new ConcurrentHashMap<>();
	private static List<Runnable> globalListeners = new ArrayList<>();

	private static ShutdownHandler handler;
	private static volatile boolean isExit = true;

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
		if (!executorListeners.containsKey(executorName)) {
			executorListeners.putIfAbsent(executorName, new ArrayList<Runnable>());
		}
		executorListeners.get(executorName).add(c);
	}

	public static void removeShutdownCallback(String executorName) {
		if (isHandling.get()) {
			return;
		}
		executorListeners.remove(executorName);
	}

	public static void exitAfterHandler(boolean isExit) {
		ShutdownHandler.isExit = isExit;
	}

	@Override
	public void handle(Signal sn) {
		if (isHandling.compareAndSet(false, true)) {
			try {
				doHandle();
			} finally {
				isHandling.set(false);
			}
		} else {
			log.info("shutdown is handling");
		}
	}

	private void doHandle() {
		log.info("msg=Received the kill command");
		callExecutorListeners();
		callGlobalListeners();
		log.info("msg=Saturn executor is closed");
		if (isExit) {
			exit();
		}
	}

	private static void callExecutorListeners() {
		Iterator<Entry<String, List<Runnable>>> iterator = executorListeners.entrySet().iterator();
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
		executorListeners.clear();
	}

	private static void callGlobalListeners() {
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
	}

	private void exit() {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace(); // NOSONAR
			Thread.currentThread().interrupt();
		}

		System.exit(-1);
	}
}
