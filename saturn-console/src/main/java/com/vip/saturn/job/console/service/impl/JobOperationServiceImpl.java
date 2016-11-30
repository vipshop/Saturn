/**
 * Copyright 2016 vip.com.
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

package com.vip.saturn.job.console.service.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.ServerDimensionService;
import com.vip.saturn.job.console.utils.JobNodePath;

@Service
public class JobOperationServiceImpl implements JobOperationService {

    @Resource
    private RegistryCenterService registryCenterService;
    
    @Resource
    private CuratorRepository curatorRepository;

    @Resource
    private ServerDimensionService serverDimensionService;
    
	@Override
	public void runAtOnceByJobnameAndExecutorName(String jobName, String exeName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String path = JobNodePath.getRunOneTimePath(jobName, exeName);
		if(curatorFrameworkOp.checkExists(path)){
			curatorFrameworkOp.delete(path);
		}
		curatorFrameworkOp.create(path);
	}
	
	@Override
	public void stopAtOnceByJobnameAndExecutorName(String jobName, String exeName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String path = JobNodePath.getStopOneTimePath(jobName, exeName);
		if(curatorFrameworkOp.checkExists(path)){
			curatorFrameworkOp.delete(path);
		}
		curatorFrameworkOp.create(path);
	}

	@Override
	public void setJobEnabledState(String jobName, boolean state) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, "enabled"), state);
	}
}
