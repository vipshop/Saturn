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

package com.vip.saturn.job.internal.execution;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.SaturnSystemErrorGroup;
import com.vip.saturn.job.basic.*;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.internal.control.ExecutionInfo;
import com.vip.saturn.job.internal.control.ReportService;
import com.vip.saturn.job.internal.failover.FailoverNode;
import com.vip.saturn.job.reg.exception.RegException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * 执行作业的服务.
 * @author dylan.xue
 */
public class ExecutionService extends AbstractSaturnService {

	private static final String NO_RETURN_VALUE = "No return value.";

	static Logger log = LoggerFactory.getLogger(ExecutionService.class);

	private ConfigurationService configService;

	private ReportService reportService;

	public ExecutionService(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	@Override
	public void start() {
		configService = jobScheduler.getConfigService();
		reportService = jobScheduler.getReportService();
	}

	/**
	 * 更新当前作业服务器运行时分片的nextFireTime。
	 *
	 * @param shardingItems 作业运行时分片上下文
	 */
	public void updateNextFireTime(final List<Integer> shardingItems) {
		if (!shardingItems.isEmpty()) {
			for (int item : shardingItems) {
				updateNextFireTimeByItem(item);
			}
		}
	}

	private void updateNextFireTimeByItem(int item) {
		if (null == jobScheduler) {
			return;
		}
		Date nextFireTimePausePeriodEffected = jobScheduler.getNextFireTimePausePeriodEffected();
		if (null != nextFireTimePausePeriodEffected) {
			getJobNodeStorage().replaceJobNode(ExecutionNode.getNextFireTimeNode(item),
					nextFireTimePausePeriodEffected.getTime());
		}
	}

	/**
	 * 注册作业启动信息.
	 *
	 * @param jobExecutionShardingContext 作业运行时分片上下文
	 */
	public void registerJobBegin(final JobExecutionMultipleShardingContext jobExecutionShardingContext) {
		List<Integer> shardingItems = jobExecutionShardingContext.getShardingItems();
		if (!shardingItems.isEmpty()) {
			reportService.clearInfoMap();
			Date nextFireTimePausePeriodEffected = jobScheduler.getNextFireTimePausePeriodEffected();
			Long nextFireTime = nextFireTimePausePeriodEffected == null ? null
					: nextFireTimePausePeriodEffected.getTime();
			for (int item : shardingItems) {
				registerJobBeginByItem(item, nextFireTime);
			}
		}
	}

	public void registerJobBeginByItem(int item, Long nextFireTime) {
		if (log.isDebugEnabled()) {
			log.debug("registerJobBeginByItem: " + item);
		}
		boolean isEnabledReport = configService.isEnabledReport();
		if (isEnabledReport) {
			getJobNodeStorage().removeJobNode(ExecutionNode.getCompletedNode(item));
			getJobNodeStorage().fillEphemeralJobNode(ExecutionNode.getRunningNode(item), executorName);
			// 清除完成状态timeout等信息
			cleanSaturnNode(item);
		}

		reportService.initInfoOnBegin(item, nextFireTime);
	}

	public void registerJobCompletedByItem(final JobExecutionMultipleShardingContext jobExecutionShardingContext, int item,
			Date nextFireTimePausePeriodEffected) throws Exception {
		registerJobCompletedControlInfoByItem(jobExecutionShardingContext, item);
		registerJobCompletedReportInfoByItem(jobExecutionShardingContext, item, nextFireTimePausePeriodEffected);
	}

	/**
	 * 作业完成信息注册，此信息用于页面展现。注意，无论作业是否上报状态(对应/config/enabledReport/节点)，都会注册此信息。
	 */
	public void registerJobCompletedReportInfoByItem(
			final JobExecutionMultipleShardingContext jobExecutionShardingContext, int item,
			Date nextFireTimePausePeriodEffected) {
		ExecutionInfo info = reportService.getInfoByItem(item);
		if (info == null) { // old data has been flushed to zk.
			info = new ExecutionInfo(item);
		}
		if (jobExecutionShardingContext instanceof SaturnExecutionContext) {
			// 为了展现分片处理失败的状态
			SaturnExecutionContext saturnContext = (SaturnExecutionContext) jobExecutionShardingContext;
			if (saturnContext.isSaturnJob()) {
				SaturnJobReturn jobRet = saturnContext.getShardingItemResults().get(item);
				if (jobRet != null) {
					int errorGroup = jobRet.getErrorGroup();
					info.setJobMsg(jobRet.getReturnMsg());
					//如果作业执行成功且不展示日志，则不展现log
					if (errorGroup == SaturnSystemErrorGroup.SUCCESS && !configService.showNormalLog()) {
						info.setJobLog(null);
					} else {
						info.setJobLog(saturnContext.getJobLog(item));
					}
				} else {
					info.setJobMsg(NO_RETURN_VALUE);
				}
			}
		}
		if (nextFireTimePausePeriodEffected != null) {
			info.setNextFireTime(nextFireTimePausePeriodEffected.getTime());
		}

		info.setLastCompleteTime(System.currentTimeMillis());
		reportService.fillInfoOnAfter(info);
	}

	/**
	 * 注册作业完成信息.
	 *
	 */
	public void registerJobCompletedControlInfoByItem(
			final JobExecutionMultipleShardingContext jobExecutionShardingContext, int item) {

		boolean isEnabledReport = configService.isEnabledReport();
		if (!isEnabledReport) {
			return;
		}

		updateErrorJobReturnIfPossible(jobExecutionShardingContext, item);
		// create completed node
		createCompletedNode(item);
		// remove running node
		getJobNodeStorage().removeJobNode(ExecutionNode.getRunningNode(item));
	}

	private void createCompletedNode(int item) {
		try {
			getJobNodeStorage().createOrUpdateJobNodeWithValue(ExecutionNode.getCompletedNode(item), executorName);
		} catch (RegException e) {
			log.warn("update job complete node fail.", e);
		}
	}

	private void updateErrorJobReturnIfPossible(JobExecutionMultipleShardingContext jobExecutionShardingContext, int item) {
		if (jobExecutionShardingContext instanceof SaturnExecutionContext) {
			// 为了展现分片处理失败的状态
			SaturnExecutionContext saturnContext = (SaturnExecutionContext) jobExecutionShardingContext;
			if (!saturnContext.isSaturnJob()) {
				return;
			}

			SaturnJobReturn jobRet = saturnContext.getShardingItemResults().get(item);
			try {
				if (jobRet != null) {
					int errorGroup = jobRet.getErrorGroup();
					if (errorGroup == SaturnSystemErrorGroup.TIMEOUT) {
						getJobNodeStorage().createJobNodeIfNeeded(ExecutionNode.getTimeoutNode(item));
					} else if (errorGroup != SaturnSystemErrorGroup.SUCCESS) {
						getJobNodeStorage().createJobNodeIfNeeded(ExecutionNode.getFailedNode(item));
					}
				} else {
					getJobNodeStorage().createJobNodeIfNeeded(ExecutionNode.getFailedNode(item));
				}
			} catch (RegException e) {
				log.warn("update job return fail.", e);
			}
		}
	}

	/**
	 * 清除分配分片序列号的运行状态.
	 *
	 * <p>
	 * 用于作业服务器恢复连接注册中心而重新上线的场景, 先清理上次运行时信息.
	 * </p>
	 *
	 * @param items 需要清理的分片项列表
	 */
	public void clearRunningInfo(final List<Integer> items) {
		for (int each : items) {
			// 已被其他executor接管的正在failover的分片不清理running节点，防止清理节点时触发JobCrashedJobListener导致重新failover了一次
			if (!getJobNodeStorage().isJobNodeExisted(FailoverNode.getExecutionFailoverNode(each))) {
				getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getRunningNode(each));
				// 清除完成状态timeout等信息
				cleanSaturnNode(each);
			}
		}
	}

	/**
	 * 删除作业执行时信息.
	 */
	public void removeExecutionInfo() {
		getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.ROOT);
	}

	/**
	 * 判断该分片是否已完成.
	 *
	 * @param item 运行中的分片路径
	 * @return 该分片是否已完成
	 */
	public boolean isCompleted(final int item) {
		return getJobNodeStorage().isJobNodeExisted(ExecutionNode.getCompletedNode(item));
	}

	public boolean isRunning(final int item) {
		return getJobNodeStorage().isJobNodeExisted(ExecutionNode.getRunningNode(item));
	}

	public boolean isFailover(final int item) {
		return getJobNodeStorage().isJobNodeExisted(FailoverNode.getExecutionFailoverNode(item));
	}

	/**
	 * 判断分片项中是否还有执行中的作业.
	 *
	 * @param items 需要判断的分片项列表
	 * @return 分片项中是否还有执行中的作业
	 */
	public boolean hasRunningItems(final List<Integer> items) {
		for (int each : items) {
			if (getJobNodeStorage().isJobNodeExisted(ExecutionNode.getRunningNode(each))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断是否还有执行中的作业.
	 *
	 * @return 是否还有执行中的作业
	 */
	public boolean hasRunningItems() {
		return hasRunningItems(getAllItems());
	}

	private List<Integer> getAllItems() {
		return Lists.transform(getJobNodeStorage().getJobNodeChildrenKeys(ExecutionNode.ROOT), new Function<String, Integer>() {

			@Override
			public Integer apply(final String input) {
				return Integer.valueOf(input);
			}
		});
	}

	/**
	 * 删除Saturn的作业item信息
	 * @param item 作业分片
	 */
	private void cleanSaturnNode(int item) {
		getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getFailedNode(item));
		getJobNodeStorage().removeJobNodeIfExisted(ExecutionNode.getTimeoutNode(item));
	}

}
