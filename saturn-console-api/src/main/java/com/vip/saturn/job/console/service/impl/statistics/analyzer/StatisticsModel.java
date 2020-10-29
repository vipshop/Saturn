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

import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;

import java.util.List;

/**
 * @author timmy.hu
 */
public class StatisticsModel {

	private ExecutorInfoAnalyzer executorInfoAnalyzer;

	private OutdatedNoRunningJobAnalyzer outdatedNoRunningJobAnalyzer;

	private UnableFailoverJobAnalyzer unableFailoverJobAnalyzer;

	private Timeout4AlarmJobAnalyzer timeout4AlarmJobAnalyzer;

	private DisabledTimeoutJobAnalyzer disabledTimeoutJobAnalyzer;

	private JobStatisticsAnalyzer jobStatisticsAnalyzer;

	private DomainStatisticsAnalyzer domainStatisticsAnalyzer;

	private ZkClusterDailyCountAnalyzer zkClusterDailyCountAnalyzer;

	public void analyzeExecutor(CuratorFrameworkOp curatorFrameworkOp, RegistryCenterConfiguration config)
			throws Exception {
		executorInfoAnalyzer.analyzeExecutor(curatorFrameworkOp, config);
	}

	public JobStatistics analyzeJobStatistics(CuratorFrameworkOp curatorFrameworkOp, String job, boolean localMode,
			RegistryCenterConfiguration config) throws Exception {
		return jobStatisticsAnalyzer.analyze(curatorFrameworkOp, job, localMode, config, executorInfoAnalyzer);
	}

	public void analyzeShardingCount(CuratorFrameworkOp curatorFrameworkOp, DomainStatistics domainStatistics) {
		domainStatisticsAnalyzer.analyzeShardingCount(curatorFrameworkOp, domainStatistics);
	}

	public void analyzeOutdatedNoRunningJob(CuratorFrameworkOp curatorFrameworkOp, List<AbnormalJob> oldAbnormalJobs,
			String jobName, String jobDegree, RegistryCenterConfiguration config) {
		outdatedNoRunningJobAnalyzer.analyze(curatorFrameworkOp, oldAbnormalJobs, jobName, jobDegree, config);
	}

	public void analyzeTimeout4AlarmJob(CuratorFrameworkOp curatorFrameworkOp,
			List<Timeout4AlarmJob> oldTimeout4AlarmJob, String jobName, String jobDegree,
			RegistryCenterConfiguration config) {
		timeout4AlarmJobAnalyzer.analyze(curatorFrameworkOp, oldTimeout4AlarmJob, jobName, jobDegree, config);
	}

	public void analyzeUnableFailoverJob(CuratorFrameworkOp curatorFrameworkOp, String jobName, String jobDegree,
			RegistryCenterConfiguration config) {
		unableFailoverJobAnalyzer.analyze(curatorFrameworkOp, jobName, jobDegree, config);
	}

	/**
	 * 扫描禁用作业时长超时的作业
	 */
	public void analyzeDisabledTimeoutJob(CuratorFrameworkOp curatorFrameworkOp,
			List<DisabledTimeoutAlarmJob> oldDisabledTimeoutAlarmJobs, String jobName, String jobDegree,
			RegistryCenterConfiguration config) {
		disabledTimeoutJobAnalyzer.analyze(curatorFrameworkOp, oldDisabledTimeoutAlarmJobs, jobName, jobDegree, config);
	}

	public void analyzeProcessCount(DomainStatistics domainStatistics, List<String> jobs,
			RegistryCenterConfiguration config) {
		domainStatisticsAnalyzer.analyzeProcessCount(domainStatistics, zkClusterDailyCountAnalyzer, jobs,
				jobStatisticsAnalyzer.getJobMap(), config);
	}

	public ExecutorInfoAnalyzer getExecutorInfoAnalyzer() {
		return executorInfoAnalyzer;
	}

	public void setExecutorInfoAnalyzer(ExecutorInfoAnalyzer executorInfoAnalyzer) {
		this.executorInfoAnalyzer = executorInfoAnalyzer;
	}

	public OutdatedNoRunningJobAnalyzer getOutdatedNoRunningJobAnalyzer() {
		return outdatedNoRunningJobAnalyzer;
	}

	public void setOutdatedNoRunningJobAnalyzer(OutdatedNoRunningJobAnalyzer outdatedNoRunningJobAnalyzer) {
		this.outdatedNoRunningJobAnalyzer = outdatedNoRunningJobAnalyzer;
	}

	public UnableFailoverJobAnalyzer getUnableFailoverJobAnalyzer() {
		return unableFailoverJobAnalyzer;
	}

	public void setUnableFailoverJobAnalyzer(UnableFailoverJobAnalyzer unableFailoverJobAnalyzer) {
		this.unableFailoverJobAnalyzer = unableFailoverJobAnalyzer;
	}

	public Timeout4AlarmJobAnalyzer getTimeout4AlarmJobAnalyzer() {
		return timeout4AlarmJobAnalyzer;
	}

	public void setTimeout4AlarmJobAnalyzer(Timeout4AlarmJobAnalyzer timeout4AlarmJobAnalyzer) {
		this.timeout4AlarmJobAnalyzer = timeout4AlarmJobAnalyzer;
	}

	public DisabledTimeoutJobAnalyzer getDisabledTimeoutJobAnalyzer() {
		return disabledTimeoutJobAnalyzer;
	}

	public void setDisabledTimeoutJobAnalyzer(DisabledTimeoutJobAnalyzer disabledTimeoutJobAnalyzer) {
		this.disabledTimeoutJobAnalyzer = disabledTimeoutJobAnalyzer;
	}

	public JobStatisticsAnalyzer getJobStatisticsAnalyzer() {
		return jobStatisticsAnalyzer;
	}

	public void setJobStatisticsAnalyzer(JobStatisticsAnalyzer jobStatisticsAnalyzer) {
		this.jobStatisticsAnalyzer = jobStatisticsAnalyzer;
	}

	public DomainStatisticsAnalyzer getDomainStatisticsAnalyzer() {
		return domainStatisticsAnalyzer;
	}

	public void setDomainStatisticsAnalyzer(DomainStatisticsAnalyzer domainStatisticsAnalyzer) {
		this.domainStatisticsAnalyzer = domainStatisticsAnalyzer;
	}

	public ZkClusterDailyCountAnalyzer getZkClusterDailyCountAnalyzer() {
		return zkClusterDailyCountAnalyzer;
	}

	public void setZkClusterDailyCountAnalyzer(ZkClusterDailyCountAnalyzer zkClusterDailyCountAnalyzer) {
		this.zkClusterDailyCountAnalyzer = zkClusterDailyCountAnalyzer;
	}
}
