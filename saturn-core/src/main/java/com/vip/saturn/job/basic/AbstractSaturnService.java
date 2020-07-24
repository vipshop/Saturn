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
package com.vip.saturn.job.basic;

import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.storage.JobNodeStorage;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;

public abstract class AbstractSaturnService implements Shutdownable {

	protected String executorName;

	protected String jobName;

	protected JobScheduler jobScheduler;

	protected JobConfiguration jobConfiguration;

	protected CoordinatorRegistryCenter coordinatorRegistryCenter;

	protected JobNodeStorage jobNodeStorage;

	public AbstractSaturnService(final JobScheduler jobScheduler) {
		this.jobScheduler = jobScheduler;
		this.jobName = jobScheduler.getJobName();
		this.executorName = jobScheduler.getExecutorName();
		jobConfiguration = jobScheduler.getCurrentConf();
		coordinatorRegistryCenter = jobScheduler.getCoordinatorRegistryCenter();
		jobNodeStorage = jobScheduler.getJobNodeStorage();
	}

	/**
	 * 获取executorName
	 * @return
	 */
	public String getExecutorName() {
		return executorName;
	}

	/**
	 * 获取作业名
	 * @return
	 */
	public String getJobName() {
		return jobName;
	}

	/**
	 * 获取jobScheduler
	 * @return
	 */
	public JobScheduler getJobScheduler() {
		return jobScheduler;
	}

	/**
	 * 获取jobConfiguration
	 * @return
	 */
	public JobConfiguration getJobConfiguration() {
		return jobConfiguration;
	}

	/**
	 * 获取zk
	 * @return
	 */
	public CoordinatorRegistryCenter getCoordinatorRegistryCenter() {
		return coordinatorRegistryCenter;
	}

	/**
	 * 获取jobNodeStorage
	 * @return
	 */
	public JobNodeStorage getJobNodeStorage() {
		return jobNodeStorage;
	}

	/**
	 * 服务启用
	 */
	public void start() {

	}

	@Override
	public void shutdown() {
	}
}
