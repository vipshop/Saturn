package com.vip.saturn.job.executor;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.reflect.TypeToken;
import com.vip.saturn.job.basic.JobRegistry;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.ShutdownHandler;
import com.vip.saturn.job.exception.SaturnExecutorException;
import com.vip.saturn.job.exception.SaturnExecutorExceptionCode;
import com.vip.saturn.job.java.TimeoutSchedulerExecutor;
import com.vip.saturn.job.reg.zookeeper.ZookeeperConfiguration;
import com.vip.saturn.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.*;
import com.vip.saturn.job.utils.StartCheckUtil.StartCheckItem;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
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
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class SaturnExecutor {

	private static final String DISCOVER_INFO_ZK_CONN_STR = "zkConnStr";

	private static final String SATURN_PROPERTY_FILE_PATH = "saturn.properties";

	private static final String SATURN_APPLICATION_CLASS = "com.vip.saturn.job.application.SaturnApplication";

	private static Logger log;

	private static AtomicBoolean inited = new AtomicBoolean(false);

	private static SaturnExecutorExtension saturnExecutorExtension;

	private ZookeeperRegistryCenter regCenter;

	private EnhancedConnectionStateListener connectionLostListener;

	private String executorName;

	private String namespace;

	private ClassLoader executorClassLoader;

	private ClassLoader jobClassLoader;

	private Object saturnApplication;

	private SaturnExecutorService saturnExecutorService;

	private ResetCountService resetCountService;

	private PeriodicTruncateNohupOutService periodicTruncateNohupOutService;

	private ReentrantLock shutdownLock = new ReentrantLock();

	private volatile boolean isShutdown;

	private volatile boolean needRestart = false;

	private Thread restartThread;

	private ExecutorService raiseAlarmExecutorService;

	private ExecutorService shutdownJobsExecutorService;

	private SaturnExecutor(String namespace, String executorName, ClassLoader executorClassLoader,
			ClassLoader jobClassLoader, Object saturnApplication) {
		this.executorName = executorName;
		this.namespace = namespace;
		this.executorClassLoader = executorClassLoader;
		this.jobClassLoader = jobClassLoader;
		this.saturnApplication = saturnApplication;
		this.raiseAlarmExecutorService = Executors
				.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-raise-alarm-thread", false));
		this.shutdownJobsExecutorService = Executors
				.newCachedThreadPool(new SaturnThreadFactory(executorName + "-shutdownJobSchedulers-thread", true));
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
								LogUtils.error(log, LogEvents.ExecutorEvent.REINIT,
										"Executor {} reinitialize failed, will retry again", executorName, t);
							}
						}
						Thread.sleep(1000L);
					}
				} catch (InterruptedException e) {
					LogUtils.info(log, LogEvents.ExecutorEvent.REINIT, "{} is interrupted", restartThreadName);
					Thread.currentThread().interrupt();
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
		Runnable shutdownHandler = new Runnable() {
			@Override
			public void run() {
				if (isShutdown) {
					return;
				}
				try {
					shutdownLock.lockInterruptibly();
					try {
						if (isShutdown) {
							return;
						}
						shutdownGracefully0();
						restartThread.interrupt();
						raiseAlarmExecutorService.shutdownNow();
						shutdownJobsExecutorService.shutdownNow();
						isShutdown = true;
					} finally {
						shutdownLock.unlock();
					}
				} catch (Exception e) {
					LogUtils.error(log, LogEvents.ExecutorEvent.GRACEFUL_SHUTDOWN, e.getMessage(), e);
				}
			}

		};
		ShutdownHandler.addShutdownCallback(executorName, shutdownHandler);
	}

	/**
	 * SaturnExecutor工厂入口
	 */
	public static SaturnExecutor buildExecutor(String namespace, String executorName, ClassLoader executorClassLoader,
			ClassLoader jobClassLoader, Object saturnApplication) {
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
		if (saturnApplication == null) {
			saturnApplication = validateAndLoadSaturnApplication(jobClassLoader);
		}
		return new SaturnExecutor(namespace, executorName, executorClassLoader, jobClassLoader, saturnApplication);
	}

	/*
	 * Try to parse the SaturnApplication from saturn.properties. If SaturnApplication is defined then call the method 'init'.
	 * This method should be called after logger is initialized.
	 */
	private static Object validateAndLoadSaturnApplication(ClassLoader jobClassLoader) {
		try {
			Properties properties = getSaturnProperty(jobClassLoader);
			if (properties == null) {
				return null;
			}
			String appClassStr = properties.getProperty("app.class");
			if (StringUtils.isBlank(appClassStr)) {
				return null;
			}

			appClassStr = appClassStr.trim();
			ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(jobClassLoader);
				Class<?> appClass = jobClassLoader.loadClass(appClassStr);
				Class<?> saturnApplicationClass = jobClassLoader.loadClass(SATURN_APPLICATION_CLASS);
				if (saturnApplicationClass.isAssignableFrom(appClass)) {
					Object saturnApplication = appClass.newInstance();
					appClass.getMethod("init").invoke(saturnApplication);
					LogUtils.info(log, LogEvents.ExecutorEvent.INIT, "SaturnApplication init successfully");
					return saturnApplication;
				} else {
					throw new RuntimeException(
							"the app.class " + appClassStr + " must be instance of " + SATURN_APPLICATION_CLASS);
				}
			} finally {
				Thread.currentThread().setContextClassLoader(oldCL);
			}
		} catch (RuntimeException e) {
			LogUtils.error(log, LogEvents.ExecutorEvent.INIT, "Fail to load SaturnApplication", e);
			throw e;
		} catch (Exception e) {
			LogUtils.error(log, LogEvents.ExecutorEvent.INIT, "Fail to load SaturnApplication", e);
			throw new RuntimeException(e);
		}
	}

	private static Properties getSaturnProperty(ClassLoader jobClassLoader) throws IOException {
		Enumeration<URL> resources = jobClassLoader.getResources(SATURN_PROPERTY_FILE_PATH);
		int count = 0;
		if (resources == null || !resources.hasMoreElements()) {
			return null;
		} else {
			while (resources.hasMoreElements()) {
				resources.nextElement();
				count++;
			}
		}
		if (count == 0) {
			return null;
		}
		if (count > 1) {
			throw new RuntimeException("the file [" + SATURN_PROPERTY_FILE_PATH + "] shouldn't exceed one");
		}

		Properties properties = new Properties();
		InputStream is = null;
		try {
			is = jobClassLoader.getResourceAsStream(SATURN_PROPERTY_FILE_PATH);
			properties.load(is);
		} finally {
			if (is != null) {
				is.close();
			}
		}
		return properties;
	}

	private static void init(String executorName, String namespace, ClassLoader executorClassLoader,
			ClassLoader jobClassLoader) {
		if (!inited.compareAndSet(false, true)) {
			return;
		}
		initExtension(executorName, namespace, executorClassLoader, jobClassLoader);
		saturnExecutorExtension.init(); // will init log, env, etc
		log = LoggerFactory.getLogger(SaturnExecutor.class);
	}

	private static synchronized void initExtension(String executorName, String namespace,
			ClassLoader executorClassLoader, ClassLoader jobClassLoader) {
		try {
			Properties props = ResourceUtils.getResource("properties/saturn-ext.properties");
			String extClass = props.getProperty("saturn.ext");
			if (!Strings.isNullOrEmpty(extClass)) {
				Class<SaturnExecutorExtension> loadClass = (Class<SaturnExecutorExtension>) SaturnExecutor.class
						.getClassLoader().loadClass(extClass);
				Constructor<SaturnExecutorExtension> constructor = loadClass
						.getConstructor(String.class, String.class, ClassLoader.class, ClassLoader.class);
				saturnExecutorExtension = constructor
						.newInstance(executorName, namespace, executorClassLoader, jobClassLoader);
			}
		} catch (Exception e) { // NOSONAR log is not allowed to use, before saturnExecutorExtension.init().
			e.printStackTrace(); // NOSONAR
		} finally {
			if (saturnExecutorExtension == null) {
				saturnExecutorExtension = new SaturnExecutorExtensionDefault(executorName, namespace,
						executorClassLoader, jobClassLoader);
			}
		}
	}

	private Map<String, String> discover() throws Exception {
		if (SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.isEmpty()) {
			throw new Exception("Please configure the parameter " + SystemEnvProperties.NAME_VIP_SATURN_CONSOLE_URI
					+ " with env or -D");
		}

		int size = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.size();
		for (int i = 0; i < size; i++) {
			String consoleUri = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.get(i);
			String url = consoleUri + "/rest/v1/discovery?namespace=" + namespace;
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
				if (statusLine != null && statusCode.intValue() == HttpStatus.SC_OK) {
					Map<String, String> discoveryInfo = JsonUtils.getGson()
							.fromJson(responseBody, new TypeToken<Map<String, String>>() {
							}.getType());
					String connectionString = discoveryInfo.get(DISCOVER_INFO_ZK_CONN_STR);
					if (StringUtils.isBlank(connectionString)) {
						LogUtils.warn(log, LogEvents.ExecutorEvent.INIT, "ZK connection string is blank!");
						continue;
					}

					LogUtils.info(log, LogEvents.ExecutorEvent.INIT,
							"Discover successfully. Url: {}, discovery info: {}", url, discoveryInfo);
					return discoveryInfo;
				} else {
					handleDiscoverException(responseBody, statusCode);
				}
			} catch (SaturnExecutorException e) {
				LogUtils.error(log, LogEvents.ExecutorEvent.INIT, e.getMessage(), e);
				if (e.getCode() != SaturnExecutorExceptionCode.UNEXPECTED_EXCEPTION) {
					throw e;
				}
			} catch (Throwable t) {
				LogUtils.error(log, LogEvents.ExecutorEvent.INIT, "Fail to discover from Saturn Console. Url: {}", url,
						t);
			} finally {
				if (httpClient != null) {
					try {
						httpClient.close();
					} catch (IOException e) {
						LogUtils.error(log, LogEvents.ExecutorEvent.INIT, "Fail to close httpclient", e);
					}
				}
			}
		}

		String namespace = getTargetNamespace();
		String consoleUrl = getTargetConsoleUrl();
		String consoleIp = getTargetConsoleIp();
		String msg = "Fail to discover from Saturn Console! Please make sure that you have added the target namespace on Saturn Console, targetNamespace:%s, targetConsoleUrl:%s, targetConsoleIp:%s";
		throw new Exception(String.format(msg, namespace, consoleUrl, consoleIp));
	}

	private String getTargetConsoleIp() {
		String consoleUrl = getTargetConsoleUrl();
		try {
			URL url = new URL(consoleUrl);
			String host = url.getHost();
			return InetAddress.getByName(host).getHostAddress();
		} catch (Exception e) {
			LogUtils.warn(log, LogEvents.ExecutorEvent.COMMON, "fail to parse url - {} to ip exception", consoleUrl, e);
			return "unknown host";
		}
	}

	private String getTargetConsoleUrl() {
		return SystemEnvProperties.VIP_SATURN_CONSOLE_URI;
	}

	private String getTargetNamespace() {
		return System.getProperty("namespace");
	}

	private void handleDiscoverException(String responseBody, Integer statusCode) throws SaturnExecutorException {
		String errMsgInResponse = obtainErrorResponseMsg(responseBody);

		StringBuilder sb = new StringBuilder("Fail to discover from saturn console. ");
		if (StringUtils.isNotBlank(errMsgInResponse)) {
			sb.append(errMsgInResponse);
		}
		String exceptionMsg = sb.toString();

		if (statusCode != null) {
			if (statusCode.intValue() == HttpStatus.SC_NOT_FOUND) {
				throw new SaturnExecutorException(SaturnExecutorExceptionCode.NAMESPACE_NOT_EXIST, exceptionMsg);
			}

			if (statusCode.intValue() == HttpStatus.SC_BAD_REQUEST) {
				throw new SaturnExecutorException(SaturnExecutorExceptionCode.BAD_REQUEST, exceptionMsg);
			}
		}

		throw new SaturnExecutorException(SaturnExecutorExceptionCode.UNEXPECTED_EXCEPTION, exceptionMsg);
	}

	private String obtainErrorResponseMsg(String responseBody) {
		if (StringUtils.isNotBlank(responseBody)) {
			JsonElement message = JsonUtils.getJsonParser().parse(responseBody).getAsJsonObject().get("message");
			return message == JsonNull.INSTANCE || message == null ? "" : message.getAsString();
		}

		return "";
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

				LogUtils.info(log, LogEvents.ExecutorEvent.INIT, "start to discover from saturn console");

				Map<String, String> discoveryInfo = discover();
				String zkConnectionString = discoveryInfo.get(DISCOVER_INFO_ZK_CONN_STR);
				if (StringUtils.isBlank(zkConnectionString)) {
					LogUtils.error(log, LogEvents.ExecutorEvent.INIT, "zk connection string is blank!");
					throw new RuntimeException("zk connection string is blank!");
				}

				saturnExecutorExtension.postDiscover(discoveryInfo);

				// 初始化注册中心
				initRegistryCenter(zkConnectionString.trim());

				// 检测是否存在仍然有正在运行的SHELL作业
				LogUtils.info(log, LogEvents.ExecutorEvent.INIT, "start to check all exist jobs");
				checkAndKillExistedShellJobs();

				// 初始化timeout scheduler
				LogUtils.info(log, LogEvents.ExecutorEvent.INIT, "start to create timeout scheduler");
				TimeoutSchedulerExecutor.createScheduler(executorName);

				// 先注册Executor再启动作业，防止Executor因为一些配置限制而抛异常了，而作业线程已启动，导致作业还运行了一会
				registerExecutor();

				// 启动定时清空nohup文件的线程
				LogUtils.info(log, LogEvents.ExecutorEvent.INIT,
						"start to register periodic truncate nohup out service");
				periodicTruncateNohupOutService = new PeriodicTruncateNohupOutService(executorName);
				periodicTruncateNohupOutService.start();

				// 启动零点清0成功数错误数的线程
				LogUtils.info(log, LogEvents.ExecutorEvent.INIT, "start ResetCountService");
				resetCountService = new ResetCountService(executorName);
				resetCountService.startRestCountTimer();

				// 添加新增作业时的回调方法，启动已经存在的作业
				LogUtils.info(log, LogEvents.ExecutorEvent.INIT,
						"start to register newJobCallback, and async start existing jobs");
				saturnExecutorService.registerJobsWatcher();

				LogUtils.info(log, LogEvents.ExecutorEvent.INIT, "The executor {} start successfully which used {} ms",
						executorName, System.currentTimeMillis() - startTime);
			} catch (Throwable t) {
				saturnExecutorExtension.handleExecutorStartError(t);
				shutdown0();
				throw t;
			}
		} finally {
			shutdownLock.unlock();
		}
	}

	private void initRegistryCenter(String serverLists) throws Exception {
		try {
			// 验证namespace是否存在
			saturnExecutorExtension.validateNamespaceExisting(serverLists);

			// 初始化注册中心
			LogUtils.info(log, LogEvents.ExecutorEvent.INIT, "start to init reg center");
			ZookeeperConfiguration zkConfig = new ZookeeperConfiguration(serverLists, namespace, 1000, 3000);
			regCenter = new ZookeeperRegistryCenter(zkConfig);
			regCenter.init();
			connectionLostListener = new EnhancedConnectionStateListener(executorName) {
				@Override
				public void onLost() {
					needRestart = true;
					raiseAlarm();
				}
			};
			regCenter.addConnectionStateListener(connectionLostListener);

			// 创建SaturnExecutorService
			saturnExecutorService = new SaturnExecutorService(regCenter, executorName, saturnExecutorExtension);
			saturnExecutorService.setJobClassLoader(jobClassLoader);
			saturnExecutorService.setExecutorClassLoader(executorClassLoader);
			saturnExecutorService.setSaturnApplication(saturnApplication);

			StartCheckUtil.setOk(StartCheckItem.ZK);
		} catch (Exception e) {
			StartCheckUtil.setError(StartCheckItem.ZK);
			throw e;
		}
	}

	private void registerExecutor() throws Exception {
		try {
			LogUtils.info(log, LogEvents.ExecutorEvent.INIT, "start to register executor");
			saturnExecutorService.registerExecutor();
			StartCheckUtil.setOk(StartCheckItem.UNIQUE);
		} catch (Exception e) {
			StartCheckUtil.setError(StartCheckItem.UNIQUE);
			throw e;
		}
	}

	private void checkAndKillExistedShellJobs() {
		try {
			ScriptPidUtils.checkAllExistJobs(regCenter);
			StartCheckUtil.setOk(StartCheckItem.JOBKILL);
		} catch (IllegalStateException e) {
			StartCheckUtil.setError(StartCheckItem.JOBKILL);
			throw e;
		}
	}

	private void raiseAlarm() {
		LogUtils.warn(log, LogEvents.ExecutorEvent.REINIT, "raise alarm to console for executor reinitialization");
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
			LogUtils.warn(log, LogEvents.ExecutorEvent.REINIT, "cannot raise alarm", t);
		}
	}

	protected Map<String, Object> constructAlarmInfo(String namespace, String executorName) {
		Map<String, Object> alarmInfo = new HashMap<>();
		alarmInfo.put("executorName", executorName);
		alarmInfo.put("name", "Saturn Event");
		alarmInfo.put("title", "Executor_Restart");
		alarmInfo.put("level", "WARNING");
		alarmInfo.put("message",
				"Executor_Restart: namespace:[" + namespace + "] executor:[" + executorName + "] restart on "
						+ SaturnUtils.convertTime2FormattedString(System.currentTimeMillis()));

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

	private void shutdownJobSchedulers() {
		Map<String, JobScheduler> schdMap = JobRegistry.getSchedulerMap().get(executorName);
		if (MapUtils.isEmpty(schdMap)) {
			return;
		}

		long startTime = System.currentTimeMillis();
		List<Future<?>> futures = new ArrayList<>();
		Iterator<String> it = schdMap.keySet().iterator();
		while (it.hasNext()) {
			final String jobName = it.next();
			final JobScheduler jobScheduler = schdMap.get(jobName);
			if (jobScheduler != null) {
				futures.add(shutdownJobsExecutorService.submit(new Runnable() {
					@Override
					public void run() {
						try {
							jobScheduler.shutdown(false);
						} catch (Throwable t) {
							LogUtils.error(log, jobName, "shutdown JobScheduler error", t);
						}
					}
				}));
			}
		}
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (Exception e) {
				LogUtils.error(log, LogEvents.ExecutorEvent.SHUTDOWN, "wait shutdown job error", e);
			}
		}

		LogUtils.info(log, LogEvents.ExecutorEvent.SHUTDOWN, "Shutdown phase [shutdownJobSchedulers] took {}ms",
				System.currentTimeMillis() - startTime);
	}

	/**
	 * Executor关闭
	 */
	private void shutdown0() throws Exception {
		shutdownLock.lockInterruptibly();
		try {
			LogUtils.info(log, LogEvents.ExecutorEvent.SHUTDOWN, "Try to stop executor {}", executorName);
			if (saturnExecutorService != null) {
				saturnExecutorService.unregisterJobsWatcher();
			}
			shutdownJobSchedulers();
			if (saturnExecutorService != null) {
				saturnExecutorService.unregisterExecutor();
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
			if (periodicTruncateNohupOutService != null) {
				periodicTruncateNohupOutService.shutdown();
			}
			TimeoutSchedulerExecutor.shutdownScheduler(executorName);
			LogUtils.info(log, LogEvents.ExecutorEvent.SHUTDOWN, "The executor {} is stopped", executorName);
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
			LogUtils.info(log, LogEvents.ExecutorEvent.GRACEFUL_SHUTDOWN, "Try to stop executor {} gracefully",
					executorName);
			if (saturnExecutorService != null) {
				saturnExecutorService.unregisterJobsWatcher();
			}
			// 先关闭统计信息上报，因为上报的zk结点为servers/xxx/xxx，如果放在下线后再关闭，则导致ExecutorCleanService执行后，仍然在上报统计信息，垃圾结点由此而生
			shutdownAllCountThread();

			if (saturnExecutorService != null) {
				saturnExecutorService.unregisterExecutor();
			}
			if (resetCountService != null) {
				resetCountService.shutdownRestCountTimer();
			}
			if (periodicTruncateNohupOutService != null) {
				periodicTruncateNohupOutService.shutdown();
			}
			// shutdown timeout-watchdog-threadpool
			TimeoutSchedulerExecutor.shutdownScheduler(executorName);
			try {
				blockUntilJobCompletedIfNotTimeout();
				shutdownJobSchedulers();
				JobRegistry.clearExecutor(executorName);
			} finally {
				if (connectionLostListener != null) {
					connectionLostListener.close();
				}
				if (regCenter != null) {
					regCenter.close();
				}
			}

			if (saturnApplication != null) {
				ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
				try {
					Thread.currentThread().setContextClassLoader(jobClassLoader);
					saturnApplication.getClass().getMethod("destroy").invoke(saturnApplication);
					LogUtils.info(log, LogEvents.ExecutorEvent.GRACEFUL_SHUTDOWN,
							"SaturnApplication destroy successfully");
				} catch (Throwable t) {
					LogUtils.error(log, LogEvents.ExecutorEvent.GRACEFUL_SHUTDOWN, "SaturnApplication destroy error",
							t);
				} finally {
					Thread.currentThread().setContextClassLoader(oldCL);
				}
			}

			LogUtils.info(log, LogEvents.ExecutorEvent.GRACEFUL_SHUTDOWN, "executor {} is stopped gracefully",
					executorName);
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
		long startTime = System.currentTimeMillis();

		boolean hasRunning = false;
		do {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				LogUtils.error(log, LogEvents.ExecutorEvent.GRACEFUL_SHUTDOWN, e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
			for (Entry<String, JobScheduler> entry : entries) {
				JobScheduler jobScheduler = entry.getValue();
				if (jobScheduler.isAllowedShutdownGracefully()) {
					if (jobScheduler.getJob().isRunning()) {
						hasRunning = true;
						break;
					} else {
						hasRunning = false;
					}
				}
				// 其他作业（消息作业）不等，因为在接下来的forceStop是优雅强杀的，即等待一定时间让业务执行再强杀
			}
		} while (hasRunning
				&& System.currentTimeMillis() - startTime < SystemEnvProperties.VIP_SATURN_SHUTDOWN_TIMEOUT * 1000);

		LogUtils.info(log, LogEvents.ExecutorEvent.GRACEFUL_SHUTDOWN,
				"Shutdown phase [blockUntilJobCompletedIfNotTimeout] took {}ms",
				System.currentTimeMillis() - startTime);
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
			shutdownJobsExecutorService.shutdownNow();
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
			shutdownJobsExecutorService.shutdownNow();
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
