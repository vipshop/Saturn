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

package com.vip.saturn.job.internal.control;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.execution.ExecutionNode;

/**
 * @author chembo.huang
 *
 */
public class ReportService extends AbstractSaturnService {

	static Logger log = LoggerFactory.getLogger(ReportService.class);

	public Map<Integer, ExecutionInfo> infoMap = new HashMap<>();

	public ReportService(JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	public void reportData2Zk() {
		synchronized (infoMap) {
			if (infoMap.size() != 0) {
				List<Integer> shardingItems = jobScheduler.getExecutionContextService().getShardingItems();
				Iterator<Entry<Integer, ExecutionInfo>> iterator = infoMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<Integer, ExecutionInfo> next = iterator.next();
					Integer item = next.getKey();
					ExecutionInfo info = next.getValue();
					if (!shardingItems.contains(item)) {
						log.info(
								"sharding items don't have such item: {}, reporter is going to ignore this executionInfo: {}",
								item, info);
						continue;
					}
					if (info.getLastBeginTime() != null) {
						jobScheduler.getJobNodeStorage().replaceJobNode(ExecutionNode.getLastBeginTimeNode(item),
								info.getLastBeginTime());
					}
					if (info.getLastCompleteTime() != null) {
						jobScheduler.getJobNodeStorage().replaceJobNode(ExecutionNode.getLastCompleteTimeNode(item),
								info.getLastCompleteTime());
					}
					if (info.getNextFireTime() != null) {
						jobScheduler.getJobNodeStorage().replaceJobNode(ExecutionNode.getNextFireTimeNode(item),
								info.getNextFireTime());
					}
					jobScheduler.getJobNodeStorage().replaceJobNode(ExecutionNode.getJobLog(item),
							(info.getJobLog() == null ? "" : info.getJobLog()));
					jobScheduler.getJobNodeStorage().replaceJobNode(ExecutionNode.getJobMsg(item),
							(info.getJobMsg() == null ? "" : info.getJobMsg()));
					log.info("done flushed {} to zk.", info);
				}
				infoMap.clear();
			}
		}
	}

	public void clearInfoMap() {
		synchronized (infoMap) {
			infoMap.clear();
		}
	}

	public void initInfoOnBegin(int item, Long nextFireTime) {
		synchronized (infoMap) {
			ExecutionInfo info = new ExecutionInfo(item, System.currentTimeMillis());
			if (nextFireTime != null) {
				info.setNextFireTime(nextFireTime);
			}
			infoMap.put(item, info);
		}
	}

	public ExecutionInfo getInfoByItem(int item) {
		synchronized (infoMap) {
			return infoMap.get(item);
		}
	}

	public void fillInfoOnAfter(ExecutionInfo info) {
		synchronized (infoMap) {
			infoMap.put(info.getItem(), info);
		}
	}

	public void updateExecutionInfoOnBefore() {
		synchronized (infoMap) {
			infoMap.clear();
		}
	}

}
