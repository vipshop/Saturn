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

package com.vip.saturn.job.console.controller;

import java.util.Collection;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.constants.SaturnConstants;
import com.vip.saturn.job.console.domain.JobBriefInfo;
import com.vip.saturn.job.console.domain.JobServer;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.ServerDimensionService;

@RestController
@RequestMapping("job")
public class JobOperationController extends AbstractController {

	protected static Logger AUDITLOGGER = LoggerFactory.getLogger("AUDITLOG");
	protected static Logger LOGGER = LoggerFactory.getLogger(JobOperationController.class);
	protected static final int DEFAULT_RECORD_COUNT = 100;
	
	@Resource
    private JobDimensionService jobDimensionService;
	
    @Resource
    private JobOperationService jobOperationService;
    
    @Resource
    private ServerDimensionService serverDimensionService;

    @Resource
    private ExecutorService executorService;
    
    @Resource
    private RegistryCenterService registryCenterService;
    
    @RequestMapping(value = "toggleJobEnabledState", method = RequestMethod.POST)
    @ResponseBody
  	public String toggleJobEnabledState(HttpServletRequest request, String jobName,Boolean state) {
    	if(state == null) {
			return "更改的状态有误。";
		}
		Boolean isJobEnabled = jobDimensionService.isJobEnabled(jobName);
		if (isJobEnabled == state) {
			if (state) {
				return "作业已经是启动状态。";
			} else {
				return "作业已经是禁用状态。";
			}
		}
		JobStatus js = jobDimensionService.getJobStatus(jobName);
		// enabled job
		if (state) {
			if (JobStatus.STOPPED.equals(js)) {
				jobOperationService.setJobEnabledState(jobName, state);
				return SaturnConstants.DEAL_SUCCESS;
			} else {
				return "作业不处于stopped状态，不能启用。";
			}
		} else {
			if (JobStatus.RUNNING.equals(js) || JobStatus.READY.equals(js)) {
				jobOperationService.setJobEnabledState(jobName, state);
				return SaturnConstants.DEAL_SUCCESS;
			} else {
				return "作业不处于running或ready状态，不能禁用。";
			}
		}
      }
    
    @RequestMapping(value = "batchToggleJobEnabledState", method = RequestMethod.POST)
    @ResponseBody
  	public String batchToggleJobEnabledState(HttpServletRequest request, String jobNames,Boolean state) {
    	if(state == null) {
			return "更改的状态有误。";
		}
		String[] jobNameArr = jobNames.split(",");
		if(jobNameArr == null || jobNameArr.length == 0){
			return "没有选中任何要操作的作业。";
		}
		StringBuilder messageSbf = new StringBuilder();
		for(String jobName : jobNameArr){
			try{
				Boolean isJobEnabled = jobDimensionService.isJobEnabled(jobName);
				if (isJobEnabled == state) {
					if (state) {
						messageSbf.append("作业【" + jobName +"】已经是启动状态，");
					} else {
						messageSbf.append("作业【" + jobName +"】已经是禁用状态，");
					}
				}
				JobStatus js = jobDimensionService.getJobStatus(jobName);
				// enabled job
				if (state) {
					if (JobStatus.STOPPED.equals(js)) {
						jobOperationService.setJobEnabledState(jobName, state);
					} else {
						messageSbf.append("作业【"+jobName+"】不处于stopped状态，不能启用，");
					}
				} else {
					if (JobStatus.RUNNING.equals(js) || JobStatus.READY.equals(js)) {
						jobOperationService.setJobEnabledState(jobName, state);
					} else {
						messageSbf.append("作业【"+jobName+"】不处于running或ready状态，不能禁用，");
					}
				}
			}catch(Exception e){
				AUDITLOGGER.error(e.getMessage(), e);
				messageSbf.append("操作作业【"+jobName+"】出现内部错误，");
				continue;
			}
		}
		if(messageSbf.length() == 0){
			return SaturnConstants.DEAL_SUCCESS;
		}
		return messageSbf.substring(0,messageSbf.length()-1);//去掉最后一个逗号
      }

    @RequestMapping(value = "remove/executor", method = RequestMethod.POST)
    @ResponseBody
  	public String removeExecutor(final String executor, final HttpServletRequest request) {
		if (executor.contains(",")) {
			StringBuilder removeAllExecutorMsg = new StringBuilder();
			// 多个executor
			String[] executors = executor.split(",");
			for (int i = 0; i < executors.length; i++) {
				String delExecutor = executors[i];
				String removeOneExecutorMsg = removeOneExecutor(delExecutor);
				if(!SaturnConstants.DEAL_SUCCESS.equals(removeOneExecutorMsg)){
					removeAllExecutorMsg.append(removeOneExecutorMsg).append(",");
				}
			}
			if(StringUtils.isBlank(removeAllExecutorMsg.toString())){
				return SaturnConstants.DEAL_SUCCESS;
			}
			return removeAllExecutorMsg.substring(0,removeAllExecutorMsg.toString().length()-1);
		}
		return removeOneExecutor(executor);
    }

	private String removeOneExecutor(String delExecutor) {
		if ( ServerStatus.ONLINE.equals(serverDimensionService.getExecutorStatus(delExecutor))) {
			return "无法删除ONLINE的Executor:("+delExecutor+")";
		}
		serverDimensionService.removeOffLineExecutor(delExecutor);
		return SaturnConstants.DEAL_SUCCESS;
	}
    @RequestMapping(value = "remove/job", method = RequestMethod.POST)
	public String removeStoppedJob(final JobServer jobServer, HttpServletRequest request) throws InterruptedException {
    	JobStatus jobStatus = jobDimensionService.getJobStatus(jobServer.getJobName());
		if (JobStatus.STOPPED.equals(jobStatus)) {
			return executorService.removeJob(jobServer.getJobName());
			// let zk and the watchers update theirselves.
		}
		return "作业【"+jobServer.getJobName()+ "】不处于STOPPED状态，不能删除.";
    }
    
    @RequestMapping(value = "batchRemove/jobs", method = RequestMethod.POST)
	public String batchRemoveStoppedJob(final String jobNames, HttpServletRequest request) throws InterruptedException {
		String[] jobNamesArr = jobNames.split(",");
		if(jobNamesArr == null || jobNamesArr.length == 0){
			AUDITLOGGER.warn("batchRemoveJobs is null");
			return "批量删除作业为空";
		}
		StringBuilder errorLog = new StringBuilder();
		for(String jobName : jobNamesArr){
			JobStatus jobStatus = jobDimensionService.getJobStatus(jobName);
			if (JobStatus.STOPPED.equals(jobStatus)) {
				String removeResult = executorService.removeJob(jobName);
				if(!SaturnConstants.DEAL_SUCCESS.equals(removeResult)){
					errorLog.append(removeResult).append(",");
				}
				// let zk and the watchers update theirselves.
			} else {
				errorLog.append("作业【"+jobName+ "】不处于STOPPED状态，不能删除.").append(",");
				continue;
			}
		}
		if(Strings.isNullOrEmpty(errorLog.toString())){
			return SaturnConstants.DEAL_SUCCESS;
		}
		if(errorLog.toString().split(",").length != jobNamesArr.length){
			return errorLog.toString()+"其他作业已成功删除";// 说明有作业已被成功删除，加个后缀提示
		}
		return errorLog.substring(0,errorLog.length()-1).toString();// 去掉最后一个逗号
    }
    
    @RequestMapping(value = "runAllOneTime", method = RequestMethod.POST)
    @ResponseBody
   	public String runAllOneTime(final JobServer jobServer, HttpServletRequest request) {
   		JobStatus js = jobDimensionService.getJobStatus(jobServer.getJobName());
   		if (JobStatus.READY.equals(js)) {
   			Collection<JobServer> servers = jobDimensionService.getServers(jobServer.getJobName());
   			if (servers != null) {
	   			for (JobServer server: servers) {
	   				if (ServerStatus.ONLINE.equals(server.getStatus())) {
	   					jobOperationService.runAtOnceByJobnameAndExecutorName( jobServer.getJobName(), server.getExecutorName());
	   				}
				}
	   			return SaturnConstants.DEAL_SUCCESS;
			}
   			return "无online executor.";
   			
   		} else {
   			return "作业不在ready状态，不能执行立即运行。";
   		}
    }
    
    @RequestMapping(value = "stopAllOneTime", method = RequestMethod.POST)
    @ResponseBody
   	public String stopAllOneTime(final JobServer jobServer, HttpServletRequest request) {
		String jobName = jobServer.getJobName();
		String jobType = jobDimensionService.getJobType(jobName);
		if(jobType != null && (jobType.equals(JobBriefInfo.JobType.MSG_JOB.name()))) {
			boolean jobEnabled = jobDimensionService.isJobEnabled(jobName);
			if(jobEnabled) {
				return "作业不处于禁用状态，不能强行终止作业。";
			} else {
				return stopOnceTime(jobName);
			}
		} else {
			JobStatus js = jobDimensionService.getJobStatus(jobServer.getJobName());
			if (JobStatus.STOPPING.equals(js)) {
				return stopOnceTime(jobName);
			}
			return "作业不处于stopping状态，不能强行终止作业。";
		}
    }

	private String stopOnceTime(String jobName) {
		Collection<JobServer> servers = jobDimensionService.getServers(jobName);
		if (servers == null || servers.size() == 0) {
			return "该作业没有executor接管，不能强行终止作业。";
		}
		for (JobServer server : servers) {
			jobOperationService.stopAtOnceByJobnameAndExecutorName(jobName, server.getExecutorName());
		}
		return SaturnConstants.DEAL_SUCCESS;
	}
    
}
