package com.vip.saturn.job.java;

import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.SaturnSystemErrorGroup;
import com.vip.saturn.job.SaturnSystemReturnCode;
import com.vip.saturn.job.basic.AbstractSaturnJob;
import com.vip.saturn.job.basic.SaturnApi;
import com.vip.saturn.job.basic.SaturnExecutionContext;
import com.vip.saturn.job.exception.JobInitAlarmException;
import com.vip.saturn.job.utils.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SaturnJavaJob extends AbstractSaturnJob {
	private static Logger log = LoggerFactory.getLogger(SaturnJavaJob.class);

	private Map<Integer, ShardingItemFutureTask> futureTaskMap = new HashMap<>();

	private Object jobBusinessInstance = null;

	public JavaShardingItemCallable createCallable(String jobName, Integer item, String itemValue, int timeoutSeconds,
			SaturnExecutionContext shardingContext, AbstractSaturnJob saturnJob) {
		return new JavaShardingItemCallable(jobName, item, itemValue, timeoutSeconds, shardingContext, saturnJob);
	}

	@Override
	public void init() {
		super.init();
		createJobBusinessInstanceIfNecessary();
		getJobVersionIfNecessary();
	}

	private void getJobVersionIfNecessary() {
		if (jobBusinessInstance != null) {
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(saturnExecutorService.getJobClassLoader());
			try {
				String version = (String) jobBusinessInstance.getClass().getMethod("getJobVersion")
						.invoke(jobBusinessInstance);
				setJobVersion(version);
			} catch (Throwable t) {
				// only log the error message as getJobVersion should not block the init process
				LogUtils.error(log, jobName, "error throws during get job version", t);
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		}
	}

	private void createJobBusinessInstanceIfNecessary() {
		String jobClassStr = configService.getJobConfiguration().getJobClass();
		if (StringUtils.isBlank(jobClassStr)) {
			LogUtils.error(log, jobName, "jobClass is not set");
			throw new JobInitAlarmException("jobClass is not set");
		}
		jobClassStr = jobClassStr.trim();
		LogUtils.info(log, jobName, "start to create job business instance, jobClass is {}", jobClassStr);
		if (jobBusinessInstance == null) {
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();
			Thread.currentThread().setContextClassLoader(jobClassLoader);
			try {
				Class<?> jobClass = jobClassLoader.loadClass(jobClassStr);

				jobBusinessInstance = tryToGetSaturnBusinessInstanceFromSaturnApplication(jobClassLoader, jobClass);

				if (jobBusinessInstance == null) {
					try {
						jobBusinessInstance = jobClass.getMethod("getObject").invoke(null);
						if (jobBusinessInstance != null) {
							LogUtils.info(log, jobName, "get job instance from getObject");
						}
					} catch (NoSuchMethodException e) {
						LogUtils.info(log, jobName,
								"the jobClass hasn't the static getObject method, will initialize job by default no arguments constructor method");
					}
				}
				// 业务没有重写getObject方法，BaseSaturnJob会默认返回null
				if (jobBusinessInstance == null) {
					jobBusinessInstance = jobClass.newInstance();
					LogUtils.info(log, jobName, "get job instance from newInstance");
				}
				SaturnApi saturnApi = new SaturnApi(getNamespace(), executorName);
				jobClass.getMethod("setSaturnApi", Object.class).invoke(jobBusinessInstance, saturnApi);
			} catch (Throwable t) {
				throw new JobInitAlarmException(logBusinessExceptionIfNecessary(jobName, t));
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		}
		if (jobBusinessInstance == null) {
			LogUtils.error(log, jobName, "job instance is null");
			throw new JobInitAlarmException("job instance is null");
		}
	}

	@Override
	protected Map<Integer, SaturnJobReturn> handleJob(final SaturnExecutionContext shardingContext) {
		final Map<Integer, SaturnJobReturn> retMap = new HashMap<Integer, SaturnJobReturn>();

		synchronized (futureTaskMap) {
			futureTaskMap.clear();

			final String jobName = shardingContext.getJobName();
			final int timeoutSeconds = getTimeoutSeconds();

			ExecutorService executorService = getExecutorService();

			// 处理自定义参数
			String jobParameter = shardingContext.getJobParameter();

			// shardingItemParameters为参数表解析出来的Key/Value值
			Map<Integer, String> shardingItemParameters = shardingContext.getShardingItemParameters();

			for (final Entry<Integer, String> shardingItem : shardingItemParameters.entrySet()) {
				final Integer key = shardingItem.getKey();
				try {
					String jobValue = shardingItem.getValue();
					final String itemVal = getRealItemValue(jobParameter, jobValue); // 作业分片的对应值

					ShardingItemFutureTask shardingItemFutureTask = new ShardingItemFutureTask(
							createCallable(jobName, key, itemVal, timeoutSeconds, shardingContext, this), null);
					Future<?> callFuture = executorService.submit(shardingItemFutureTask);
					if (timeoutSeconds > 0) {
						TimeoutSchedulerExecutor.scheduleTimeoutJob(shardingContext.getExecutorName(), timeoutSeconds,
								shardingItemFutureTask);
					}
					shardingItemFutureTask.setCallFuture(callFuture);
					futureTaskMap.put(key, shardingItemFutureTask);
				} catch (Throwable t) {
					LogUtils.error(log, jobName, t.getMessage(), t);
					retMap.put(key, new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, t.getMessage(),
							SaturnSystemErrorGroup.FAIL));
				}
			}
		}

		for (Entry<Integer, ShardingItemFutureTask> entry : futureTaskMap.entrySet()) {
			Integer item = entry.getKey();
			ShardingItemFutureTask futureTask = entry.getValue();
			try {
				futureTask.getCallFuture().get();
			} catch (Exception e) {
				LogUtils.error(log, jobName, e.getMessage(), e);
				retMap.put(item, new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, e.getMessage(),
						SaturnSystemErrorGroup.FAIL));
				continue;
			}
			retMap.put(item, futureTask.getCallable().getSaturnJobReturn());
		}

		synchronized (futureTaskMap) {
			futureTaskMap.clear();
		}

		return retMap;
	}

	@Override
	public void abort() {
		super.abort();
		forceStop();
	}

	@Override
	public void forceStop() {
		super.forceStop();
		synchronized (futureTaskMap) {
			for (ShardingItemFutureTask shardingItemFutureTask : futureTaskMap.values()) {
				JavaShardingItemCallable shardingItemCallable = shardingItemFutureTask.getCallable();
				Thread currentThread = shardingItemCallable.getCurrentThread();
				if (currentThread != null) {
					try {
						if (shardingItemCallable.forceStop()) {
							LogUtils.info(log, jobName, "Force stop job, jobName:{}, item:{}", jobName,
									shardingItemCallable.getItem());
							shardingItemCallable.beforeForceStop();
							ShardingItemFutureTask.killRunningBusinessThread(shardingItemFutureTask);
						}
					} catch (Throwable t) {
						LogUtils.error(log, jobName, t.getMessage(), t);
					}
				}
			}
		}
	}

	public SaturnJobReturn doExecution(final String jobName, final Integer key, final String value,
			SaturnExecutionContext shardingContext, final JavaShardingItemCallable callable) throws Throwable {

		String jobClass = shardingContext.getJobConfiguration().getJobClass();
		LogUtils.info(log, jobName, "Running SaturnJavaJob,  jobClass [{}], item [{}]", jobClass, key);

		try {
			Object ret = new JobBusinessClassMethodCaller() {
				@Override
				protected Object internalCall(ClassLoader jobClassLoader, Class<?> saturnJobExecutionContextClazz)
						throws Exception {
					return jobBusinessInstance.getClass()
							.getMethod("handleJavaJob", String.class, Integer.class, String.class,
									saturnJobExecutionContextClazz).invoke(jobBusinessInstance, jobName, key, value,
									callable.getContextForJob(jobClassLoader));
				}
			}.call(jobBusinessInstance, saturnExecutorService);

			SaturnJobReturn saturnJobReturn = (SaturnJobReturn) JavaShardingItemCallable
					.cloneObject(ret, saturnExecutorService.getExecutorClassLoader());
			if (saturnJobReturn != null) {
				callable.setBusinessReturned(true);
			}
			return saturnJobReturn;
		} catch (Exception e) {
			if (e.getCause() instanceof ThreadDeath) {
				throw e.getCause();
			}
			String message = logBusinessExceptionIfNecessary(jobName, e);
			return new SaturnJobReturn(SaturnSystemReturnCode.USER_FAIL, message, SaturnSystemErrorGroup.FAIL);
		}
	}

	public void postTimeout(final String jobName, final Integer key, final String value,
			SaturnExecutionContext shardingContext, final JavaShardingItemCallable callable) {
		callJobBusinessClassMethodTimeoutOrForceStop(jobName, shardingContext, callable, "onTimeout", key, value);
	}

	public void beforeTimeout(final String jobName, final Integer key, final String value,
			SaturnExecutionContext shardingContext, final JavaShardingItemCallable callable) {
		callJobBusinessClassMethodTimeoutOrForceStop(jobName, shardingContext, callable, "beforeTimeout", key, value);
	}

	public void beforeForceStop(final String jobName, final Integer key, final String value,
			SaturnExecutionContext shardingContext, final JavaShardingItemCallable callable) {
		callJobBusinessClassMethodTimeoutOrForceStop(jobName, shardingContext, callable, "beforeForceStop", key, value);
	}

	public void postForceStop(final String jobName, final Integer key, final String value,
			SaturnExecutionContext shardingContext, final JavaShardingItemCallable callable) {
		callJobBusinessClassMethodTimeoutOrForceStop(jobName, shardingContext, callable, "postForceStop", key, value);
	}

	@Override
	public void notifyJobEnabled() {
		callJobBusinessClassMethodEnableOrDisable("onEnabled");
	}

	@Override
	public void notifyJobDisabled() {
		callJobBusinessClassMethodEnableOrDisable("onDisabled");
	}

	private void callJobBusinessClassMethodTimeoutOrForceStop(final String jobName,
			SaturnExecutionContext shardingContext, final JavaShardingItemCallable callable, final String methodName,
			final Integer key, final String value) {
		String jobClass = shardingContext.getJobConfiguration().getJobClass();
		LogUtils.info(log, jobName, "SaturnJavaJob {},  jobClass is {}", methodName, jobClass);

		try {
			new JobBusinessClassMethodCaller() {
				@Override
				protected Object internalCall(ClassLoader jobClassLoader, Class<?> saturnJobExecutionContextClazz)
						throws Exception {
					return jobBusinessInstance.getClass()
							.getMethod(methodName, String.class, Integer.class, String.class,
									saturnJobExecutionContextClazz).invoke(jobBusinessInstance, jobName, key, value,
									callable.getContextForJob(jobClassLoader));
				}
			}.call(jobBusinessInstance, saturnExecutorService);
		} catch (Exception e) {
			logBusinessExceptionIfNecessary(jobName, e);
		}
	}

	private void callJobBusinessClassMethodEnableOrDisable(final String methodName) {
		String jobClass = configService.getJobConfiguration().getJobClass();
		LogUtils.info(log, jobName, "SaturnJavaJob {},  jobClass is {}", methodName, jobClass);
		try {
			new JobBusinessClassMethodCaller() {
				@Override
				protected Object internalCall(ClassLoader jobClassLoader, Class<?> saturnJobExecutionContextClazz)
						throws Exception {
					return jobBusinessInstance.getClass().getMethod(methodName, String.class)
							.invoke(jobBusinessInstance, jobName);
				}
			}.call(jobBusinessInstance, saturnExecutorService);
		} catch (Exception e) {
			logBusinessExceptionIfNecessary(jobName, e);
		}
	}

	@Override
	public void onForceStop(int item) {
	}

	@Override
	public void onTimeout(int item) {
	}

	@Override
	public void onNeedRaiseAlarm(int item, String alarmMessage) {
		// TODO: need to raise alarm by implementor
	}

}
