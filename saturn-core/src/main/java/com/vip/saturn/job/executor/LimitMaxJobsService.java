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

package com.vip.saturn.job.executor;

import java.util.List;

import com.vip.saturn.job.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.utils.SystemEnvProperties;

public class LimitMaxJobsService extends AbstractSaturnService {
	static Logger log = LoggerFactory.getLogger(LimitMaxJobsService.class);

	public LimitMaxJobsService(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	/**
	 * 如果当前作业为新增作业，而且超出该域最大作业数量限制，将打印警告日志，返回false；否则返回true。
	 * @param jobName 新增作业名
	 * @return 是否超出
	 */
	public boolean check(String jobName) {
		List<String> childrenKeys = coordinatorRegistryCenter.getChildrenKeys(SaturnExecutorsNode.JOBSNODE_PATH);
		if (childrenKeys != null && !childrenKeys.isEmpty() && !childrenKeys.contains(jobName)
				&& childrenKeys.size() >= SystemEnvProperties.VIP_SATURN_MAX_NUMBER_OF_JOBS) {
			LogUtils.warn(log, jobName, "The jobs that are under the namespace exceed {}",
					SystemEnvProperties.VIP_SATURN_MAX_NUMBER_OF_JOBS);
			return false;
		}
		return true;
	}
}
