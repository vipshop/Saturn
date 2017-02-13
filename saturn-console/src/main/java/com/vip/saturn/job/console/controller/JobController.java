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

package com.vip.saturn.job.console.controller;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.ExecutionInfo;
import com.vip.saturn.job.console.domain.JobBriefInfo;
import com.vip.saturn.job.console.domain.JobMode;
import com.vip.saturn.job.console.domain.JobServer;
import com.vip.saturn.job.console.domain.JobSettings;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.utils.CronExpression;

@RestController
@RequestMapping("job")
public class JobController extends AbstractController {

	protected static Logger LOGGER = LoggerFactory.getLogger(JobController.class);

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private JobOperationService jobOperationService;

	@RequestMapping(value = "jobs", method = RequestMethod.GET)
	public Collection<JobBriefInfo> getAllJobsBriefInfo(final ModelMap model, HttpServletRequest request) {
		return jobDimensionService.getAllJobsBriefInfo(null,null);
	}

	@RequestMapping(value = "settings", method = RequestMethod.GET)
	public JobSettings getJobSettings(final String jobName, final Long historyId, final ModelMap model,
			HttpServletRequest request) throws Exception {
		model.put("jobName", jobName);
		model.put("jobStatus", jobDimensionService.getJobStatus(jobName));
		model.put("isJobEnabled", jobDimensionService.getJobStatus(jobName));
		return jobDimensionService.getJobSettings(jobName, getActivatedConfigInSession(request.getSession()));
	}

	@RequestMapping(value = "checkAndForecastCron", method = RequestMethod.POST)
	public RequestResult checkAndForecastCron(final String cron, HttpServletRequest request) {
		RequestResult result = new RequestResult();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (cron != null && !cron.trim().isEmpty()) {
			try {
				CronExpression cronExpression = new CronExpression(cron.trim());
				StringBuilder sb = new StringBuilder(100);
				Date date = new Date();
				for (int i = 0; i < 10; i++) {
					Date next = cronExpression.getNextValidTimeAfter(date);
					if (next != null) {
						sb.append(dateFormat.format(next)).append("<br>");
						date = next;
					}
				}
				result.setSuccess(true);
				if (sb.length() == 0) {
					result.setMessage("Cron maybe describe the past time, the job will never be executed");
				} else {
					if (sb.toString().split("<br>") != null && sb.toString().split("<br>").length >= 10) {
						sb.append("......");
					}
					result.setMessage(sb.toString());
				}
			} catch (ParseException e) {
				result.setSuccess(false);
				result.setMessage(e.toString());
				return result;
			}
		} else {
            result.setSuccess(false);
            result.setMessage("Cron cannot be null or empty");
		}
		return result;
	}

	@RequestMapping(value = "settings", method = RequestMethod.POST)
	public RequestResult updateJobSettings(final JobSettings jobSettings,HttpServletRequest request) {
		RequestResult result = new RequestResult();
		if (!JobStatus.STOPPED.equals(jobDimensionService.getJobStatus(jobSettings.getJobName()))) {
			result.setSuccess(false);
			result.setMessage("The job is not stopped, cannot update it's settings");
			return result;
		}
		JobBriefInfo.JobType jobType = JobBriefInfo.JobType.getJobType(jobSettings.getJobType());
		if(jobType == JobBriefInfo.JobType.JAVA_JOB || jobType == JobBriefInfo.JobType.SHELL_JOB) {
			String cron = jobSettings.getCron();
			if (cron != null && !cron.trim().isEmpty()) {
				try {
					CronExpression.validateExpression(cron.trim());
				} catch (ParseException e) {
					result.setSuccess(false);
					result.setMessage("Cron expression is not valid");
					result.setObj(jobDimensionService.getJobSettings(jobSettings.getJobName(),
							getActivatedConfigInSession(request.getSession())));
					return result;
				}
			} else {
				result.setSuccess(false);
				result.setMessage("The cron cannot be null or empty for cron-job");
				result.setObj(jobDimensionService.getJobSettings(jobSettings.getJobName(), getActivatedConfigInSession(request.getSession())));
				return result;
			}
		}
		if(jobSettings.getJobMode() != null && jobSettings.getJobMode().startsWith(JobMode.SYSTEM_PREFIX)) {
			result.setSuccess(false);
			result.setMessage("The jobMode cannot be start with " + JobMode.SYSTEM_PREFIX);
			result.setObj(jobDimensionService.getJobSettings(jobSettings.getJobName(), getActivatedConfigInSession(request.getSession())));
			return result;
		}
		String returnMsg = jobDimensionService.updateJobSettings(jobSettings,
				getActivatedConfigInSession(request.getSession()));
		if (Strings.isNullOrEmpty(returnMsg)) {
			result.setSuccess(true);
			result.setMessage("update success");
			result.setObj(jobSettings);
		} else {
			result.setSuccess(false);
			result.setMessage(returnMsg);
			result.setObj(jobDimensionService.getJobSettings(jobSettings.getJobName(),
					getActivatedConfigInSession(request.getSession())));
		}
		return result;
	}

	@RequestMapping(value = "servers", method = RequestMethod.GET)
	public Collection<JobServer> getServers(final JobServer jobServer, HttpServletRequest request) {
		return jobDimensionService.getServers(jobServer.getJobName());
	}

	@RequestMapping(value = "execution", method = RequestMethod.GET)
	public Collection<ExecutionInfo> getExecutionInfo(final JobSettings config) {
		return jobDimensionService.getExecutionInfo(config.getJobName());
	}

	/**
	 * 获取作业分片执行日志信息
	 * @param config 请求ExecutionInfo对象
	 * @return ExecutionInfo对象
	 */
	@RequestMapping(value = "logs", method = RequestMethod.GET)
	public ExecutionInfo getLogInfo(final ExecutionInfo config) {
		ExecutionInfo info = jobDimensionService.getExecutionJobLog(config.getJobName(), config.getItem());
		return info;
	}
	
	@RequestMapping(value = "getJobNextFireTime", method = RequestMethod.GET)
    public long getJobNextFireTime(String jobName) {
    	Long calculateJobNextTime = jobDimensionService.calculateJobNextTime(jobName);
        return calculateJobNextTime == null?0l:calculateJobNextTime;
    }
	
	 /**
     * 获取所有作业的分组列表
     */
    @RequestMapping(value = "getAllJobGroups", method = RequestMethod.GET)
    public List<String> getAllJobGroups() {
        return jobDimensionService.getAllJobGroups();
    }
}
