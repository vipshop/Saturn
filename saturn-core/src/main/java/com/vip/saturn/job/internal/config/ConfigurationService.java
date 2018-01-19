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

package com.vip.saturn.job.internal.config;

import com.google.common.base.Strings;
import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.SaturnConstant;
import com.vip.saturn.job.exception.ShardingItemParametersException;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.JsonUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 弹性化分布式作业配置服务.
 * 
 * 
 */
public class ConfigurationService extends AbstractSaturnService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationService.class);

	private static final String DOUBLE_QUOTE = "\"";

	// 参考http://stackoverflow.com/questions/17963969/java-regex-pattern-split-commna
	private static final String PATTERN = ",(?=(([^\"]*\"){2})*[^\"]*$)";

	private MapType customContextType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class,
			String.class);

	private TimeZone jobTimeZone;

	private ExecutorService executorService;

	private static final Object lock = new Object();

	public ConfigurationService(JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	@Override
	public void start() {
		super.start();
		executorService = Executors.newSingleThreadExecutor(
				new SaturnThreadFactory(executorName + "-" + jobName + "-enabledChanged", false));
	}

	@Override
	public void shutdown() {
		super.shutdown();
		if (executorService != null) {
			executorService.shutdown();
		}
	}

	public void notifyJobEnabledOrNot() {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {
					try {
						if (isJobEnabled()) {
							jobScheduler.getJob().notifyJobEnabled();
						} else {
							jobScheduler.getJob().notifyJobDisabled();
						}
					} catch (Throwable t) {
						LOGGER.error(t.getMessage(), t);
					}
				}
			}
		});
	}

	public void notifyJobEnabled() {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {
					try {
						jobScheduler.getJob().notifyJobEnabled();
					} catch (Throwable t) {
						LOGGER.error(t.getMessage(), t);
					}
				}
			}
		});
	}

	public void notifyJobDisabled() {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {
					try {
						jobScheduler.getJob().notifyJobDisabled();
					} catch (Throwable t) {
						LOGGER.error(t.getMessage(), t);
					}
				}
			}
		});
	}

	/**
	 * 判断是否需要发送Enabled或者Disabled事件
	 * 
	 * 非Local模式的作业，所有的Executor都会收到事件
	 * 
	 * 对于Local模式的作业，如果配置了优先Executor，那么事件只会给优先Executor的服务器发送
	 * 
	 * @return
	 */
	public boolean needSendJobEnabledOrDisabledEvent() {
		if (!this.isLocalMode()) {
			return true;
		}
		List<String> perferList = this.getPreferList();
		if (CollectionUtils.isEmpty(perferList)) {
			return true;
		}
		return perferList.contains(this.executorName);
	}

	/**
	 * 获取作业分片总数.
	 * 
	 * @return 作业分片总数
	 */
	public int getShardingTotalCount() {
		return jobConfiguration.getShardingTotalCount();
	}

	public boolean isLocalMode() {
		return jobConfiguration.isLocalMode();
	}

	/**
	 * 获取分片序列号和个性化参数对照表.<br>
	 * 如果是本地模式的作业，则获取到[-1=xx]
	 * 
	 * @return 分片序列号和个性化参数对照表
	 */
	public Map<Integer, String> getShardingItemParameters() {
		Map<Integer, String> result = new HashMap<>();
		String value = jobConfiguration.getShardingItemParameters();
		if (Strings.isNullOrEmpty(value)) {
			return result;
		}
		// 解释命令行参数
		String[] shardingItemParameters = value.split(PATTERN);
		Map<String, String> result0 = new HashMap<>(shardingItemParameters.length);
		for (String each : shardingItemParameters) {
			String item = "";
			String exec = "";

			int index = each.indexOf('=');
			if (index > -1) {
				item = each.substring(0, index).trim();
				exec = each.substring(index + 1, each.length()).trim();
				// 去掉前后的双引号"
				if (exec.startsWith(DOUBLE_QUOTE)) {
					exec = exec.substring(1);
				}

				if (exec.endsWith(DOUBLE_QUOTE)) {
					exec = exec.substring(0, exec.length() - 1);
				}
			} else {
				throw new ShardingItemParametersException("Sharding item parameters '%s' format error", value);
			}
			result0.put(item, exec);
		}
		if (isLocalMode()) {
			if (result0.containsKey("*")) {
				result.put(-1, result0.get("*"));
			} else {
				throw new ShardingItemParametersException(
						"Sharding item parameters '%s' format error with local mode job, should be *=xx", value);
			}
		} else {
			Iterator<Map.Entry<String, String>> iterator = result0.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, String> next = iterator.next();
				String item = next.getKey();
				String exec = next.getValue();
				try {
					result.put(Integer.valueOf(item), exec);
				} catch (final NumberFormatException ex) {
					throw new ShardingItemParametersException("Sharding item parameters key '%s' is not an integer.",
							item);
				}
			}
		}
		return result;
	}

	/**
	 * 获取作业自定义参数.
	 * 
	 * @return 作业自定义参数
	 */
	public String getJobParameter() {
		return jobConfiguration.getJobParameter();
	}

	/**
	 * 获取作业时区字符串
	 */
	public String getTimeZoneStr() {
		String timeZone = jobConfiguration.getTimeZone();
		if (timeZone == null || timeZone.trim().isEmpty()) {
			return SaturnConstant.TIME_ZONE_ID_DEFAULT;
		}
		return timeZone;
	}

	/**
	 * 获取作业时区对象
	 */
	public TimeZone getTimeZone() {
		String timeZoneStr = jobConfiguration.getTimeZone();
		if (timeZoneStr == null || timeZoneStr.trim().isEmpty()) {
			timeZoneStr = SaturnConstant.TIME_ZONE_ID_DEFAULT;
		}
		if (jobTimeZone != null && timeZoneStr.equals(jobTimeZone.getID())) {
			return jobTimeZone;
		} else {
			jobTimeZone = TimeZone.getTimeZone(timeZoneStr);
			return jobTimeZone;
		}
	}

	/**
	 * 获取作业启动时间的cron表达式.
	 * 
	 * @return 作业启动时间的cron表达式
	 */
	public String getCron() {
		return jobConfiguration.getCron();
	}

	/**
	 * 获取统计作业处理数据数量的间隔时间.
	 *
	 * @return 统计作业处理数据数量的间隔时间
	 */
	public int getProcessCountIntervalSeconds() {
		return jobConfiguration.getProcessCountIntervalSeconds();
	}

	/**
	 * 本机当前时间是否在作业暂停时间段范围内。
	 * <p>
	 * 特别的，无论pausePeriodDate，还是pausePeriodTime，如果解析发生异常，则忽略该节点，视为没有配置该日期或时分段。
	 *
	 * @return 本机当前时间是否在作业暂停时间段范围内.
	 */
	public boolean isInPausePeriod() {
		return isInPausePeriod(new Date());
	}

	/**
	 * 该时间是否在作业暂停时间段范围内。
	 * <p>
	 * 特别的，无论pausePeriodDate，还是pausePeriodTime，如果解析发生异常，则忽略该节点，视为没有配置该日期或时分段。
	 * 
	 * @param date 时间，本机时区的时间
	 * 
	 * @return 该时间是否在作业暂停时间段范围内。
	 */
	public boolean isInPausePeriod(Date date) {
		Calendar calendar = Calendar.getInstance(getTimeZone());
		calendar.setTime(date);
		int M = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH begin from 0.
		int d = calendar.get(Calendar.DAY_OF_MONTH);
		int h = calendar.get(Calendar.HOUR_OF_DAY);
		int m = calendar.get(Calendar.MINUTE);

		boolean dateIn = false;
		String pausePeriodDate = jobConfiguration.getPausePeriodDate();
		boolean pausePeriodDateIsEmpty = (pausePeriodDate == null || pausePeriodDate.trim().isEmpty());
		if (!pausePeriodDateIsEmpty) {
			dateIn = parsePausePeriodTime(M, d, pausePeriodDate);
		}
		boolean timeIn = false;
		String pausePeriodTime = jobConfiguration.getPausePeriodTime();
		boolean pausePeriodTimeIsEmpty = (pausePeriodTime == null || pausePeriodTime.trim().isEmpty());
		if (!pausePeriodTimeIsEmpty) {
			timeIn = parsePausePeriodTime(h, m, pausePeriodTime);
		}

		if (pausePeriodDateIsEmpty) {
			if (pausePeriodTimeIsEmpty) {
				return false;
			}
			return timeIn;
		}

		if (pausePeriodTimeIsEmpty) {
			return dateIn;
		}
		return dateIn && timeIn;
	}

	private boolean parsePausePeriodTime(int h, int m, String pausePeriodTimeOrDateStr) {
		String[] periodsTime = pausePeriodTimeOrDateStr.split(",");
		if (periodsTime == null) {
			return false;
		}

		boolean result = false;
		for (String period : periodsTime) {
			String[] tmp = period.trim().split("-");
			if (tmp != null && tmp.length == 2) {
				String left = tmp[0].trim();
				String right = tmp[1].trim();
				String[] hmLeft = left.split(":");
				String[] hmRight = right.split(":");
				if (hmLeft != null && hmLeft.length == 2 && hmRight != null && hmRight.length == 2) {
					try {
						int hLeft = Integer.parseInt(hmLeft[0]);
						int mLeft = Integer.parseInt(hmLeft[1]);
						int hRight = Integer.parseInt(hmRight[0]);
						int mRight = Integer.parseInt(hmRight[1]);
						result = (h > hLeft || h == hLeft && m >= mLeft) // NOSONAR
								&& (h < hRight || h == hRight && m <= mRight); // NOSONAR
						if (result) {
							return true;
						}
					} catch (NumberFormatException e) {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return result;
	}

	/**
	 * 获取是否开启失效转移.
	 * 
	 * @return 是否开启失效转移
	 */
	public boolean isFailover() {
		return jobConfiguration.isFailover();
	}

	/**
	 * 获取是否开启作业.
	 * 
	 * @return 作业是否开启
	 */
	public boolean isJobEnabled() {
		return jobConfiguration.isEnabled();
	}

	/**
	 * 获取作业是否上报状态。
	 *
	 * @return true, 上报状态；false，不上报状态；
	 */
	public boolean isEnabledReport() {
		return jobConfiguration.isEnabledReport();
	}

	/**
	 * 获取超时时间
	 * 
	 * @return 超时时间
	 */
	public int getTimeoutSeconds() {
		return jobConfiguration.getTimeoutSeconds();
	}

	/**
	 * 获取是否显示正常日志
	 * @return 是否显示正常日志
	 */
	public boolean showNormalLog() {
		return jobConfiguration.isShowNormalLog();
	}

	/**
	 * 获取自定义上下文
	 * @return 获取自定义上下文
	 */
	public Map<String, String> getCustomContext() {
		String jobNodeData = getJobNodeStorage().getJobNodeData(ConfigurationNode.CUSTOM_CONTEXT);
		return toCustomContext(jobNodeData);
	}


	/**
	 * 将str转为map
	 *
	 * @param customContextStr str字符串
	 * @return 自定义上下文map
	 */
	private Map<String, String> toCustomContext(String customContextStr) {
		Map<String, String> customContext = null;
		if (customContextStr != null) {
			customContext = JsonUtils.fromJSON(customContextStr, customContextType);
		}
		if (customContext == null) {
			customContext = new HashMap<>();
		}
		return customContext;
	}

	public String getRawJobType() {
		return jobConfiguration.getJobType();
	}

	/**
	 * 作业接收的queue名字
	 */
	public String getQueueName() {
		return jobConfiguration.getQueueName();
	}

	/**
	 * 执行作业发送的channel名字
	 */
	public String getChannelName() {
		return jobConfiguration.getChannelName();
	}

	public List<String> getPreferList() {
		List<String> result = new ArrayList<String>();
		String prefer = jobConfiguration.getPreferList();
		if (StringUtils.isBlank(prefer)) {
			return result;
		}
		String[] executors = prefer.split(",");
		List<String> allExistsExecutors = this.getAllExistingExecutors();
		for (String executor : executors) {
			executor = executor.trim();
			if (!"".equals(executor)) {
				fillRealPreferListIfIsDockerOrNot(result, executor, allExistsExecutors);
			}
		}
		return result;
	}

	private List<String> getAllExistingExecutors() {
		List<String> allExistsExecutors = new ArrayList<>();
		if (coordinatorRegistryCenter.isExisted(SaturnExecutorsNode.getExecutorsNodePath())) {
			List<String> executors = coordinatorRegistryCenter
					.getChildrenKeys(SaturnExecutorsNode.getExecutorsNodePath());
			if (executors != null) {
				allExistsExecutors.addAll(executors);
			}
		}
		return allExistsExecutors;
	}

	/**
	 * 如果prefer不是docker容器，并且preferList不包含，则直接添加；<br>
	 * 如果prefer是docker容器（以@开头），则prefer为task，获取该task下的所有executor，如果不包含，添加进preferList。
	 */
	private void fillRealPreferListIfIsDockerOrNot(List<String> preferList, String prefer,
			List<String> allExistsExecutors) {
		if (!prefer.startsWith("@")) { // not docker server
			if (!preferList.contains(prefer)) {
				preferList.add(prefer);
			}
		} else { // docker server, get the real executorList by task
			String task = prefer.substring(1);
			for (int i = 0; i < allExistsExecutors.size(); i++) {
				String executor = allExistsExecutors.get(i);
				if (coordinatorRegistryCenter.isExisted(SaturnExecutorsNode.getExecutorTaskNodePath(executor))) {
					String taskData = coordinatorRegistryCenter
							.get(SaturnExecutorsNode.getExecutorTaskNodePath(executor));
					if (taskData != null && task.equals(taskData)) {
						if (!preferList.contains(executor)) {
							preferList.add(executor);
						}
					}
				}
			}
		}
	}

	public boolean isUseDispreferList() {
		return jobConfiguration.isUseDispreferList();
	}
}
