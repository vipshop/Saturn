package com.vip.saturn.job.java;

import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.SaturnSystemErrorGroup;
import com.vip.saturn.job.SaturnSystemReturnCode;
import com.vip.saturn.job.basic.*;
import com.vip.saturn.job.exception.JobException;
import com.vip.saturn.job.internal.config.JobConfiguration;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			SaturnExecutionContext shardingContext,  AbstractSaturnJob saturnJob){
		return new JavaShardingItemCallable(jobName, item, itemValue,
				timeoutSeconds, shardingContext, saturnJob);
	}
	
	@Override
	public void init() throws SchedulerException{
		super.init();
		createJobBusinessInstanceIfNecessary();
		getJobVersionIfNecessary();
	}

	private void getJobVersionIfNecessary() throws SchedulerException {
		if (jobBusinessInstance != null) {
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(saturnExecutorService.getJobClassLoader());
			try {
				String version = (String) jobBusinessInstance.getClass().getMethod("getJobVersion").invoke(jobBusinessInstance);
				setJobVersion(version);
			} catch (Throwable t) {
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, "error throws during get job version"), t);
				throw new SchedulerException(t);
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		}
	}

	private void createJobBusinessInstanceIfNecessary() throws SchedulerException {
		JobConfiguration currentConf = configService.getJobConfiguration();
		String jobClassStr = currentConf.getJobClass();
		if (jobClassStr != null && !jobClassStr.trim().isEmpty()) {
			if (jobBusinessInstance == null) {
				ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
				ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();
				Thread.currentThread().setContextClassLoader(jobClassLoader);
				try {
					Class<?> jobClass = jobClassLoader.loadClass(currentConf.getJobClass());
					try {
						Method getObject = jobClass.getMethod("getObject");
						if (getObject != null) {
							try {
								jobBusinessInstance = getObject.invoke(null);
							} catch (Throwable t) {
								log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, jobClassStr + " getObject error"), t);
							}
						}
					} catch (Exception ex) {//NOSONAR
						//log.error("",ex);
					}

					if (jobBusinessInstance == null) {
						jobBusinessInstance = jobClass.newInstance();
					}
					SaturnApi saturnApi = new SaturnApi(getNamespace(), executorName);
					jobClass.getMethod("setSaturnApi", Object.class).invoke(jobBusinessInstance, saturnApi);
				} catch (Throwable t) {
					log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, "create job business instance error"), t);
					throw new SchedulerException(t);
				} finally {
					Thread.currentThread().setContextClassLoader(oldClassLoader);
				}
			}
		}
		if (jobBusinessInstance == null) {
			throw new SchedulerException("init job business instance failed, the job class is " + jobClassStr);
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

				ShardingItemFutureTask shardingItemFutureTask = new ShardingItemFutureTask(createCallable(jobName, key, itemVal,
						timeoutSeconds, shardingContext, this),null);
				Future<?> callFuture = executorService.submit(shardingItemFutureTask);
				if (timeoutSeconds > 0) {
					TimeoutSchedulerExecutor.scheduleTimeoutJob(shardingContext.getExecutorName(), timeoutSeconds,
							shardingItemFutureTask);
				}
				shardingItemFutureTask.setCallFuture(callFuture);
				futureTaskMap.put(key, shardingItemFutureTask);
			} catch (Throwable t) {
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, t.getMessage()), t);
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
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, e.getMessage()), e);
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

		if (futureTaskMap != null) {
			for (ShardingItemFutureTask shardingItemFutureTask : futureTaskMap.values()) {
				JavaShardingItemCallable shardingItemCallable = shardingItemFutureTask.getCallable();
				Thread currentThread = shardingItemCallable.getCurrentThread();
				if (currentThread != null) {
					try {
						log.info("[{}] msg=force stop {} - {}", jobName, shardingItemCallable.getJobName(),
								shardingItemCallable.getItem());
						if (shardingItemCallable.forceStop()) {
							ShardingItemFutureTask.killRunningBusinessThread(shardingItemFutureTask);
						}
					} catch (Throwable t) {
						log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, t.getMessage()), t);
					}
				}
			}
		}
	}
	
	@Override
	public SaturnJobReturn doExecution(String jobName, Integer key, String value,
			SaturnExecutionContext shardingContext, JavaShardingItemCallable callable) throws Throwable{
		return handleJavaJob(jobName, key, value, shardingContext,callable);
	}

	public SaturnJobReturn handleJavaJob(String jobName, Integer key, String value,
			SaturnExecutionContext shardingContext, JavaShardingItemCallable callable) throws Throwable {

		String jobClass = shardingContext.getJobConfiguration().getJobClass();
		log.info("[{}] msg=Running SaturnJavaJob,  jobClass [{}] ,item [{}]", jobName, jobClass, key);

		try {
			if( jobBusinessInstance == null){
				throw new JobException("the job business instance is not initialized");
			}
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();
			Thread.currentThread().setContextClassLoader(jobClassLoader);
			try {
				Class<?> saturnJobExecutionContextClazz = jobClassLoader
						.loadClass(SaturnJobExecutionContext.class.getCanonicalName());

				Object ret = jobBusinessInstance.getClass()
						.getMethod("handleJavaJob", String.class, Integer.class, String.class,
								saturnJobExecutionContextClazz)
						.invoke(jobBusinessInstance, jobName, key, value, callable.getContextForJob(jobClassLoader));
				SaturnJobReturn saturnJobReturn = (SaturnJobReturn) JavaShardingItemCallable.cloneObject(ret, saturnExecutorService.getExecutorClassLoader());
				if(saturnJobReturn != null) {
					callable.setBusinessReturned(true);
				}
				return saturnJobReturn;
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}

		} catch (Exception e) {
			if (e.getCause() instanceof ThreadDeath) {
				throw e.getCause();
			}
			String message = logBusinessExceptionIfNecessary(jobName, e);
			return new SaturnJobReturn(SaturnSystemReturnCode.USER_FAIL, message, SaturnSystemErrorGroup.FAIL);
		}
	}

	public void postTimeout(String jobName, Integer key, String value, SaturnExecutionContext shardingContext,
			JavaShardingItemCallable callable) {
		String jobClass = shardingContext.getJobConfiguration().getJobClass();
		log.info("[{}] msg=SaturnJavaJob onTimeout,  jobClass is {} ", jobName, jobClass);

		try {
			if( jobBusinessInstance == null){
				throw new JobException("the job business instance is not initialized");
			}
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();
			Thread.currentThread().setContextClassLoader(jobClassLoader);
			try {
				Class<?> saturnJobExecutionContextClazz = jobClassLoader
						.loadClass(SaturnJobExecutionContext.class.getCanonicalName());

				jobBusinessInstance.getClass()
						.getMethod("onTimeout", String.class, Integer.class, String.class,
								saturnJobExecutionContextClazz)
						.invoke(jobBusinessInstance, jobName, key, value, callable.getContextForJob(jobClassLoader));
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		} catch (Exception e) {
			logBusinessExceptionIfNecessary(jobName, e);
		}
	}
	
	public void beforeTimeout(String jobName, Integer key, String value, SaturnExecutionContext shardingContext,
			JavaShardingItemCallable callable) {
		String jobClass = shardingContext.getJobConfiguration().getJobClass();
		log.info("[{}] msg=SaturnJavaJob beforeTimeout,  jobClass is {} ", jobName, jobClass);

		try {
			if( jobBusinessInstance == null){
				throw new JobException("the job business instance is not initialized");
			}
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();
			Thread.currentThread().setContextClassLoader(jobClassLoader);
			try {
				Class<?> saturnJobExecutionContextClazz = jobClassLoader
						.loadClass(SaturnJobExecutionContext.class.getCanonicalName());

				jobBusinessInstance.getClass()
						.getMethod("beforeTimeout", String.class, Integer.class, String.class,
								saturnJobExecutionContextClazz)
						.invoke(jobBusinessInstance, jobName, key, value, callable.getContextForJob(jobClassLoader));
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		} catch (Exception e) {
			logBusinessExceptionIfNecessary(jobName, e);
		}
	}

	public void postForceStop(String jobName, Integer key, String value, SaturnExecutionContext shardingContext,
			JavaShardingItemCallable callable) {
		String jobClass = shardingContext.getJobConfiguration().getJobClass();
		log.info("[{}] msg=SaturnJavaJob postForceStop,  jobClass is {} ", jobName, jobClass);

		try {
			if( jobBusinessInstance == null){
				throw new JobException("the job business instance is not initialized");
			}
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();
			
			Thread.currentThread().setContextClassLoader(jobClassLoader);
			try {
				Class<?> saturnJobExecutionContextClazz = jobClassLoader
						.loadClass(SaturnJobExecutionContext.class.getCanonicalName());

				jobBusinessInstance.getClass()
						.getMethod("postForceStop", String.class, Integer.class, String.class,
								saturnJobExecutionContextClazz)
						.invoke(jobBusinessInstance, jobName, key, value, callable.getContextForJob(jobClassLoader));
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		} catch (Exception e) {
			logBusinessExceptionIfNecessary(jobName, e);
		}
	}

	@Override
	public void notifyJobEnabled() {
		String jobClass = configService.getJobConfiguration().getJobClass();
		log.info("[{}] msg=SaturnJavaJob onEnabled,  jobClass is {} ", jobName, jobClass);
		try {
			if (jobBusinessInstance == null) {
				throw new JobException("the job business instance is not initialized");
			}
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();

			Thread.currentThread().setContextClassLoader(jobClassLoader);
			try {
				jobBusinessInstance.getClass().getMethod("onEnabled", String.class).invoke(jobBusinessInstance, jobName);
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
		} catch (Exception e) {
			logBusinessExceptionIfNecessary(jobName, e);
		}
	}

	@Override
	public void notifyJobDisabled() {
		String jobClass = configService.getJobConfiguration().getJobClass();
		log.info("[{}] msg=SaturnJavaJob onDisabled,  jobClass is {} ", jobName, jobClass);
		try {
			if (jobBusinessInstance == null) {
				throw new JobException("the job business instance is not initialized");
			}
			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader jobClassLoader = saturnExecutorService.getJobClassLoader();

			Thread.currentThread().setContextClassLoader(jobClassLoader);
			try {
				jobBusinessInstance.getClass().getMethod("onDisabled", String.class).invoke(jobBusinessInstance, jobName);
			} finally {
				Thread.currentThread().setContextClassLoader(oldClassLoader);
			}
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
		//TODO: need to raise alarm by implementor
	}

}
