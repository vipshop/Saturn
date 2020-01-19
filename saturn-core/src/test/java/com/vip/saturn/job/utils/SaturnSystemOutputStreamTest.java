package com.vip.saturn.job.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @author hebelala
 */
public class SaturnSystemOutputStreamTest {

	@Test
	public void test() throws InterruptedException {
		SaturnSystemOutputStream.initLogger();
		try {
			System.out.println("abc");
			int count = 200;
			final CountDownLatch countDownLatch = new CountDownLatch(count);
			for (int i = 0; i < count; i++) {
				final int j = i;
				new Thread(new Runnable() {
					@Override
					public void run() {
						System.out.println("abc" + j);
						countDownLatch.countDown();
					}
				}).start();
			}
			countDownLatch.await();
		} finally {
			String result = SaturnSystemOutputStream.clearAndGetLog();
			Assert.assertEquals(100, result.split(System.lineSeparator()).length);
		}
	}

}
