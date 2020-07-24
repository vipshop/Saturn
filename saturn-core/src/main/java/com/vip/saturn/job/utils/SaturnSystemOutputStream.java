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

import org.apache.commons.exec.LogOutputStream;

import java.io.PrintStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 捕获System.out输出，存储最近一定行数的数据
 *
 * @author dylan.xue
 */
public class SaturnSystemOutputStream extends LogOutputStream {

	private static final int MAX_LINE = 100;

	// 使用InheritableThreadLocal，业务线程的子线程可以共享同一个queue
	private static ThreadLocal<SaturnSystemOutQueue<String>> queueTL = new InheritableThreadLocal<SaturnSystemOutQueue<String>>() {
		@Override
		protected SaturnSystemOutQueue<String> initialValue() {
			return new SaturnSystemOutQueue<>(MAX_LINE);
		}
	};

	private static PrintStream outCaught = new PrintStream(new SaturnSystemOutputStream(1));
	private static PrintStream out = System.out; // NOSONAR

	static {
		System.setOut(outCaught);
	}

	private SaturnSystemOutputStream(int level) {
		super(level);
	}

	@Override
	protected void processLine(String line, int level) {
		SaturnSystemOutQueue queue = queueTL.get();
		if (!queue.stopped) {
			while (!queue.offer(line)) {
				queue.poll();
			}
		}
		out.println(line);
	}

	public static void initLogger() {
		queueTL.get().clear();
	}

	private static void clearCache() {
		queueTL.remove();
	}

	public static String clearAndGetLog() {
		try {
			StringBuilder sb = new StringBuilder();
			SaturnSystemOutQueue<String> queue = queueTL.get();
			queue.stopped = true;
			String line = null;
			while ((line = queue.poll()) != null) {
				sb.append(line).append(System.lineSeparator());
			}
			clearCache();
			return sb.toString();
		} catch (Exception e) {// NOSONAR
			return "";
		}
	}

	static class SaturnSystemOutQueue<E> extends LinkedBlockingQueue<E> {

		volatile boolean stopped;

		public SaturnSystemOutQueue(int capacity) {
			super(capacity);
		}

	}
}