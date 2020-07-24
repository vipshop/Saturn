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

package com.vip.saturn.job.sharding;

import java.util.concurrent.ThreadFactory;

/**
 * zk treecache的线程Factory
 * 
 * @author chembo.huang
 *
 */
public class TreeCacheThreadFactory implements ThreadFactory {

	private String threadName;

	public TreeCacheThreadFactory(String threadName) {
		this.threadName = "treecache-for-" + threadName;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, threadName);
		if (t.isDaemon()) {
			t.setDaemon(false);
		}
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		return t;
	}
}
