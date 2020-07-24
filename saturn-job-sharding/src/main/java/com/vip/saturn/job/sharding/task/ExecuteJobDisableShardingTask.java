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

import java.util.List;

/**
 * 作业禁用，摘取所有executor运行的该作业的shard，注意要相应地减loadLevel，不需要放回
 */
public class ExecuteJobDisableShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteJobDisableShardingTask.class);

	private String jobName;

	public ExecuteJobDisableShardingTask(NamespaceShardingService namespaceShardingService, String jobName) {
		super(namespaceShardingService);
		this.jobName = jobName;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {} with {} disable", this.getClass().getSimpleName(), jobName);
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) {
		// 摘取所有该作业的Shard
		shardList.addAll(removeJobShardsOnExecutors(lastOnlineTrafficExecutorList, jobName));

		// 如果shardList为空，则没必要进行放回等操作，摘取失败
		if (shardList.isEmpty()) {
			return false;
		}

		return true;
	}

	@Override
	protected void putBackBalancing(List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) {
		// 不做操作
	}

}
