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

package com.vip.saturn.job.console.service.impl.statistics.analyzer;

import com.vip.saturn.job.console.domain.DisabledTimeoutAlarmJob;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.helper.DashboardServiceHelper;
import com.vip.saturn.job.console.service.impl.JobServiceImpl;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 禁用作业时长超时的作业分析器
 */
public class DisabledTimeoutJobAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(DisabledTimeoutJobAnalyzer.class);

	private static final FastDateFormat fastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

	private ReportAlarmService reportAlarmService;

	private List<DisabledTimeoutAlarmJob> disabledTimeoutAlarmJobs = new ArrayList<DisabledTimeoutAlarmJob>();

	/**
	 * 查找禁用超时告警作业
	 */
	public void analyze(CuratorFrameworkOp curatorFrameworkOp, List<DisabledTimeoutAlarmJob> oldDisabledTimeoutJobs,
			String jobName, String jobDegree, RegistryCenterConfiguration config) {
		DisabledTimeoutAlarmJob disabledTimeoutAlarmJob = new DisabledTimeoutAlarmJob(jobName, config.getNamespace(),
				config.getNameAndNamespace(), config.getDegree());
		if (isDisabledTimeout(disabledTimeoutAlarmJob, oldDisabledTimeoutJobs, curatorFrameworkOp)) {
			disabledTimeoutAlarmJob.setJobDegree(jobDegree);
			addDisabledTimeoutJob(disabledTimeoutAlarmJob);
		}
	}

	private synchronized void addDisabledTimeoutJob(DisabledTimeoutAlarmJob disabledTimeoutAlarmJob) {
		disabledTimeoutAlarmJobs.add(disabledTimeoutAlarmJob);
	}

	/**
	 * 如果配置了禁用超时告警时间，而且enabled节点设置为false的时长大于该时长，则告警
	 */
	private boolean isDisabledTimeout(DisabledTimeoutAlarmJob disabledTimeoutAlarmJob,
			List<DisabledTimeoutAlarmJob> oldDisabledTimeoutJobs, CuratorFrameworkOp curatorFrameworkOp) {
		String jobName = disabledTimeoutAlarmJob.getJobName();
		int disableTimeoutSeconds = getDisableTimeoutSeconds(curatorFrameworkOp, jobName);
		if (disableTimeoutSeconds <= 0) {
			return false;
		}
		String enableStr = curatorFrameworkOp.getData(SaturnExecutorsNode.getJobConfigEnableNodePath(jobName));
		if (enableStr == null || Boolean.parseBoolean(enableStr)) {
			return false;
		}
		long mtime = curatorFrameworkOp.getMtime(SaturnExecutorsNode.getJobConfigEnableNodePath(jobName));
		if ((System.currentTimeMillis() - mtime) < (disableTimeoutSeconds * 1000)) {
			return false;
		}
		disabledTimeoutAlarmJob.setDisableTimeoutSeconds(disableTimeoutSeconds);
		disabledTimeoutAlarmJob.setDisableTime(mtime);
		disabledTimeoutAlarmJob.setDisableTimeStr(fastDateFormat.format(mtime));
		DisabledTimeoutAlarmJob oldJob = DashboardServiceHelper.findEqualDisabledTimeoutJob(disabledTimeoutAlarmJob,
				oldDisabledTimeoutJobs);
		if (oldJob != null) {
			disabledTimeoutAlarmJob.setRead(oldJob.isRead());
			if (oldJob.getUuid() != null) {
				disabledTimeoutAlarmJob.setUuid(oldJob.getUuid());
			} else {
				disabledTimeoutAlarmJob.setUuid(UUID.randomUUID().toString());
			}
		} else {
			disabledTimeoutAlarmJob.setUuid(UUID.randomUUID().toString());
		}
		if (!disabledTimeoutAlarmJob.isRead()) {
			try {
				reportAlarmService.dashboardLongTimeDisabledJob(disabledTimeoutAlarmJob.getDomainName(), jobName,
						mtime, disableTimeoutSeconds);
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}
		return true;
	}

	/**
	 * 获取作业设置的禁用超时告警时间设置
	 */
	private int getDisableTimeoutSeconds(CuratorFrameworkOp curatorFrameworkOp, String jobName) {
		String disableTimeoutSecondsStr = curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, JobServiceImpl.CONFIG_DISABLE_TIMEOUT_SECONDS));
		int disableTimeoutSeconds = 0;
		if (disableTimeoutSecondsStr != null) {
			try {
				disableTimeoutSeconds = Integer.parseInt(disableTimeoutSecondsStr);
			} catch (NumberFormatException e) {
				log.error(e.getMessage(), e);
			}
		}
		return disableTimeoutSeconds;
	}

	public List<DisabledTimeoutAlarmJob> getDisabledTimeoutAlarmJobList() {
		return new ArrayList<DisabledTimeoutAlarmJob>(disabledTimeoutAlarmJobs);
	}

	public void setReportAlarmService(ReportAlarmService reportAlarmService) {
		this.reportAlarmService = reportAlarmService;
	}
}
