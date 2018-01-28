/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. </p>
 */

package com.vip.saturn.job.internal.listener;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.analyse.AnalyseResetListenerManager;
import com.vip.saturn.job.internal.config.ConfigurationListenerManager;
import com.vip.saturn.job.internal.control.ControlListenerManager;
import com.vip.saturn.job.internal.election.ElectionListenerManager;
import com.vip.saturn.job.internal.failover.FailoverListenerManager;
import com.vip.saturn.job.internal.server.JobOperationListenerManager;
import com.vip.saturn.job.internal.sharding.ShardingListenerManager;

/**
 * 作业注册中心的监听器管理者.
 *
 *
 */
public class ListenerManager extends AbstractListenerManager {

	private ElectionListenerManager electionListenerManager;

	private FailoverListenerManager failoverListenerManager;

	private JobOperationListenerManager jobOperationListenerManager;

	private ConfigurationListenerManager configurationListenerManager;

	private ShardingListenerManager shardingListenerManager;

	private AnalyseResetListenerManager analyseResetListenerManager;

	private ControlListenerManager controlListenerManager;

	public ListenerManager(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	/**
	 * 开启所有监听器.
	 */
	@Override
	public void start() {
		electionListenerManager = new ElectionListenerManager(jobScheduler);
		failoverListenerManager = new FailoverListenerManager(jobScheduler);
		jobOperationListenerManager = new JobOperationListenerManager(jobScheduler);
		configurationListenerManager = new ConfigurationListenerManager(jobScheduler);
		shardingListenerManager = new ShardingListenerManager(jobScheduler);
		analyseResetListenerManager = new AnalyseResetListenerManager(jobScheduler);
		controlListenerManager = new ControlListenerManager(jobScheduler);

		electionListenerManager.start();
		failoverListenerManager.start();
		jobOperationListenerManager.start();
		configurationListenerManager.start();
		shardingListenerManager.start();
		analyseResetListenerManager.start();
		controlListenerManager.start();
	}

	@Override
	public void shutdown() {
		electionListenerManager.shutdown();
		failoverListenerManager.shutdown();
		jobOperationListenerManager.shutdown();
		configurationListenerManager.shutdown();
		shardingListenerManager.shutdown();
		analyseResetListenerManager.shutdown();
		controlListenerManager.shutdown();
	}
}
