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
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 作业的executor下线，将该executor运行的该作业分片都摘取，如果是本地作业，则移除
 */
public class ExecuteJobServerOfflineShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteJobServerOfflineShardingTask.class);

	private String jobName;

	private String executorName;

	public ExecuteJobServerOfflineShardingTask(NamespaceShardingService namespaceShardingService, String jobName,
			String executorName) {
		super(namespaceShardingService);
		this.jobName = jobName;
		this.executorName = executorName;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {}, jobName is {}, executorName is {}", this.getClass().getSimpleName(), jobName,
				executorName);
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
		boolean localMode = isLocalMode(jobName);

		// Should use lastOnlineExecutorList, because jobName should be removed from jobNameList.
		// But use lastOnlineTrafficExecutorList, the executor maybe cannot be found.
		for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
			Executor executor = lastOnlineExecutorList.get(i);
			if (!executor.getExecutorName().equals(executorName)) {
				continue;
			}
			Iterator<Shard> iterator = executor.getShardList().iterator();
			while (iterator.hasNext()) {
				Shard shard = iterator.next();
				if (shard.getJobName().equals(jobName)) {
					if (!localMode) {
						shardList.add(shard);
					}
					iterator.remove();
				}
			}
			executor.getJobNameList().remove(jobName);
			break;
		}
		return true;
	}

}
