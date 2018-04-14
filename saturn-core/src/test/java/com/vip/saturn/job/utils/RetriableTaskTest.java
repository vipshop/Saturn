package com.vip.saturn.job.utils;

import com.vip.saturn.job.exception.SaturnExecutorException;
import com.vip.saturn.job.reg.exception.RegException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RetriableTaskTest {

	public static int failCount;

	@Before
	public void beforeTest() {
		failCount = 0;
	}

	@Test
	public void retrySuccessfulTest() throws Exception {
		RetriableTask<Void> retriableTask = new RetriableTask<>(new RetryCallable<Void>() {
			@Override
			public Void call() throws Exception {
				failCount++;
				return null;
			}
		});

		retriableTask.call();

		assertEquals("fail count shoule be 1", 1, failCount);
	}

	@Test
	public void retrySuccessfulWithReturnTest() throws Exception {
		RetriableTask<Boolean> retriableTask = new RetriableTask<>(new RetryCallable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return true;
			}
		});

		assertTrue(retriableTask.call());
	}

	@Test
	public void retryFailureTest() throws Exception {
		RetriableTask<Void> retriableTask = new RetriableTask<>(new RetryCallable<Void>() {
			@Override
			public Void call() throws Exception {
				failCount++;
				throw new RegException("mock an exception");
			}
		});
		try {
			retriableTask.call();
		} catch (SaturnExecutorException e) {
			assertEquals("mock an exception", e.getCause().getMessage());
		}

		assertEquals("fail count shoule be 3", 3, failCount);
	}

}