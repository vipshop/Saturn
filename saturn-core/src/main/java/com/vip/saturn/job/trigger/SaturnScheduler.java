package com.vip.saturn.job.trigger;

import com.vip.saturn.job.basic.AbstractElasticJob;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author chembo.huang
 */
public class SaturnScheduler {

	private static final String SATURN_QUARTZ_WORKER = "-saturnWorker";
	private final AbstractElasticJob job;

	private Trigger trigger;
	private final ExecutorService executor;
	private SaturnWorker saturnWorker;

	public SaturnScheduler(final AbstractElasticJob job, final Trigger trigger) {
		this.job = job;
		this.trigger = trigger;
		executor = Executors.newSingleThreadExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r,
						job.getExecutorName() + "_" + job.getConfigService().getJobName() + SATURN_QUARTZ_WORKER);
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

	public void start() {
		saturnWorker = new SaturnWorker(job, trigger.createTriggered(false, null), trigger.createQuartzTrigger());
		if (trigger.isInitialTriggered()) {
			trigger(null);
		}
		executor.submit(saturnWorker);
	}

	public Trigger getTrigger() {
		return trigger;
	}

	public void shutdown() {
		saturnWorker.halt();
		executor.shutdown();
	}

	public boolean isTerminated() {
		return executor.isTerminated();
	}

	public void awaitTermination(long timeout) {
		try {
			executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void trigger(String triggeredDataStr) {
		saturnWorker.trigger(trigger.createTriggered(true, triggeredDataStr));
	}

	public boolean isShutdown() {
		return saturnWorker.isShutDown();
	}

	public void reInitializeTrigger() {
		saturnWorker.reInitTrigger(trigger.createQuartzTrigger());
	}

	public Date getNextFireTimePausePeriodEffected() {
		return saturnWorker.getNextFireTimePausePeriodEffected();
	}
}
