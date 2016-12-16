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

package com.vip.saturn.job.console.service;

import java.util.Collection;
import java.util.List;

import com.vip.saturn.job.console.domain.ExecutionInfo;
import com.vip.saturn.job.console.domain.HealthCheckJobServer;
import com.vip.saturn.job.console.domain.JobBriefInfo;
import com.vip.saturn.job.console.domain.JobMigrateInfo;
import com.vip.saturn.job.console.domain.JobServer;
import com.vip.saturn.job.console.domain.JobSettings;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

public interface JobDimensionService {
	
	JobStatus getJobStatus(final String jobName);
	
    Collection<JobBriefInfo> getAllJobsBriefInfo(String sessionBsKey, String namespace);
    
    String geJobRunningInfo(final String jobName);
    
    String getJobType(final String jobName);
    
    JobSettings getJobSettings(String jobName, RegistryCenterConfiguration configInSession);
    
    String updateJobSettings(JobSettings jobSettings, RegistryCenterConfiguration configInSession);
    
    Collection<JobServer> getServers(String jobName);
    
    void getServersVersion(final String jobName, List<HealthCheckJobServer> allJobServers, RegistryCenterConfiguration registryCenterConfig);
    
    Collection<ExecutionInfo> getExecutionInfo(String jobName);
    
    boolean isJobEnabled(String jobName);
    
    
    /**
     * 获取作业执行日志信息
     * @param jobName 作业名称
     * @param item 分片号
     * @return 日志信息
     */
    ExecutionInfo getExecutionJobLog(String jobName, int item);
    
    /**
	 * 检查否是新版本的executor(新的域)
	 * 旧域：该域下必须至少有一个executor并且所有的executor都没有版本号version节点
	 * 新域：该域下必须至少有一个executor并且所有的executor都有版本号version节点(新版本的executor才在启动时添加了这个节点)
	 * 未知域：该域下没有任何executor或executor中既有新版的又有旧版的Executor
	 * 
	 * @param version executor的版本号
	 * @return 当version参数为空时：1：新域 0：旧域 -1：未知域(无法判断新旧域)
	 *         当version参数不为空时，说明要判断是否大于该版本，仅适用于1.1.0及其之后的版本比较：
	 *         	 2：该域下所有Executor的版本都大于等于指定的版本
     *        	 3：该域下所有Executor的版本都小于指定的版本
     *         	-2：Executor的版本存在大于、等于或小于指定的版本
	 */
    int isNewSaturn(String version);
    
    String getAllExecutors(String jobName);

    JobMigrateInfo getJobMigrateInfo(String jobName) throws SaturnJobConsoleException;

    void migrateJobNewTask(String jobName, String taskNew) throws SaturnJobConsoleException;

	Collection<JobBriefInfo> getAllJobsBriefInfo4Tree();
}
