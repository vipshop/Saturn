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

package com.vip.saturn.job.internal.listener;

import org.apache.curator.framework.state.ConnectionStateListener;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.Shutdownable;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.reg.zookeeper.ZkCacheManager;

/**
 * 作业注册中心的监听器管理者的抽象类.
 * 
 * 
 */
public abstract class AbstractListenerManager implements Shutdownable {

	protected CoordinatorRegistryCenter coordinatorRegistryCenter;
	protected String jobName;
	protected String executorName;
	protected JobScheduler jobScheduler;

	protected JobConfiguration jobConfiguration;
	protected ZkCacheManager zkCacheManager;

	public AbstractListenerManager(final JobScheduler jobScheduler) {
		this.jobScheduler = jobScheduler;
		this.jobName = jobScheduler.getJobName();
		this.executorName = jobScheduler.getExecutorName();
		jobConfiguration = jobScheduler.getCurrentConf();
		coordinatorRegistryCenter = jobScheduler.getCoordinatorRegistryCenter();
		zkCacheManager = jobScheduler.getZkCacheManager();
	}

	public abstract void start();

	protected void addConnectionStateListener(final ConnectionStateListener listener) {
		coordinatorRegistryCenter.addConnectionStateListener(listener);
	}

	protected void removeConnectionStateListener(final ConnectionStateListener listener) {
		coordinatorRegistryCenter.removeConnectionStateListener(listener);
	}

	@Override
	public void shutdown() {
	}
}
