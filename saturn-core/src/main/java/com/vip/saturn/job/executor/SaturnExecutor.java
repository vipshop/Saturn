package com.vip.saturn.job.executor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.vip.saturn.job.basic.JobRegistry;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.ShutdownHandler;
import com.vip.saturn.job.basic.TimeoutSchedulerExecutor;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.reg.zookeeper.ZookeeperConfiguration;
import com.vip.saturn.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.AlarmUtils;
import com.vip.saturn.job.utils.LocalHostService;
import com.vip.saturn.job.utils.ResourceUtils;
import com.vip.saturn.job.utils.SaturnUtils;
import com.vip.saturn.job.utils.ScriptPidUtils;
import com.vip.saturn.job.utils.StartCheckUtil;
import com.vip.saturn.job.utils.StartCheckUtil.StartCheckItem;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class SaturnExecutor {

	private static Logger LOGGER;

	private static AtomicBoolean inited = new AtomicBoolean(false);

	private static SaturnExecutorExtension saturnExecutorExtension;

	private ZookeeperRegistryCenter regCenter;

	private EnhancedConnectionStateListener connectionLostListener;

	private String executorName;

	private String namespace;

	private ClassLoader executorClassLoader;

	private ClassLoader jobClassLoader;

	private Runnable shutdownHandler;

	private ZookeeperConfiguration zkConfig;

	private SaturnExecutorService saturnExecutorService;

	private ResetCountService resetCountService;

	private ReentrantLock shutdownLock = new ReentrantLock();

	private volatile boolean isShutdown;

	private volatile boolean needRestart = false;

	private Thread restartThread;

	private ExecutorService raiseAlarmExecutorService;

	private SaturnExecutor(String namespace, String executorName, ClassLoader executorClassLoader,
			ClassLoader jobClassLoader) {
		this.executorName = executorName;
		this.namespace = namespace;
		this.executorClassLoader = executorClassLoader;
		this.jobClassLoader = jobClassLoader;
		this.raiseAlarmExecutorService = Executors
				.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-raise-alarm-thread", false));
		initRestartThread();
		registerShutdownHandler();
	}

	private void initRestartThread() {
		final String restartThreadName = executorName + "-restart-thread";
		this.restartThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						if (isShutdown) {
							return;
						}
						if (needRestart) {
							try {
								needRestart = false;
								execute();
							} catch (InterruptedException e) {
								throw e;
							} catch (Throwable t) {
								needRestart = true;
								LOGGER.error("The executor " + executorName + " restart failed, will retry again.", t);
							}
						}
						Thread.sleep(1000L);
					}
				} catch (InterruptedException e) {
					LOGGER.info("{} is interrupted", restartThreadName);
				}
			}
		}, restartThreadName);
		this.restartThread.setDaemon(false);
		this.restartThread.start();
	}

	/**
	 * 注册退出时资源清理回调
	 */
	private void registerShutdownHandler() {
		shutdownHandler = new Runnable() {
			@Override
			public void run() {
				if (isShutdown) {
					return;
				}
				try {
					shutdownLock.lockInterruptibly();
					try {
						if(isShutdown) {
							return;
						}
						shutdownGracefully0();
						restartThread.interrupt();
						raiseAlarmExecutorService.shutdownNow();
						isShutdown = true;
					} finally {
						shutdownLock.unlock();
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}

		};
		ShutdownHandler.addShutdownCallback(executorName, shutdownHandler);
	}

	/**
	 * SaturnExecutor工厂入口
	 */
	public static SaturnExecutor buildExecutor(String namespace, String executorName, ClassLoader executorClassLoader,
			ClassLoader jobClassLoader) {
		if ("$SaturnSelf".equals(namespace)) {
			throw new RuntimeException("The namespace cannot be $SaturnSelf");
		}
		if (executorName == null || executorName.isEmpty()) {
			String hostName = LocalHostService.getHostName();
			if ("localhost".equals(hostName) || "localhost6".equals(hostName)) {
				throw new RuntimeException(
						"You are using hostName as executorName, it cannot be localhost or localhost6, please configure hostName.");
			}
			executorName = hostName;// NOSONAR
		}
		init(executorName, namespace, executorClassLoader, jobClassLoader);
		return new SaturnExecutor(namespace, executorName, executorClassLoader, jobClassLoader);
	}

	private static void init(String executorName, String namespace, ClassLoader executorClassLoader,
			ClassLoader jobClassLoader) {
		if (!inited.compareAndSet(false, true)) {
			return;
		}
		initExtension(executorName, namespace, executorClassLoader, jobClassLoader);
		saturnExecutorExtension.init(); // will init log, env, etc
		LOGGER = LoggerFactory.getLogger(SaturnExecutor.class);
	}

	private static void initExtension(String executorName, String namespace, ClassLoader executorClassLoader,
			ClassLoader jobClassLoader) {
		try {
			Properties props = ResourceUtils.getResource("properties/saturn-ext.properties");
			String extClass = props.getProperty("saturn.ext");
			if (!Strings.isNullOrEmpty(extClass)) {
				Class<SaturnExecutorExtension> loadClass = (Class<SaturnExecutorExtension>) SaturnExecutor.class
						.getClassLoader().loadClass(extClass);
				Constructor<SaturnExecutorExtension> constructor = loadClass.getConstructor(String.class, String.class,
						ClassLoader.class, ClassLoader.class);
				saturnExecutorExtension = constructor.newInstance(executorName, namespace, executorClassLoader,
						jobClassLoader);
			}
		} catch (Exception e) { // log is not allowed to use, before saturnExecutorExtension.init().
			e.printStackTrace(); // NOSONAR
		}

		initSaturnExecutorExtension(executorName, namespace, executorClassLoader, jobClassLoader);
	}

	private synchronized static void initSaturnExecutorExtension(String executorName, String namespace,
			ClassLoader executorClassLoader, ClassLoader jobClassLoader) {
		if (saturnExecutorExtension == null) {
			saturnExecutorExtension = new SaturnExecutorExtensionDefault(executorName, namespace, executorClassLoader,
					jobClassLoader);
		}
	}

	private String discoverZK() throws Exception {
		int size = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.size();
		if (size == 0) {
			throw new Exception("Please configure the parameter " + SystemEnvProperties.NAME_VIP_SATURN_CONSOLE_URI
					+ " with env or -D");
		}
		for (int i = 0; i < size; i++) {
			String consoleUri = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.get(i);
			String url = consoleUri + "/rest/v1/discoverZk?namespace=" + namespace;
			CloseableHttpClient httpClient = null;
			try {
				httpClient = HttpClientBuilder.create().build();
				HttpGet httpGet = new HttpGet(url);
				RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(10000)
						.build();
				httpGet.setConfig(requestConfig);
				CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
				StatusLine statusLine = httpResponse.getStatusLine();
				String responseBody = EntityUtils.toString(httpResponse.getEntity());
				Integer statusCode = statusLine != null ? statusLine.getStatusCode() : null;
				if (statusLine != null && statusCode == HttpStatus.SC_OK) {
					String connectionString = JSON.parseObject(responseBody, String.class);
					if (StringUtils.isBlank(connectionString)) {
						LOGGER.warn("ZK connection string is blank！");
						continue;
					}

					LOGGER.info("Discover zk connection string successfully. Url: {}, zk connection string: {}", url,
							connectionString);
					return connectionString;
				} else {
					if (responseBody != null && !responseBody.trim().isEmpty()) {
						JSONObject parseObject = JSONObject.parseObject(responseBody);
						String errMsg = parseObject.getString("message");
						LOGGER.warn(
								"Fail to discover zk connection string. Url: {}, response statusCode: {}, response message: {}",
								url, statusCode, errMsg);
					} else {
						LOGGER.warn(
								"Fail to discover zk connection string. Url: {}, response statusCode: {}, response no content",
								url, statusCode);
					}
				}
			} catch (Exception e) {
				LOGGER.error("Fail to discover zk connection. Url: " + url, e);
			} finally {
				if (httpClient != null) {
					try {
						httpClient.close();
					} catch (IOException e) {
						LOGGER.error("Fail to close httpclient.", e);
					}
				}
			}
		}

		throw new Exception(
				"Fail to discover zk connection string! Please make sure that you have added your namespace on Saturn Console.");
	}

	private boolean scheduleJob(String jobName) {
		LOGGER.info("[{}] msg=add new job {} - {}", jobName, executorName, jobName);
		JobConfiguration jobConfig = new JobConfiguration(regCenter, jobName);
		if (jobConfig.getSaturnJobClass() == null) {
			LOGGER.warn("[{}] msg={} - {} the saturnJobClass is null, jobType is {}", jobConfig, executorName, jobName,
					jobConfig.getJobType());
			return false;
		}
		if (jobConfig.isDeleting()) {
			LOGGER.warn("[{}] msg={} - {} the job is on deleting", jobName, executorName, jobName);
			String serverNodePath = JobNodePath.getServerNodePath(jobName, executorName);
			if (regCenter.isExisted(serverNodePath)) {
				regCenter.remove(serverNodePath);
			}
			return false;
		}
		JobScheduler scheduler = new JobScheduler(regCenter, jobConfig);
		scheduler.setSaturnExecutorService(saturnExecutorService);
		return scheduler.init();
	}

	public void execute() throws Exception {
		shutdownLock.lockInterruptibly();

		try {
			if (isShutdown) {
				return;
			}

			long startTime = System.currentTimeMillis();

			shutdown0();

			try {
				StartCheckUtil.add2CheckList(StartCheckItem.ZK, StartCheckItem.UNIQUE, StartCheckItem.JOBKILL);

				LOGGER.info("start to discover zk connection string.");
				String serverLists = discoverZK();
				if (StringUtils.isBlank(serverLists)) {
					LOGGER.error("zk connection string is blank!");
					throw new RuntimeException("zk connection string is blank!");
				}

				serverLists = serverLists.trim();

				try {
					// 验证namespace是否存在
					saturnExecutorExtension.validateNamespaceExisting(serverLists);

					zkConfig = new ZookeeperConfiguration(serverLists, namespace, 1000, 3000, 3);
					regCenter = new ZookeeperRegistryCenter(zkConfig);
					saturnExecutorService = new SaturnExecutorService(regCenter, executorName);
					saturnExecutorService.setJobClassLoader(jobClassLoader);
					saturnExecutorService.setExecutorClassLoader(executorClassLoader);

					// 初始化注册中心
					LOGGER.info("start to init reg center.");
					regCenter.init();
					connectionLostListener = new EnhancedConnectionStateListener(executorName) {
						@Override
						public void onLost() {
							needRestart = true;
							raiseAlarm();
						}
					};
					regCenter.addConnectionStateListener(connectionLostListener);

					StartCheckUtil.setOk(StartCheckUtil.StartCheckItem.ZK);
				} catch (Exception e) {
					StartCheckUtil.setError(StartCheckUtil.StartCheckItem.ZK);
					throw e;
				}

				// 注册作业名
				LOGGER.info("start to check all exist jobs.");
				List<String> zkJobNames = saturnExecutorService.registerJobNames();
				try {
					ScriptPidUtils.checkAllExistJobs(regCenter, zkJobNames);
					StartCheckUtil.setOk(StartCheckUtil.StartCheckItem.JOBKILL);
				} catch (IllegalStateException e) {
					StartCheckUtil.setError(StartCheckUtil.StartCheckItem.JOBKILL);
					throw e;
				}

				// 初始化timeout scheduler
				LOGGER.info("start to create scheduler.");
				TimeoutSchedulerExecutor.createScheduler(executorName);

				// 先注册Executor再启动作业，防止Executor因为一些配置限制而抛异常了，而作业线程已启动，导致作业还运行了一会
				// 注册Executor
				try {
					LOGGER.info("start to register executor.");
					saturnExecutorService.registerExecutor();
					StartCheckUtil.setOk(StartCheckUtil.StartCheckItem.UNIQUE);
				} catch (Exception e) {
					StartCheckUtil.setError(StartCheckUtil.StartCheckItem.UNIQUE);
					throw e;
				}

				// 启动作业
				if (zkJobNames != null) {
					LOGGER.info("start to schedule jobs.");
					Iterator<String> iterator = zkJobNames.iterator();
					while (iterator.hasNext()) {
						String jobName = iterator.next();
						if (scheduleJob(jobName)) {
							LOGGER.info("The job {} initialize successfully", jobName);
						} else {
							iterator.remove();
							LOGGER.warn("The job {} initialize fail", jobName);
						}
					}
				}

				LOGGER.info("start to process the remaining steps.");
				// 添加新增作业时的回调方法
				saturnExecutorService.addNewJobListenerCallback(new ScheduleNewJobCallback() {
					@Override
					public boolean call(String jobName) {
						try {
							return scheduleJob(jobName);
						} catch (Exception e) {
							LOGGER.error(e.getMessage(), e);
							return false;
						}
					}
				});

				// 启动零点清0成功数错误数线程
				resetCountService = new ResetCountService(executorName);
				resetCountService.startRestCountTimer();

				LOGGER.info("The executor {} start successfully which used {} ms", executorName,
						System.currentTimeMillis() - startTime);
			} catch (Throwable t) {
				LOGGER.error("Fail to start executor {}", executorName);
				shutdown0();
				throw t;
			}
		} finally {
			shutdownLock.unlock();
		}
	}

	private void raiseAlarm() {
		LOGGER.info("raise alarm to console for restarting event.");
		raiseAlarmExecutorService.submit(new Runnable() {
			@Override
			public void run() {
				raiseAlarm2Console(namespace, executorName);
			}
		});
	}

	protected void raiseAlarm2Console(String namespace, String executorName) {
		Map<String, Object> alarmInfo = constructAlarmInfo(namespace, executorName);
		try {
			AlarmUtils.raiseAlarm(alarmInfo, namespace);
		} catch (Throwable t) {
			LOGGER.warn("cannot raise alarm", t);
		}
	}

	protected Map<String,Object> constructAlarmInfo(String namespace, String executorName) {
		Map<String, Object> alarmInfo = new HashMap<>();
		alarmInfo.put("executorName", executorName);
		alarmInfo.put("name", "Saturn Event");
		alarmInfo.put("title", "Executor_Restart");
		alarmInfo.put("level", "WARNING");
		alarmInfo.put("message", "Executor_Restart: namespace:["  + namespace + "] executor:[" + executorName + "] restart on " + SaturnUtils.convertTime2FormattedString(System.currentTimeMillis()));

		return alarmInfo;
	}

	private void shutdownAllCountThread() {
		Map<String, JobScheduler> schdMap = JobRegistry.getSchedulerMap().get(executorName);
		if (schdMap != null) {
			Iterator<String> it = schdMap.keySet().iterator();
			while (it.hasNext()) {
				String jobName = it.next();
				JobScheduler jobScheduler = schdMap.get(jobName);
				if (jobScheduler != null) {
					jobScheduler.shutdownCountThread();
				}
			}
		}
	}

	private void shutdownUnfinishJob() {
		Map<String, JobScheduler> schdMap = JobRegistry.getSchedulerMap().get(executorName);
		if (schdMap != null) {
			Iterator<String> it = schdMap.keySet().iterator();
			while (it.hasNext()) {
				String jobName = it.next();
				JobScheduler jobScheduler = schdMap.get(jobName);
				if (jobScheduler != null) {
					if (!regCenter.isConnected() || jobScheduler.getCurrentConf().isEnabled()) {
						LOGGER.info("[{}] msg=job {} is enabled, force shutdown.", jobName, jobName);
						jobScheduler.stopJob(true);
					}
					jobScheduler.shutdown(false);
				}
			}
		}
	}

	/**
	 * Executor关闭
	 */
	private void shutdown0() throws Exception {
		shutdownLock.lockInterruptibly();
		try {
			LOGGER.info("Try to stop executor {}", executorName);
			shutdownUnfinishJob();
			if (saturnExecutorService != null) {
				saturnExecutorService.shutdown();
			}
			if (connectionLostListener != null) {
				connectionLostListener.close();
			}
			if (regCenter != null) {
				regCenter.close();
			}
			JobRegistry.clearExecutor(executorName);
			if (resetCountService != null) {
				resetCountService.shutdownRestCountTimer();
			}
			TimeoutSchedulerExecutor.shutdownScheduler(executorName);
			LOGGER.info("The executor {} is stopped", executorName);
		} finally {
			shutdownLock.unlock();
		}
	}

	/**
	 * Executor优雅退出： 把自己从集群中拿掉，现有的作业不停； 一直到全部作业都执行完毕，再真正退出； 设置一定超时时间，如果超过这个时间仍未退出，则强行中止
	 */
	private void shutdownGracefully0() throws Exception {
		shutdownLock.lockInterruptibly();
		try {
			LOGGER.info("Try to stop executor {} gracefully", executorName);
			shutdownAllCountThread();

			if (saturnExecutorService != null) {
				saturnExecutorService.shutdown();
			}
			if (resetCountService != null) {
				resetCountService.shutdownRestCountTimer();
			}
			// shutdown timeout-watchdog-threadpool
			TimeoutSchedulerExecutor.shutdownScheduler(executorName);
			try {
				blockUntilJobCompletedIfNotTimeout();
				shutdownUnfinishJob();
				JobRegistry.clearExecutor(executorName);
			} finally {
				if (connectionLostListener != null) {
					connectionLostListener.close();
				}
				if (regCenter != null) {
					regCenter.close();
				}
			}
			LOGGER.info("The executor {} is stopped gracefully", executorName);
		} finally {
			shutdownLock.unlock();
		}
	}

	/**
	 * block until all Job is completed if it is not timeout
	 */
	private void blockUntilJobCompletedIfNotTimeout() {
		Map<String, JobScheduler> schdMap = JobRegistry.getSchedulerMap().get(executorName);
		if (schdMap == null) {
			return;
		}
		Set<Entry<String, JobScheduler>> entries = schdMap.entrySet();

		if (CollectionUtils.isEmpty(entries)) {
			return;
		}
		long start = System.currentTimeMillis();

		boolean hasRunning = false;
		do {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				LOGGER.error(e.getMessage(), e);
			}
			for (Entry<String, JobScheduler> entry : entries) {
				JobScheduler jobScheduler = entry.getValue();
				if ("JAVA_JOB".equals(jobScheduler.getCurrentConf().getJobType())) {
					if (jobScheduler.getJob().isRunning()) {
						hasRunning = true;
						break;
					} else {
						hasRunning = false;
					}
				} else if ("SHELL_JOB".equals(jobScheduler.getCurrentConf().getJobType())) {
					if (jobScheduler.getCurrentConf().isEnabled()) {
						if (jobScheduler.getJob().isRunning()) {
							hasRunning = true;
							break;
						} else {
							hasRunning = false;
						}
					}
				} else {
					jobScheduler.stopJob(false);
				}
			}
		} while (hasRunning
				&& System.currentTimeMillis() - start < SystemEnvProperties.VIP_SATURN_SHUTDOWN_TIMEOUT * 1000);

	}

	public void shutdown() throws Exception {
		if (isShutdown) {
			return;
		}
		shutdownLock.lockInterruptibly();
		try {
			if (isShutdown) {
				return;
			}
			shutdown0();
			restartThread.interrupt();
			raiseAlarmExecutorService.shutdownNow();
			ShutdownHandler.removeShutdownCallback(executorName);
			isShutdown = true;
		} finally {
			shutdownLock.unlock();
		}
	}

	public void shutdownGracefully() throws Exception {
		if (isShutdown) {
			return;
		}
		shutdownLock.lockInterruptibly();
		try {
			if (isShutdown) {
				return;
			}
			shutdownGracefully0();
			restartThread.interrupt();
			raiseAlarmExecutorService.shutdownNow();
			ShutdownHandler.removeShutdownCallback(executorName);
			isShutdown = true;
		} finally {
			shutdownLock.unlock();
		}
	}

	public SaturnExecutorService getSaturnExecutorService() {
		return saturnExecutorService;
	}

	public void setSaturnExecutorService(SaturnExecutorService saturnExecutorService) {
		this.saturnExecutorService = saturnExecutorService;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
}
