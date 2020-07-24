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
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 域下重排，移除已经存在所有executor，重新获取executors，重新获取作业shards
 */
public class ExecuteAllShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteAllShardingTask.class);

	public ExecuteAllShardingTask(NamespaceShardingService namespaceShardingService) {
		super(namespaceShardingService);
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {} ", this.getClass().getSimpleName());
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJob, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
		// 修正所有executor对所有作业的jobNameList
		for (int j = 0; j < allJobs.size(); j++) {
			fixJobNameList(lastOnlineExecutorList, allJobs.get(j));
		}

		// 获取该域下所有enable作业的所有分片
		for (int i = 0; i < allEnableJob.size(); i++) {
			String jobName = allEnableJob.get(i);
			shardList.addAll(createShards(jobName, lastOnlineTrafficExecutorList));
		}

		return true;
	}

	@Override
	protected List<Executor> customLastOnlineExecutorList() throws Exception {
		if (!isNodeExisted(SaturnExecutorsNode.getExecutorsNodePath())) {
			return new ArrayList<>();
		}
		// 从$SaturnExecutors节点下，获取所有正在运行的Executor
		List<String> zkExecutors = curatorFramework.getChildren().forPath(SaturnExecutorsNode.getExecutorsNodePath());
		if (zkExecutors == null) {
			return new ArrayList<>();
		}

		List<Executor> lastOnlineExecutorList = new ArrayList<>();
		for (int i = 0; i < zkExecutors.size(); i++) {
			String zkExecutor = zkExecutors.get(i);
			if (isNodeExisted(SaturnExecutorsNode.getExecutorIpNodePath(zkExecutor))) {
				byte[] ipData = curatorFramework.getData()
						.forPath(SaturnExecutorsNode.getExecutorIpNodePath(zkExecutor));
				if (ipData != null) {
					Executor executor = new Executor();
					executor.setExecutorName(zkExecutor);
					executor.setIp(new String(ipData, "UTF-8"));
					executor.setNoTraffic(getExecutorNoTraffic(zkExecutor));
					executor.setShardList(new ArrayList<Shard>());
					executor.setJobNameList(new ArrayList<String>());
					lastOnlineExecutorList.add(executor);
				}
			}
		}
		return lastOnlineExecutorList;
	}

	private boolean isNodeExisted(String executorsNodePath) throws Exception {
		return curatorFramework.checkExists().forPath(executorsNodePath) != null;
	}

}
