package com.vip.saturn.job.basic;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.SaturnJobReturn;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class ShardingItemFutureTask implements Callable<SaturnJobReturn> {
	private static Logger log = LoggerFactory.getLogger(ShardingItemFutureTask.class);

	private JavaShardingItemCallable callable;

	private Callable<?> doneFinallyCallback;

	private ScheduledFuture<?> timeoutFuture;

	private Future<?> callFuture;

	private volatile boolean done = false; //NOSONAR

	public Future<?> getCallFuture() {
		return callFuture;
	}

	public void setCallFuture(Future<?> callFuture) {
		this.callFuture = callFuture;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public ScheduledFuture<?> getTimeoutFuture() {
		return timeoutFuture;
	}

	public void setTimeoutFuture(ScheduledFuture<?> timeoutFuture) {
		this.timeoutFuture = timeoutFuture;
	}

	public ShardingItemFutureTask(JavaShardingItemCallable callable, Callable<?> doneFinallyCallback) {
		this.callable = callable;
		this.doneFinallyCallback = doneFinallyCallback;
	}

	public JavaShardingItemCallable getCallable() {
		return this.callable;
	}

	public void reset() {
		done = false;
		callable.reset();
	}

	@Override
	public SaturnJobReturn call() throws Exception {
		Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler(){

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				if(e instanceof IllegalMonitorStateException || e instanceof ThreadDeath){		
					log.warn(String.format(SaturnConstant.ERROR_LOG_FORMAT, callable.getJobName(), "business thread pool maybe crashed"), e);
					if(callFuture != null){
						callFuture.cancel(false);
					}
					log.warn(String.format(SaturnConstant.ERROR_LOG_FORMAT, callable.getJobName(), "close the old business thread pool, and re-create new one"));
					callable.getSaturnJob().getJobScheduler().reCreateExecutorService();
				}
			}
			
		});
		try {
			SaturnJobReturn ret = callable.call();
			return ret;
		} finally {
			done();
		}
	}

	private void done() {
		if (timeoutFuture != null) {
			timeoutFuture.cancel(true);
			timeoutFuture = null;
		}

		if (done) {
			return;
		}
		done = true;
		try {
			try {
				if (callable.isTimeout()) {
					callable.onTimeout();
				}
			} catch (Throwable t) {
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, callable.getJobName(), t.getMessage()), t);
			}

			try {
				if (callable.isForceStop()) {
					callable.postForceStop();
				}
			} catch (Throwable t) {
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, callable.getJobName(), t.getMessage()), t);
			}

			callable.checkAndSetSaturnJobReturn();

			callable.afterExecution();

		} finally {
			try {
				if (doneFinallyCallback != null) {
					doneFinallyCallback.call();
				}
			} catch (Exception e) {
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, callable.getJobName(), e.getMessage()), e);
			}
		}
	}

	public static void killRunningBusinessThread(ShardingItemFutureTask shardingItemFutureTask) {
		JavaShardingItemCallable shardingItemCallable = shardingItemFutureTask.getCallable();
		Thread businessThread = shardingItemCallable.getCurrentThread();
		if (businessThread != null) {
			try {

				while (!shardingItemCallable.isBreakForceStop() && !shardingItemFutureTask.isDone()) {
					businessThread.stop();
					if (!shardingItemCallable.isBreakForceStop() && !shardingItemFutureTask.isDone()) {
						Thread.sleep(50);
					} else {
						break;
					}
				}
				
			} catch (InterruptedException e) {// NOSONAR
			}
		}
	}
}
