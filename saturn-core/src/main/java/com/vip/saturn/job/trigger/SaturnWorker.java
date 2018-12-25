package com.vip.saturn.job.trigger;

import com.vip.saturn.job.basic.AbstractElasticJob;
import com.vip.saturn.job.exception.JobException;
import com.vip.saturn.job.utils.LogEvents;
import com.vip.saturn.job.utils.LogUtils;
import org.quartz.Trigger;
import org.quartz.spi.OperableTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author chembo.huang
 */
public class SaturnWorker implements Runnable {

	static Logger log = LoggerFactory.getLogger(SaturnWorker.class);
	private final Object sigLock = new Object();
	private final AbstractElasticJob job;
	private final Triggered notTriggered;
	private volatile OperableTrigger triggerObj;
	private volatile boolean paused = false;
	private volatile Triggered triggered;
	private AtomicBoolean halted = new AtomicBoolean(false);

	public SaturnWorker(AbstractElasticJob job, Triggered notTriggered, Trigger trigger) {
		this.job = job;
		this.notTriggered = notTriggered;
		this.triggered = notTriggered;
		initTrigger(trigger);
	}

	void reInitTrigger(Trigger trigger) {
		initTrigger(trigger);
		synchronized (sigLock) {
			sigLock.notifyAll();
		}
	}

	private void initTrigger(Trigger trigger) {
		if (trigger == null) {
			return;
		}
		if (!(trigger instanceof OperableTrigger)) {
			throw new JobException("the trigger should be the instance of OperableTrigger");
		}
		this.triggerObj = (OperableTrigger) trigger;
		Date ft = this.triggerObj.computeFirstFireTime(null);
		if (ft == null) {
			LogUtils.warn(log, LogEvents.ExecutorEvent.COMMON,
					"Based on configured schedule, the given trigger {} will never fire.", trigger.getKey(),
					job.getJobName());
		}
	}

	boolean isShutDown() {
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

	void trigger(Triggered triggered) {
		synchronized (sigLock) {
			this.triggered = triggered == null ? notTriggered : triggered;
			sigLock.notifyAll();
		}
	}

	Date getNextFireTimePausePeriodEffected() {
		if (triggerObj == null) {
			return null;
		}
		triggerObj.updateAfterMisfire(null);
		Date nextFireTime = triggerObj.getNextFireTime();
		while (nextFireTime != null && job.getConfigService().isInPausePeriod(nextFireTime)) {
			nextFireTime = triggerObj.getFireTimeAfter(nextFireTime);
		}
		return nextFireTime;
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
						if (triggered.isYes()) {
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
				Triggered currentTriggered = notTriggered;
				// 触发执行只有两个条件：1.时间到了 2.点立即执行
				synchronized (sigLock) {
					goAhead = !halted.get() && !paused;
					// 重置立即执行标志，赋值当前立即执行数据
					if (triggered.isYes()) {
						currentTriggered = triggered;
						triggered = notTriggered;
					} else if (goAhead) { // 非立即执行。即，执行时间到了，或者没有下次执行时间
						goAhead = goAhead && !noFireTime; // 有下次执行时间，即执行时间到了，才执行作业
						if (goAhead) { // 执行时间到了，更新执行时间
							if (triggerObj != null) {
								triggerObj.triggered(null);
							}
						} else { // 没有下次执行时间，则尝试睡一秒，防止不停的循环导致CPU使用率过高（如果cron不再改为周期性执行）
							try {
								sigLock.wait(1000L);
							} catch (InterruptedException ignore) {
							}
						}
					}
				}
				if (goAhead) {
					job.execute(currentTriggered);
				}

			} catch (RuntimeException e) {
				LogUtils.error(log, job.getJobName(), e.getMessage(), e);
			}
		}

	}

}