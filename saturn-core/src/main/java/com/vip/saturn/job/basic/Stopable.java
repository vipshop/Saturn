/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.basic;

/**
 * 可停止的作业或目标.
 * @author dylan.xue
 */
public interface Stopable {

	/**
	 * 停止运行中的作业或目标.
	 */
	void stop();

	/**
	 * 中止作业（上报作业状态）.
	 */
	void forceStop();

	/**
	 * 恢复运行作业或目标.
	 */
	void resume();

	/**
	 * ZK断开、Executor停止时关闭作业（不上报状态）
	 */
	void abort();

	/**
	 * 关闭作业
	 */
	void shutdown();
}
