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

package com.vip.saturn.job.sharding.task;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 作业启用，获取该作业的shards，注意要过滤不能运行该作业的executors
 */
public class ExecuteJobEnableShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteJobEnableShardingTask.class);

	private String jobName;

	public ExecuteJobEnableShardingTask(NamespaceShardingService namespaceShardingService, String jobName) {
		super(namespaceShardingService);
		this.jobName = jobName;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {} with {} enable", this.getClass().getSimpleName(), jobName);
	}

	@Override
	protected List<String> notifyEnableJobsPrior() {
		List<String> notifyEnableJobsPrior = new ArrayList<>();
		notifyEnableJobsPrior.add(jobName);
		return notifyEnableJobsPrior;
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
		// 移除已经在Executor运行的该作业的所有Shard
		removeJobShardsOnExecutors(lastOnlineTrafficExecutorList, jobName);

		// 修正该所有executor的对该作业的jobNameList
		fixJobNameList(lastOnlineExecutorList, jobName);

		// 获取该作业的Shard
		shardList.addAll(createShards(jobName, lastOnlineTrafficExecutorList));

		return true;
	}

}
