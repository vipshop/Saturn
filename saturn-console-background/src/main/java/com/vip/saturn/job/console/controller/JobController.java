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

import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.ExecutionInfo;
import com.vip.saturn.job.console.domain.JobBriefInfo;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobMigrateInfo;
import com.vip.saturn.job.console.domain.JobMode;
import com.vip.saturn.job.console.domain.JobServer;
import com.vip.saturn.job.console.domain.JobSettings;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ExecutorProvided;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.utils.CronExpression;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RestController
@RequestMapping("job")
public class JobController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobController.class);

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private JobOperationService jobOperationService;

	@RequestMapping(value = "jobs", method = RequestMethod.GET)
	public Collection<JobBriefInfo> getAllJobsBriefInfo(final ModelMap model, HttpServletRequest request) {
		return jobDimensionService.getAllJobsBriefInfo(null, null);
	}

	@RequestMapping(value = "settings", method = RequestMethod.GET)
	public JobConfig getJobSettings(final String jobName, final Long historyId, final ModelMap model,
			HttpServletRequest request) throws Exception {
		model.put("jobName", jobName);
		model.put("jobStatus", jobDimensionService.getJobStatus(jobName));
		model.put("isJobEnabled", jobDimensionService.getJobStatus(jobName));
		if (historyId != null) {
			return jobDimensionService.getHistoryJobConfigByHistoryId(historyId);
		}
		return jobDimensionService.getJobSettings(jobName, getActivatedConfigInSession(request.getSession()));
	}

	@RequestMapping(value = "checkAndForecastCron", method = RequestMethod.POST)
	public RequestResult checkAndForecastCron(final String timeZone, final String cron, HttpServletRequest request) {
		RequestResult result = new RequestResult();
		if (timeZone == null || timeZone.trim().isEmpty()) {
			result.setSuccess(false);
			result.setMessage("timeZone cannot be null or empty");
			return result;
		}
		if (cron == null || cron.trim().isEmpty()) {
			result.setSuccess(false);
			result.setMessage("cron cannot be null or empty");
			return result;
		}
		String timeZoneTrim = timeZone.trim();
		String cronTrim = cron.trim();
		if (!SaturnConstants.TIME_ZONE_IDS.contains(timeZoneTrim)) {
			result.setSuccess(false);
			result.setMessage("timeZone is not available");
			return result;
		}
		try {
			TimeZone tz = TimeZone.getTimeZone(timeZoneTrim);
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			dateFormat.setTimeZone(tz);
			CronExpression cronExpression = new CronExpression(cronTrim);
			cronExpression.setTimeZone(tz);

			Map<String, String> obj = new HashMap<>();
			obj.put("timeZone", timeZoneTrim);

			StringBuilder sb = new StringBuilder(100);
			Date now = new Date();
			for (int i = 0; i < 10; i++) {
				Date next = cronExpression.getNextValidTimeAfter(now);
				if (next != null) {
					sb.append(dateFormat.format(next)).append("<br>");
					now = next;
				}
			}
			if (sb.length() == 0) {
				obj.put("times", "Cron maybe describe the past time, the job will never be executed");
			} else {
				if (sb.toString().split("<br>") != null && sb.toString().split("<br>").length >= 10) {
					sb.append("......");
				}
				obj.put("times", sb.toString());
			}

			result.setSuccess(true);
			result.setObj(obj);
		} catch (ParseException e) {
			result.setSuccess(false);
			result.setMessage(e.toString());
			return result;
		}
		return result;
	}

	@RequestMapping(value = "tasksMigrateEnabled", method = RequestMethod.GET)
	public RequestResult tasksMigrateEnabled(final String jobName, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			JobMigrateInfo jobMigrateInfo = jobDimensionService.getJobMigrateInfo(jobName);
			requestResult.setSuccess(true);
			requestResult.setObj(jobMigrateInfo);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		}
		return requestResult;
	}

	@RequestMapping(value = "batchTasksMigrateEnabled", method = RequestMethod.GET)
	public RequestResult batchTasksMigrateEnabled(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			JobMigrateInfo jobMigrateInfo = jobDimensionService.getAllJobMigrateInfo();
			requestResult.setSuccess(true);
			requestResult.setObj(jobMigrateInfo);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		}
		return requestResult;
	}

	@RequestMapping(value = "batchSetPreferExecutorsEnabled", method = RequestMethod.GET)
	public RequestResult batchSetPreferExecutorsEnabled(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			List<ExecutorProvided> allExecutorsOfNamespace = jobDimensionService.getAllExecutorsOfNamespace();
			requestResult.setSuccess(true);
			requestResult.setObj(allExecutorsOfNamespace);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		}
		return requestResult;
	}

	@RequestMapping(value = "batchSetPreferExecutors", method = RequestMethod.POST)
	public RequestResult batchSetPreferExecutors(final String jobNames, final String newPreferExecutors, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (jobNames == null) {
				throw new SaturnJobConsoleException("The jobNames cannot be null");
			}
			if (newPreferExecutors == null) {
				throw new SaturnJobConsoleException("The new prefer executors cannot be null");
			}
			if (jobNames.trim().length() == 0) {
				throw new SaturnJobConsoleException("The jobNames cannot be empty string");
			}
			jobDimensionService.batchSetPreferExecutors(jobNames.trim(), newPreferExecutors.trim());
			requestResult.setSuccess(true);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		}
		return requestResult;
	}

	@RequestMapping(value = "batchMigrateJobNewTask", method = RequestMethod.POST)
	public RequestResult batchMigrateJobNewTask(final String jobNames, final String newTask,
			HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (jobNames == null) {
				throw new SaturnJobConsoleException("The jobNames cannot be null");
			}
			if (newTask == null) {
				throw new SaturnJobConsoleException("The new task cannot be null");
			}
			if (jobNames.trim().length() == 0) {
				throw new SaturnJobConsoleException("The jobNames cannot be empty string");
			}
			if (newTask.trim().length() == 0) {
				throw new SaturnJobConsoleException("The new task cannot be empty string");
			}
			jobDimensionService.batchMigrateJobNewTask(jobNames.trim(), newTask.trim());
			requestResult.setSuccess(true);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		}
		return requestResult;
	}

	@RequestMapping(value = "migrateJobNewTask", method = RequestMethod.POST)
	public RequestResult migrateJobNewTask(final String jobName, final String newTask, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (jobName == null) {
				throw new SaturnJobConsoleException("The jobName cannot be null");
			}
			if (newTask == null) {
				throw new SaturnJobConsoleException("The new task cannot be null");
			}
			if (jobName.trim().length() == 0) {
				throw new SaturnJobConsoleException("The jobName cannot be empty string");
			}
			if (newTask.trim().length() == 0) {
				throw new SaturnJobConsoleException("The new task cannot be empty string");
			}
			jobDimensionService.migrateJobNewTask(jobName.trim(), newTask.trim());
			requestResult.setSuccess(true);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		}
		return requestResult;
	}

	@RequestMapping(value = "settings", method = RequestMethod.POST)
	public RequestResult updateJobSettings(final JobSettings jobSettings, HttpServletRequest request) {
		RequestResult result = new RequestResult();
		if (!JobStatus.STOPPED.equals(jobDimensionService.getJobStatus(jobSettings.getJobName()))) {
			result.setSuccess(false);
			result.setMessage("The job is not stopped, cannot update it's settings");
			return result;
		}
		JobBriefInfo.JobType jobType = JobBriefInfo.JobType.getJobType(jobSettings.getJobType());
		if (jobType == JobBriefInfo.JobType.JAVA_JOB || jobType == JobBriefInfo.JobType.SHELL_JOB) {
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
				result.setObj(jobDimensionService.getJobSettings(jobSettings.getJobName(),
						getActivatedConfigInSession(request.getSession())));
				return result;
			}
		}
		if (jobSettings.getJobMode() != null && jobSettings.getJobMode().startsWith(JobMode.SYSTEM_PREFIX)) {
			result.setSuccess(false);
			result.setMessage("The jobMode cannot be start with " + JobMode.SYSTEM_PREFIX);
			result.setObj(jobDimensionService.getJobSettings(jobSettings.getJobName(),
					getActivatedConfigInSession(request.getSession())));
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

	/**
	 * 获取所有的executor作为优先候选列表
	 */
	@RequestMapping(value = "getAllExecutors", method = RequestMethod.GET)
	public RequestResult getAllExecutors(String jobName) {
		RequestResult requestResult = new RequestResult();
		try {
			List<ExecutorProvided> allExecutors = jobDimensionService.getAllExecutors(jobName);
			requestResult.setObj(allExecutors);
			requestResult.setSuccess(true);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.toString());
		}
		return requestResult;
	}

	/**
	 * 获取所有作业的分组列表
	 */
	@RequestMapping(value = "getAllJobGroups", method = RequestMethod.GET)
	public List<String> getAllJobGroups() {
		return jobDimensionService.getAllJobGroups();
	}
}
