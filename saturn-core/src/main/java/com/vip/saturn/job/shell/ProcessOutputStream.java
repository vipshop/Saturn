/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */
package com.vip.saturn.job.shell;

import org.apache.commons.exec.LogOutputStream;

import com.vip.saturn.job.utils.LRUList;

/**
 * 封装子进程的输出信息
 */
public class ProcessOutputStream extends LogOutputStream {
	private static final int MAX_LINE = 100;

	/** 保存最近运行的100条记录 */
	// private LRUMap map = new LRUMap(MAX_LINE);
	LRUList<String> lruList = new LRUList<>(MAX_LINE);

	Object lock = new Object();

	public ProcessOutputStream(final int level) {
		super(level);
	}

	@Override
	protected void processLine(final String line, final int level) {
		synchronized (lock) {
			lruList.put(line);
		}
	}

	/**
	 * @return 获取运行作业的日志
	 */
	public String getJobLog() {
		StringBuilder sb = new StringBuilder();
		synchronized (lock) {
			for (String line : lruList) {
				sb.append(line).append(System.lineSeparator());
			}
		}
		return sb.toString();
	}
}