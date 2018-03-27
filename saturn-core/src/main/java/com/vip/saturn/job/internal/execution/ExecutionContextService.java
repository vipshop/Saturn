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

package com.vip.saturn.job.internal.execution;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobExecutionMultipleShardingContext;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.SaturnExecutionContext;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.internal.failover.FailoverService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 作业运行时上下文服务.
 */
public class ExecutionContextService extends AbstractSaturnService {

	private ConfigurationService configService;

	private FailoverService failoverService;

	public ExecutionContextService(JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	@Override
	public void start() {
		configService = jobScheduler.getConfigService();
		failoverService = jobScheduler.getFailoverService();
	}

	/**
	 * 获取当前作业服务器运行时分片上下文.
	 *
	 * @return 当前作业服务器运行时分片上下文
	 */
	public JobExecutionMultipleShardingContext getJobExecutionShardingContext() {
		SaturnExecutionContext result = new SaturnExecutionContext();
		result.setJobName(configService.getJobName());
		result.setShardingTotalCount(configService.getShardingTotalCount());
		List<Integer> shardingItems = getShardingItems();
		boolean isEnabledReport = configService.isEnabledReport();
		if (isEnabledReport) {
			removeRunningItems(shardingItems);
		}
		result.setShardingItems(shardingItems);
		result.setJobParameter(configService.getJobParameter());
		result.setCustomContext(configService.getCustomContext());
		result.setJobConfiguration(jobConfiguration);

		if (coordinatorRegistryCenter != null) {
			result.setNamespace(coordinatorRegistryCenter.getNamespace());
			result.setExecutorName(coordinatorRegistryCenter.getExecutorName());
		}

		if (result.getShardingItems().isEmpty()) {
			return result;
		}
		Map<Integer, String> shardingItemParameters = configService.getShardingItemParameters();
		if (shardingItemParameters.containsKey(-1)) { // 本地模式
			for (int each : result.getShardingItems()) {
				result.getShardingItemParameters().put(each, shardingItemParameters.get(-1));
			}
		} else {
			for (int each : result.getShardingItems()) {
				if (shardingItemParameters.containsKey(each)) {
					result.getShardingItemParameters().put(each, shardingItemParameters.get(each));
				}
			}
		}
		if (jobConfiguration.getTimeoutSeconds() > 0) {
			result.setTimetoutSeconds(jobConfiguration.getTimeoutSeconds());
		}
		return result;
	}

	private void removeRunningItems(final List<Integer> items) {
		List<Integer> toBeRemovedItems = new ArrayList<>(items.size());
		for (int each : items) {
			if (isRunningItem(each)) {
				toBeRemovedItems.add(each);
			}
		}
		items.removeAll(toBeRemovedItems);
	}

	private boolean isRunningItem(final int item) {
		return jobScheduler.getJobNodeStorage().isJobNodeExisted(ExecutionNode.getRunningNode(item));
	}

	/**
	 * 获取分片项列表。
	 * @return 分片项列表。
	 */
	public List<Integer> getShardingItems() {
		List<Integer> shardingItems = jobScheduler.getShardingService().getLocalHostShardingItems();
		boolean isEnabledReport = configService.isEnabledReport();
		if (configService.isFailover() && isEnabledReport) {
			List<Integer> failoverItems = failoverService.getLocalHostFailoverItems();
			if (!failoverItems.isEmpty()) {
				return failoverItems;
			} else {
				shardingItems.removeAll(failoverService.getLocalHostTakeOffItems());
				return shardingItems;
			}
		} else {
			return shardingItems;
		}
	}
}
