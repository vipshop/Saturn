/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
