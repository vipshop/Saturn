/**
 * 
 */
package com.vip.saturn.job.trigger;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.spi.OperableTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.AbstractElasticJob;
import com.vip.saturn.job.basic.SaturnConstant;

/**
 * @author chembo.huang
 *
 */
public class SaturnWorker implements Runnable {
	static Logger log = LoggerFactory.getLogger(SaturnWorker.class);

	private AbstractElasticJob job;
	private OperableTrigger triggerObj;
	private final Object sigLock = new Object();
	private boolean paused = false;
	private boolean triggered = false;
	private AtomicBoolean halted = new AtomicBoolean(false);

	public SaturnWorker(AbstractElasticJob job, Trigger trigger) throws SchedulerException {
		this.job = job;
		initTrigger(trigger);
	}

	public void reInitTrigger(Trigger trigger) throws SchedulerException {
		initTrigger(trigger);
		synchronized (sigLock) {
			sigLock.notifyAll();
		}
	}

	private void initTrigger(Trigger trigger) throws SchedulerException {
		if (trigger == null)
			return;

		this.triggerObj = (OperableTrigger) trigger;
		Date ft = this.triggerObj.computeFirstFireTime(null);
		if (ft == null) {
			log.warn("[{}] msg=Based on configured schedule, the given trigger '" + trigger.getKey()
					+ "' will never fire.", job.getJobName());
		}
	}

	public boolean isShutDown() {
		return halted.get();
	}

	void togglePause(boolean pause) {
		synchronized (sigLock) {
			paused = pause;
			sigLock.notifyAll();
		}
	}

	void halt() {
		synchronized (sigLock) {
			halted.set(true);
			sigLock.notifyAll();
		}
	}

	void trigger() {
		synchronized (sigLock) {
			triggered = true;
			sigLock.notifyAll();
		}
	}

	@Override
	public void run() {
		while (!halted.get()) {
			try {
				synchronized (sigLock) {
					while (paused && !halted.get()) {
						try {
							sigLock.wait(1000L);
						} catch (InterruptedException ignore) {
						}
					}
					if (halted.get()) {
						break;
					}
				}
				boolean noFireTime = false; // 没有下次执行时间，初始化为false
				long timeUntilTrigger = 1000;
				if (triggerObj != null) {
					triggerObj.updateAfterMisfire(null);
					long now = System.currentTimeMillis();
					Date nextFireTime = triggerObj.getNextFireTime();
					if (nextFireTime != null) {
						timeUntilTrigger = nextFireTime.getTime() - now;
					} else {
						noFireTime = true;
					}
				}

				while (!noFireTime && timeUntilTrigger > 2) {
					synchronized (sigLock) {
						if (halted.get()) {
							break;
						}
						if (triggered) {
							break;
						}

						try {
							sigLock.wait(timeUntilTrigger);
						} catch (InterruptedException ignore) {
						}

						if (triggerObj != null) {
							long now = System.currentTimeMillis();
							Date nextFireTime = triggerObj.getNextFireTime();
							if (nextFireTime != null) {
								timeUntilTrigger = nextFireTime.getTime() - now;
							} else {
								noFireTime = true;
							}
						}
					}
				}
				boolean goAhead;
				// 触发执行只有两个条件：1.时间到了；2。点立即执行；
				synchronized (sigLock) {
					goAhead = !halted.get() && !paused;
					// 重置立即执行标志；
					if (triggered) {
						triggered = false;
					} else { // 非立即执行。即，执行时间到了，或者没有下次执行时间。
						goAhead = goAhead && !noFireTime;
						if (goAhead && triggerObj != null) { // 执行时间到了，更新执行时间；没有下次执行时间，不更新时间，并且不执行作业
							triggerObj.triggered(null);
						}
					}
				}
				if (goAhead) {
					job.execute();
				}

			} catch (RuntimeException e) {
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, job.getJobName(), e.getMessage()), e);
			}
		}

	}

}