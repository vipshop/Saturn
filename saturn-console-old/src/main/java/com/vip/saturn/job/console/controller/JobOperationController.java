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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.HistoryJobConfig;
import com.vip.saturn.job.console.mybatis.service.HistoryJobConfigService;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.service.ServerDimensionService;

@RestController
@RequestMapping("job")
public class JobOperationController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobOperationController.class);

	private static final int DEFAULT_RECORD_COUNT = 100;

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private JobOperationService jobOperationService;

	@Resource
	private ServerDimensionService serverDimensionService;

	@Resource
	private ExecutorService executorService;

	@Resource
	private HistoryJobConfigService historyJobConfigService;

	@RequestMapping(value = "toggleJobEnabledState", method = RequestMethod.POST)
	public RequestResult toggleJobEnabledState(HttpServletRequest request, String jobName, Boolean state,
			Boolean confirmed) {
		RequestResult requestResult = new RequestResult();
		if (state == null) {
			requestResult.setSuccess(false);
			requestResult.setMessage("更改的状态有误。");
			return requestResult;
		}
		Boolean isJobEnabled = jobDimensionService.isJobEnabled(jobName);
		if (isJobEnabled == state) {
			if (state) {
				requestResult.setSuccess(false);
				requestResult.setMessage("作业已经是启动状态。");
				return requestResult;
			} else {
				requestResult.setSuccess(false);
				requestResult.setMessage("作业已经是禁用状态。");
				return requestResult;
			}
		}
		if (confirmed != null && !confirmed) {
			try {
				if (state) { // 启用时，检查其依赖的作业是否已经启动
					List<JobConfig> dependentJobsStatus = jobDimensionService.getDependentJobsStatus(jobName);
					if (dependentJobsStatus != null) {
						String unableJobs = "";
						for (JobConfig jobConfig : dependentJobsStatus) {
							if (!jobConfig.getEnabled()) {
								unableJobs += jobConfig.getJobName() + ",";
							}
						}
						if (!unableJobs.equals("")) {
							requestResult.setSuccess(false);
							requestResult.setMessage("该作业依赖的作业（" + unableJobs.substring(0, unableJobs.length() - 1)
									+ "）不处于启用状态，是否继续启用该作业？");
							requestResult.setObj("confirmDependencies");
							return requestResult;
						}
					}
				} else { // 禁用时，检查依赖它的作业是否已经禁用
					List<JobConfig> dependedJobsStatus = jobDimensionService.getDependedJobsStatus(jobName);
					if (dependedJobsStatus != null) {
						String enableJobs = "";
						for (JobConfig jobConfig : dependedJobsStatus) {
							if (jobConfig.getEnabled()) {
								enableJobs += jobConfig.getJobName() + ",";
							}
						}
						if (!enableJobs.equals("")) {
							requestResult.setSuccess(false);
							requestResult.setMessage("依赖该作业的作业（" + enableJobs.substring(0, enableJobs.length() - 1)
									+ "）不处于禁用状态，是否继续禁用作业？");
							requestResult.setObj("confirmDependencies");
							return requestResult;
						}
					}
				}
			} catch (SaturnJobConsoleException e) {
				requestResult.setSuccess(false);
				requestResult.setMessage(e.getMessage());
				return requestResult;
			} catch (Exception e) {
				requestResult.setSuccess(false);
				requestResult.setMessage(e.toString());
				return requestResult;
			}
		}
		JobStatus js = jobDimensionService.getJobStatus(jobName);
		// enabled job
		if (state) {
			if (JobStatus.STOPPED.equals(js)) {
				try {
					jobOperationService.setJobEnabledState(jobName, state);
				} catch (SaturnJobConsoleException e) {
					requestResult.setSuccess(false);
					requestResult.setMessage(e.toString());
					return requestResult;
				}
				requestResult.setSuccess(true);
				return requestResult;
			} else {
				requestResult.setSuccess(false);
				requestResult.setMessage("作业不处于stopped状态，不能启用。");
				return requestResult;
			}
		} else {
			if (JobStatus.RUNNING.equals(js) || JobStatus.READY.equals(js)) {
				try {
					jobOperationService.setJobEnabledState(jobName, state);
				} catch (SaturnJobConsoleException e) {
					requestResult.setSuccess(false);
					requestResult.setMessage(e.toString());
					return requestResult;
				}
				requestResult.setSuccess(true);
				return requestResult;
			} else {
				requestResult.setSuccess(false);
				requestResult.setMessage("作业不处于running或ready状态，不能禁用。");
				return requestResult;
			}
		}
	}

	@RequestMapping(value = "batchToggleJobEnabledState", method = RequestMethod.POST)
	public RequestResult batchToggleJobEnabledState(HttpServletRequest request, String jobNames, Boolean state,
			Boolean confirmed) {
		RequestResult requestResult = new RequestResult();
		if (state == null) {
			requestResult.setSuccess(false);
			requestResult.setMessage("更改的状态有误。");
			return requestResult;
		}
		String[] jobNameArr = jobNames.split(",");
		if (jobNameArr == null || jobNameArr.length == 0) {
			requestResult.setSuccess(false);
			requestResult.setMessage("没有选中任何要操作的作业。");
			return requestResult;
		}
		if (confirmed != null && !confirmed) {
			for (String jobName : jobNameArr) {
				if (jobName != null && jobName.trim().length() > 0) {
					jobName = jobName.trim();
					try {
						if (state) { // 启用时，检查其依赖的作业是否已经启动
							List<JobConfig> dependentJobsStatus = jobDimensionService.getDependentJobsStatus(jobName);
							if (dependentJobsStatus != null) {
								String unableJobs = "";
								for (JobConfig jobConfig : dependentJobsStatus) {
									if (!jobConfig.getEnabled()) {
										unableJobs += jobConfig.getJobName() + ",";
									}
								}
								if (!unableJobs.equals("")) {
									requestResult.setSuccess(false);
									requestResult.setMessage("有作业依赖的作业还没启用，是否继续批量启用作业？");
									requestResult.setObj("confirmDependencies");
									return requestResult;
								}
							}
						} else { // 禁用时，检查依赖它的作业是否已经禁用
							List<JobConfig> dependedJobsStatus = jobDimensionService.getDependedJobsStatus(jobName);
							if (dependedJobsStatus != null) {
								String enableJobs = "";
								for (JobConfig jobConfig : dependedJobsStatus) {
									if (jobConfig.getEnabled()) {
										enableJobs += jobConfig.getJobName();
									}
								}
								if (!enableJobs.equals("")) {
									requestResult.setSuccess(false);
									requestResult.setMessage("有作业被依赖的作业还没禁用，是否继续批量禁用作业？");
									requestResult.setObj("confirmDependencies");
									return requestResult;
								}
							}
						}
					} catch (SaturnJobConsoleException e) {
						requestResult.setSuccess(false);
						requestResult.setMessage(e.getMessage());
						return requestResult;
					} catch (Exception e) {
						requestResult.setSuccess(false);
						requestResult.setMessage(e.toString());
						return requestResult;
					}
				}
			}
		}
		StringBuilder messageSbf = new StringBuilder();
		for (String jobName : jobNameArr) {
			try {
				Boolean isJobEnabled = jobDimensionService.isJobEnabled(jobName);
				if (isJobEnabled == state) {
					if (state) {
						messageSbf.append("作业【" + jobName + "】已经是启动状态，");
					} else {
						messageSbf.append("作业【" + jobName + "】已经是禁用状态，");
					}
				}
				JobStatus js = jobDimensionService.getJobStatus(jobName);
				// enabled job
				if (state) {
					if (JobStatus.STOPPED.equals(js)) {
						jobOperationService.setJobEnabledState(jobName, state);
					} else {
						messageSbf.append("作业【" + jobName + "】不处于stopped状态，不能启用，");
					}
				} else {
					if (JobStatus.RUNNING.equals(js) || JobStatus.READY.equals(js)) {
						jobOperationService.setJobEnabledState(jobName, state);
					} else {
						messageSbf.append("作业【" + jobName + "】不处于running或ready状态，不能禁用，");
					}
				}
			} catch (Exception e) {
				messageSbf.append("操作作业【" + jobName + "】出现内部错误，");
				continue;
			}
		}
		if (messageSbf.length() == 0) {
			requestResult.setSuccess(true);
			return requestResult;
		} else {
			requestResult.setSuccess(false);
			requestResult.setMessage(messageSbf.substring(0, messageSbf.length() - 1)); // 去掉最后一个逗号
			return requestResult;
		}
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
				if (!SaturnConstants.DEAL_SUCCESS.equals(removeOneExecutorMsg)) {
					removeAllExecutorMsg.append(removeOneExecutorMsg).append(",");
				}
			}
			if (StringUtils.isBlank(removeAllExecutorMsg.toString())) {
				return SaturnConstants.DEAL_SUCCESS;
			}
			return removeAllExecutorMsg.substring(0, removeAllExecutorMsg.toString().length() - 1);
		}
		return removeOneExecutor(executor);
	}

	private String removeOneExecutor(String delExecutor) {
		if (ServerStatus.ONLINE.equals(serverDimensionService.getExecutorStatus(delExecutor))) {
			return "无法删除ONLINE的Executor:(" + delExecutor + ")";
		}
		serverDimensionService.removeOffLineExecutor(delExecutor);
		return SaturnConstants.DEAL_SUCCESS;
	}

	@RequestMapping(value = "remove/job", method = RequestMethod.POST)
	public RequestResult removeStoppedJob(String jobName, HttpServletRequest request) throws InterruptedException {
		RequestResult requestResult = new RequestResult();
		JobStatus jobStatus = jobDimensionService.getJobStatus(jobName);
		if (JobStatus.STOPPED.equals(jobStatus)) {
			try {
				executorService.removeJob(jobName);
				requestResult.setSuccess(true);
			} catch (SaturnJobConsoleException e) {
				requestResult.setSuccess(false);
				requestResult.setMessage(e.getMessage());
			}
		} else {
			requestResult.setSuccess(false);
			requestResult.setMessage("作业【" + jobName + "】不处于STOPPED状态，不能删除.");
		}
		return requestResult;
	}

	@RequestMapping(value = "batchRemove/jobs", method = RequestMethod.POST)
	public String batchRemoveStoppedJob(final String jobNames, HttpServletRequest request) throws InterruptedException {
		String[] jobNamesArr = jobNames.split(",");
		if (jobNamesArr == null || jobNamesArr.length == 0) {
			return "批量删除作业为空";
		}
		StringBuilder errorLog = new StringBuilder();
		for (String jobName : jobNamesArr) {
			JobStatus jobStatus = jobDimensionService.getJobStatus(jobName);
			if (JobStatus.STOPPED.equals(jobStatus)) {
				try {
					executorService.removeJob(jobName);
				} catch (SaturnJobConsoleException e) {
					errorLog.append(e.getMessage()).append(",");
				}
				// let zk and the watchers update theirselves.
			} else {
				errorLog.append("作业【" + jobName + "】不处于STOPPED状态，不能删除.").append(",");
				continue;
			}
		}
		if (Strings.isNullOrEmpty(errorLog.toString())) {
			return SaturnConstants.DEAL_SUCCESS;
		}
		if (errorLog.toString().split(",").length != jobNamesArr.length) {
			return errorLog.toString() + "其他作业已成功删除";// 说明有作业已被成功删除，加个后缀提示
		}
		return errorLog.substring(0, errorLog.length() - 1).toString();// 去掉最后一个逗号
	}

	@RequestMapping(value = "runAllOneTime", method = RequestMethod.POST)
	@ResponseBody
	public String runAllOneTime(final JobServer jobServer, HttpServletRequest request) {
		JobStatus js = jobDimensionService.getJobStatus(jobServer.getJobName());
		if (JobStatus.READY.equals(js)) {
			Collection<JobServer> servers = jobDimensionService.getServers(jobServer.getJobName());
			if (servers != null) {
				for (JobServer server : servers) {
					if (ServerStatus.ONLINE.equals(server.getStatus())) {
						jobOperationService.runAtOnceByJobnameAndExecutorName(jobServer.getJobName(),
								server.getExecutorName());
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
		if (jobType != null && (jobType.equals(JobBriefInfo.JobType.MSG_JOB.name()))) {
			boolean jobEnabled = jobDimensionService.isJobEnabled(jobName);
			if (jobEnabled) {
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

	@RequestMapping(value = "loadHistoryConfig", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> loadHistoryConfig(String jobName, String ns, Integer length, HttpSession session)
			throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		HistoryJobConfig history = new HistoryJobConfig();
		history.setJobName(jobName);
		history.setNamespace(ns);
		int total = historyJobConfigService.selectCount(history);
		total = total > DEFAULT_RECORD_COUNT ? DEFAULT_RECORD_COUNT : total;
		PageRequest page = new PageRequest(0, DEFAULT_RECORD_COUNT, Direction.DESC, "last_update_time");
		data.put("data", historyJobConfigService.selectPage(history, page));
		data.put("recordsTotal", total);
		data.put("recordsFiltered", length);
		data.put("draw", 1);
		return data;
	}
}
