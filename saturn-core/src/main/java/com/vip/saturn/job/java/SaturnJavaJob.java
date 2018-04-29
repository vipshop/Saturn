package com.vip.saturn.job.java;

import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.SaturnSystemErrorGroup;
import com.vip.saturn.job.SaturnSystemReturnCode;
import com.vip.saturn.job.basic.*;
import com.vip.saturn.job.internal.config.JobConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SaturnJavaJob extends CrondJob {
	private static Logger log = LoggerFactory.getLogger(SaturnJavaJob.class);

	private Map<Integer, ShardingItemFutureTask> futureTaskMap;

	private Object jobBusinessInstance = null;

	public JavaShardingItemCallable createCallable(String jobName, Integer item, String itemValue, int timeoutSeconds,
			SaturnExecutionContext shardingContext, AbstractSaturnJob saturnJob) {
		return new JavaShardingItemCallable(jobName, item, itemValue, timeoutSeconds, shardingContext, saturnJob);
	}

	@Override
	public void init() throws SchedulerException {
		super.init();
		createJobBusinessInstanceIfNecessary();
		getJobVersionIfNecessary();
	}

	private void getJobVersionIfNecessary() throws SchedulerException {
		if (jobBusinessInstance != null) {
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(saturnExecutorService.getJobClassLoader());
			try {
				String version = (String) jobBusinessInstance.getClass().getMethod("getJobVersion")
						.invoke(jobBusinessInstance);
				setJobVersion(version);
			} catch (Throwable t) {
				log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName,
						"error throws during get job version"), t);
				throw new SchedulerException(t);
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		}
	}

	private void createJobBusinessInstanceIfNecessary() throws SchedulerException {
		JobConfiguration currentConf = configService.getJobConfiguration();
		String jobClassStr = currentConf.getJobClass();
		if (StringUtils.isBlank(jobClassStr)) {
			throw new SchedulerException("init job business instance failed, the job class is " + jobClassStr);
		}

		if (jobBusinessInstance == null) {
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();
			Thread.currentThread().setContextClassLoader(jobClassLoader);
			try {
				reflectionInvokeInitMethodsOfJobBusinessInstance(currentConf, jobClassStr, jobClassLoader);
			} catch (Throwable t) {
				log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName,
						"create job business instance error"), t);
				throw new SchedulerException(t);
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		}
		if (jobBusinessInstance == null) {
			throw new SchedulerException("init job business instance failed, the job class is " + jobClassStr);
		}
	}

	private void reflectionInvokeInitMethodsOfJobBusinessInstance(JobConfiguration currentConf, String jobClassStr,
			ClassLoader jobClassLoader)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		Class<?> jobClass = jobClassLoader.loadClass(currentConf.getJobClass());
		try {
			Method getObject = jobClass.getMethod("getObject");
			if (getObject != null) {
				reflectionInvokeGetObjectMethod(jobClassStr, getObject);
			}
		} catch (Exception ex) {// NOSONAR
		}

		if (jobBusinessInstance == null) {
			jobBusinessInstance = jobClass.newInstance();
		}
		SaturnApi saturnApi = new SaturnApi(getNamespace(), executorName);
		jobClass.getMethod("setSaturnApi", Object.class).invoke(jobBusinessInstance, saturnApi);
	}

	private void reflectionInvokeGetObjectMethod(String jobClassStr, Method getObject) {
		try {
			jobBusinessInstance = getObject.invoke(null);
		} catch (Throwable t) {
			log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, jobClassStr + " getObject error"),
					t);
		}
	}

	@Override
	protected Map<Integer, SaturnJobReturn> handleJob(final SaturnExecutionContext shardingContext) {
		final Map<Integer, SaturnJobReturn> retMap = new HashMap<Integer, SaturnJobReturn>();

		final String jobName = shardingContext.getJobName();
		final int timeoutSeconds = getTimeoutSeconds();

		ExecutorService executorService = getExecutorService();

		futureTaskMap = new HashMap<Integer, ShardingItemFutureTask>();

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
				log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, t.getMessage()), t);
				retMap.put(key, new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, t.getMessage(),
						SaturnSystemErrorGroup.FAIL));
			}
		}

		for (Entry<Integer, ShardingItemFutureTask> entry : futureTaskMap.entrySet()) {
			Integer item = entry.getKey();
			ShardingItemFutureTask futureTask = entry.getValue();
			try {
				futureTask.getCallFuture().get();
			} catch (Exception e) {
				log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, e.getMessage()), e);
				retMap.put(item, new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, e.getMessage(),
						SaturnSystemErrorGroup.FAIL));
				continue;
			}
			retMap.put(item, futureTask.getCallable().getSaturnJobReturn());
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

		if (futureTaskMap == null) {
			return;
		}

		for (ShardingItemFutureTask shardingItemFutureTask : futureTaskMap.values()) {
			JavaShardingItemCallable shardingItemCallable = shardingItemFutureTask.getCallable();
			Thread currentThread = shardingItemCallable.getCurrentThread();
			if (currentThread != null) {
				try {
					if (shardingItemCallable.forceStop()) {
						log.info("[{}] msg=Force stop job, jobName:{}, item:{}", jobName, jobName,
								shardingItemCallable.getItem());
						shardingItemCallable.beforeForceStop();
						ShardingItemFutureTask.killRunningBusinessThread(shardingItemFutureTask);
					}
				} catch (Throwable t) {
					log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, t.getMessage()), t);
				}
			}
		}
	}

	@Override
	public SaturnJobReturn doExecution(String jobName, Integer key, String value,
			SaturnExecutionContext shardingContext, JavaShardingItemCallable callable) throws Throwable {
		return handleJavaJob(jobName, key, value, shardingContext, callable);
	}

	public SaturnJobReturn handleJavaJob(final String jobName, final Integer key, final String value,
			SaturnExecutionContext shardingContext, final JavaShardingItemCallable callable) throws Throwable {

		String jobClass = shardingContext.getJobConfiguration().getJobClass();
		log.info("[{}] msg=Running SaturnJavaJob,  jobClass [{}], item [{}]", jobName, jobClass, key);

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
		log.info("[{}] msg=SaturnJavaJob {},  jobClass is {}", jobName, methodName, jobClass);

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
		log.info("[{}] msg=SaturnJavaJob {},  jobClass is {}", jobName, methodName, jobClass);
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
