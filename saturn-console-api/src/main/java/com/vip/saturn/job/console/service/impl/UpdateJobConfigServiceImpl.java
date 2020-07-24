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

package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.integrate.entity.JobConfigInfo;
import com.vip.saturn.job.integrate.exception.UpdateJobConfigException;
import com.vip.saturn.job.integrate.service.UpdateJobConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author timmy.hu
 */
public class UpdateJobConfigServiceImpl implements UpdateJobConfigService {

	@Autowired
	private CurrentJobConfigService currentJobConfigService;

	@Override
	public void batchUpdatePreferList(List<JobConfigInfo> jobConfigInfos) throws UpdateJobConfigException {
		if (CollectionUtils.isEmpty(jobConfigInfos)) {
			return;
		}
		List<JobConfig4DB> currentJobConfigs = new ArrayList<JobConfig4DB>();
		JobConfig4DB currentJobConfig = null;
		for (JobConfigInfo jobConfigInfo : jobConfigInfos) {
			currentJobConfig = new JobConfig4DB();
			currentJobConfig.setNamespace(jobConfigInfo.getNamespace());
			currentJobConfig.setJobName(jobConfigInfo.getJobName());
			currentJobConfig.setPreferList(jobConfigInfo.getPerferList());
			currentJobConfigs.add(currentJobConfig);
		}
		try {
			currentJobConfigService.batchUpdatePreferList(currentJobConfigs);
		} catch (SaturnJobConsoleException e) {
			throw new UpdateJobConfigException(e);
		}
	}

}
