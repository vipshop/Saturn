package com.vip.saturn.job.basic;

import com.google.common.base.Strings;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.SaturnSystemErrorGroup;
import com.vip.saturn.job.SaturnSystemReturnCode;
import com.vip.saturn.job.exception.JobException;
import com.vip.saturn.job.executor.SaturnExecutorService;
import com.vip.saturn.job.internal.statistics.ProcessCountStatistics;
import com.vip.saturn.job.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PropertyPlaceholderHelper;

import java.util.*;

/**
 * Saturn抽象父类
 *
 * @author linzhaoming
 */
public abstract class AbstractSaturnJob extends AbstractElasticJob {

	private static final Logger log = LoggerFactory.getLogger(AbstractSaturnJob.class);

	protected static PropertyPlaceholderHelper placeHolderHelper = new PropertyPlaceholderHelper("{", "}");

	@Override
	protected final void executeJob(final JobExecutionMultipleShardingContext shardingContext) {
		if (!(shardingContext instanceof SaturnExecutionContext)) {
			LogUtils.error(log, jobName, "!!! The context must be instance of SaturnJobExecutionContext !!!");
			return;
		}
		long start = System.currentTimeMillis();

		SaturnExecutionContext saturnContext = (SaturnExecutionContext) shardingContext;
		saturnContext.setSaturnJob(true);

		Map<Integer, SaturnJobReturn> retMap = new HashMap<Integer, SaturnJobReturn>();

		// shardingItemParameters为参数表解析出来的Key/Value值
		Map<Integer, String> shardingItemParameters = saturnContext.getShardingItemParameters();

		// items为需要处理的作业分片
		List<Integer> items = saturnContext.getShardingItems();

		LogUtils.info(log, jobName, "Job {} handle items: {}", jobName, items);

		for (Integer item : items) {
			// 兼容配置错误，如配置3个分片, 参数表配置为0=*, 2=*, 则1分片不会执行
			if (!shardingItemParameters.containsKey(item)) {
				LogUtils.error(log, jobName,
						"The {} item's parameter is not valid, will not execute the business code, please check shardingItemParameters",
						items);
				SaturnJobReturn errRet = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL,
						"Config of parameter is not valid, check shardingItemParameters", SaturnSystemErrorGroup.FAIL);
				retMap.put(item, errRet);
			}
		}

		Map<Integer, SaturnJobReturn> handleJobMap = handleJob(saturnContext);
		if (handleJobMap != null) {
			retMap.putAll(handleJobMap);
		}

		// 汇总修改
		for (Integer item : items) {
			if (item == null) {
				continue;
			}
			SaturnJobReturn saturnJobReturn = retMap.get(item);
			if (saturnJobReturn == null) {
				saturnJobReturn = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL,
						"Can not find the corresponding SaturnJobReturn", SaturnSystemErrorGroup.FAIL);
				retMap.put(item, saturnJobReturn);
			}
			updateExecuteResult(saturnJobReturn, saturnContext, item);
		}

		long end = System.currentTimeMillis();
		LogUtils.info(log, jobName, "{} finished, totalCost={}ms, return={}", jobName, (end - start), retMap);
	}

	protected void updateExecuteResult(SaturnJobReturn saturnJobReturn, SaturnExecutionContext saturnContext,
			int item) {
		int successCount = 0;
		int errorCount = 0;
		if (SaturnSystemReturnCode.JOB_NO_COUNT != saturnJobReturn.getReturnCode()) {
			int errorGroup = saturnJobReturn.getErrorGroup();
			if (errorGroup == SaturnSystemErrorGroup.SUCCESS) {
				successCount++;
			} else {
				if (errorGroup == SaturnSystemErrorGroup.TIMEOUT) {
					onTimeout(item);
				} else if (errorGroup == SaturnSystemErrorGroup.FAIL_NEED_RAISE_ALARM) {
					onNeedRaiseAlarm(item, saturnJobReturn.getReturnMsg());
				}
				errorCount++;
			}
		}
		// 为了展现分片处理失败的状态
		saturnContext.getShardingItemResults().put(item, saturnJobReturn);
		// 执行次数加1
		ProcessCountStatistics.increaseTotalCountDelta(executorName, jobName);
		// 只要有出错和失败的分片，就认为是处理失败; 否则认为处理成功
		if (errorCount == 0 && successCount >= 0) {
			ProcessCountStatistics.incrementProcessSuccessCount(executorName, jobName, successCount);
		} else {
			ProcessCountStatistics.increaseErrorCountDelta(executorName, jobName);
			ProcessCountStatistics.incrementProcessFailureCount(executorName, jobName, errorCount);
		}
	}

	@Override
	protected boolean mayRunDownStream(JobExecutionMultipleShardingContext shardingContext) {
		if (!super.mayRunDownStream(shardingContext)) {
			return false;
		}
		// 只要有一个失败，就不触发下游
		if (shardingContext instanceof SaturnExecutionContext) {
			SaturnExecutionContext saturnContext = (SaturnExecutionContext) shardingContext;
			Map<Integer, SaturnJobReturn> shardingItemResults = saturnContext.getShardingItemResults();
			if (shardingItemResults != null && !shardingItemResults.isEmpty()) {
				Iterator<Map.Entry<Integer, SaturnJobReturn>> iterator = shardingItemResults.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<Integer, SaturnJobReturn> next = iterator.next();
					Integer item = next.getKey();
					SaturnJobReturn saturnJobReturn = next.getValue();
					if (saturnJobReturn.getErrorGroup() != SaturnSystemErrorGroup.SUCCESS) {
						LogUtils.warn(log, jobName,
								"item {} ran unsuccessfully, SaturnJobReturn is {}, wont run downStream", item,
								saturnJobReturn, item);
						return false;
					}
				}
			}
		}
		return true;
	}

	public Properties parseKV(String path) {
		if (Strings.isNullOrEmpty(path)) {
			return null;
		}
		Properties kv = new Properties();
		String[] paths = path.split(",");
		if (paths != null && paths.length > 0) {
			for (String p : paths) {
				String[] tmps = p.split("=");
				if (tmps != null && tmps.length == 2) {
					kv.put(tmps[0].trim(), tmps[1].trim());
				} else {
					LogUtils.warn(log, jobName, "Param is not valid {}", p);
				}
			}
		}

		return kv;
	}

	/**
	 * 获取作业超时时间(秒)
	 */
	public int getTimeoutSeconds() {
		return getConfigService().getTimeoutSeconds();
	}

	/**
	 * 获取替换后的作业分片执行值
	 *
	 * @param jobParameter 作业参数
	 * @param jobValue 作业value
	 * @return 替换后的值
	 */
	protected String getRealItemValue(String jobParameter, String jobValue) {
		// 处理自定义参数
		Properties kvProp = parseKV(jobParameter);
		int kvSize = kvProp != null ? kvProp.size() : 0;
		final String itemVal; // 作业分片的对应值
		if (kvSize > 0) {
			// 有自定义参数, 解析完替换
			itemVal = placeHolderHelper.replacePlaceholders(jobValue, kvProp);
		} else {
			itemVal = jobValue;
		}
		return itemVal.replaceAll("!!", "\"").replaceAll("@@", "=").replaceAll("##", ",");
	}

	public String logBusinessExceptionIfNecessary(String jobName, Throwable t) {
		String message = null;
		if (t instanceof ReflectiveOperationException) {
			Throwable cause = t.getCause();
			if (cause != null) {
				message = cause.toString();
			}
		}
		if (message == null) {
			message = t.toString();
		}
		LogUtils.error(log, jobName, message, t);
		return message;
	}

	/**
	 * 实际处理逻辑
	 *
	 * @param shardingContext 上下文
	 * @return 每个分片返回一个SaturnJobReturn. 若为null，表示执行失败
	 */
	protected abstract Map<Integer, SaturnJobReturn> handleJob(SaturnExecutionContext shardingContext);

	protected abstract static class JobBusinessClassMethodCaller {

		public Object call(Object jobBusinessInstance, SaturnExecutorService saturnExecutorService) throws Exception {
			if (jobBusinessInstance == null) {
				throw new JobException("the job business instance is not initialized");
			}
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();
			Thread.currentThread().setContextClassLoader(jobClassLoader);
			try {
				final Class<?> saturnJobExecutionContextClazz = jobClassLoader
						.loadClass(SaturnJobExecutionContext.class.getCanonicalName());
				return internalCall(jobClassLoader, saturnJobExecutionContextClazz);
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		}

		protected abstract Object internalCall(ClassLoader jobClassLoader, Class<?> saturnJobExecutionContextClazz)
				throws Exception;
	}

	protected Object tryToGetSaturnBusinessInstanceFromSaturnApplication(ClassLoader jobClassLoader,
			Class<?> jobClass) {
		try {
			Object saturnApplication = saturnExecutorService.getSaturnApplication();
			if (saturnApplication != null) {
				Class<?> ssaClazz = jobClassLoader
						.loadClass("com.vip.saturn.job.application.AbstractSaturnApplication");
				if (ssaClazz.isInstance(saturnApplication)) {
					Object jobBusinessInstance = saturnApplication.getClass().getMethod("getJobInstance", Class.class)
							.invoke(saturnApplication, jobClass);
					if (jobBusinessInstance != null) {
						LogUtils.info(log, jobName, "get job instance from {}",
								saturnApplication.getClass().getCanonicalName());
						return jobBusinessInstance;
					}
				}
			}
		} catch (Throwable t) {
			LogUtils.error(log, jobName, "get job instance from SaturnApplication fail", t);
		}
		return null;
	}
}
