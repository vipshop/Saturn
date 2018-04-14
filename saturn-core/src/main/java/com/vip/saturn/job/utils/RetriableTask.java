package com.vip.saturn.job.utils;

import com.vip.saturn.job.exception.SaturnExecutorException;

/**
 * 实现操作重试机制
 */
public class RetriableTask<T> {

	public static final int DEFAULT_NUMBER_OF_RETRIES = 3;

	public static final long DEFAULT_SLEEP_TIME_IN_MILL_SECONDS = 1000L;

	private RetryCallable<T> task;

	private int numberOfRetries;

	private long sleepTime;

	public RetriableTask(RetryCallable<T> task) {
		this(task, DEFAULT_NUMBER_OF_RETRIES, DEFAULT_SLEEP_TIME_IN_MILL_SECONDS);
	}

	public RetriableTask(RetryCallable<T> task, int numberOfRetries, long sleepTime) {
		this.task = task;
		this.numberOfRetries = numberOfRetries;
		this.sleepTime = sleepTime;
	}

	public T call() throws Exception {
		int count = 0;
		while (true) {
			try {
				return task.call();
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e) {
				count++;
				if (count >= numberOfRetries) {
					throw new SaturnExecutorException("retry and fails for operation", e);
				}

				Thread.sleep(sleepTime);
			}
		}
	}
}
