package com.vip.saturn.job.executor;

import com.google.common.base.Strings;
import com.vip.saturn.job.basic.JobRegistry;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.JobTypeManager;
import com.vip.saturn.job.basic.ShutdownHandler;
import com.vip.saturn.job.basic.TimeoutSchedulerExecutor;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.java.SaturnJavaJob;
import com.vip.saturn.job.reg.zookeeper.ZookeeperConfiguration;
import com.vip.saturn.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.vip.saturn.job.shell.SaturnScriptJob;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.LocalHostService;
import com.vip.saturn.job.utils.ResourceUtils;
import com.vip.saturn.job.utils.ScriptPidUtils;
import com.vip.saturn.job.utils.StartCheckUtil;
import com.vip.saturn.job.utils.StartCheckUtil.StartCheckItem;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	/**
	 * 日志目录
	 */
	private static final String NAME_SATURN_LOG_DIR = "SATURN_LOG_DIR";

	private static Logger LOGGER;

	private static AtomicBoolean inited = new AtomicBoolean(false);

	private static Class<?> extClazz;

	protected ZookeeperRegistryCenter regCenter;

	private String executorName;

	private String namespace;

	private ClassLoader executorClassLoader;

	private ClassLoader jobClassLoader;

	private Runnable shutdownHandler;

	private ZookeeperConfiguration zkConfig;

	private ExecutorService executor;

	private SaturnExecutorService saturnExecutorService;

	private ResetCountService resetCountService;

	private ReentrantLock shutdownLock = new ReentrantLock();

	private boolean isShutdown;

	private AtomicBoolean restarting = new AtomicBoolean(false);

	private SaturnExecutor(String namespace, String executorName) {
		this.executorName = executorName;
		this.namespace = namespace;
		executor = Executors
				.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-zk-reconnect-thread", false));
	}

	private String discoverZK() throws Exception {
		if (extClazz == null) {
			return SystemEnvProperties.VIP_SATURN_ZK_CONNECTION;
		}
		return (String) extClazz.getMethod("discoverZK", String.class).invoke(null, namespace);
	}
	
	/**
	 * 判断zk是否有该域
	 */
	private void doValidation(String connectString) throws Exception {
		if (extClazz == null) {
			return;
		}
		extClazz.getMethod("doValidation", String.class, String.class).invoke(null, namespace, connectString);
	}

	private static void initExt() {
		try {
			final Properties props = ResourceUtils.getResource("properties/saturn-ext.properties");
			if (props != null) {
				String extClass = props.getProperty("saturn.ext");
				if (!Strings.isNullOrEmpty(extClass)) {
					extClazz = SaturnExecutor.class.getClassLoader().loadClass(extClass);
					extClazz.getMethod("init").invoke(null);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); // NOSONAR
		}
	}

	/**
	 * 获取环境变量
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static String getEnv(String key, String defaultValue) {
		String v = System.getenv(key);
		if (v == null || v.isEmpty()) {
			return defaultValue;
		}
		return v;
	}

	/**
	 * 获取日志目录
	 * @return
	 */
	public static String getLogDir() {
		String SATURN_LOG_DIR_DEFAULT = "/apps/logs/saturn/" + System.getProperty("namespace") + "/"
				+ System.getProperty("log.folder");
		String SATURN_LOG_DIR = System.getProperty(NAME_SATURN_LOG_DIR,
				getEnv(NAME_SATURN_LOG_DIR, SATURN_LOG_DIR_DEFAULT));
		return SATURN_LOG_DIR;
	}

	/**
	 * 初始化
	 * @param executorName
	 */
	public static void init(String executorName) {
		if (!inited.compareAndSet(false, true)) {
			return;
		}
		System.setProperty("log.folder", executorName + "-" + LocalHostService.cachedIpAddress);
		System.setProperty("saturn.log.dir", getLogDir());
		JobTypeManager.getInstance().registerHandler("JAVA_JOB", SaturnJavaJob.class);
		JobTypeManager.getInstance().registerHandler("SHELL_JOB", SaturnScriptJob.class);
		initExt();
		LOGGER = LoggerFactory.getLogger(SaturnExecutor.class);
	}

	/**
	 * SaturnExecutor工厂入口
	 */
	public static SaturnExecutor buildExecutor(String namespace, String _executorName) {
		if ("$SaturnSelf".equals(namespace)) {
			throw new RuntimeException("The namespace cannot be $SaturnSelf");
		}
		if (_executorName == null || _executorName.isEmpty()) {
			String hostName = LocalHostService.getHostName();
			if ("localhost".equals(hostName) || "localhost6".equals(hostName)) {
				throw new RuntimeException(
						"You are using hostName as executorName, it cannot be localhost or localhost6, please configure hostName.");
			}
			_executorName = hostName;// NOSONAR
		}
		init(_executorName);
		return new SaturnExecutor(namespace, _executorName);
	}

	public boolean scheduleJob(String jobName) {
		LOGGER.info("[{}] msg=add new job {} - {}", jobName, executorName, jobName);
		JobConfiguration jobConfig = new JobConfiguration(regCenter, jobName);
		if (jobConfig.getSaturnJobClass() == null) {
			LOGGER.warn("[{}] msg={} - {} the saturnJobClass is null, jobType is {}", jobConfig, executorName, jobName, jobConfig.getJobType());
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

	class ConnectionLostListener implements ConnectionStateListener {

		private AtomicBoolean connected = new AtomicBoolean(false);
		
		private long getSessionId(CuratorFramework client) {
			long sessionId;
			try {
				sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
			} catch (Exception e) {// NOSONAR
				return -1;
			}
			return sessionId;
		}

		@Override
		public void stateChanged(final CuratorFramework client, final ConnectionState newState) {
			// 使用single thread executor严格保证ZK事件执行的顺序性，避免并发性问题
			if (ConnectionState.SUSPENDED == newState) {
				connected.set(false);
				if(restarting.compareAndSet(false, true)) {
					LOGGER.warn("The executor {} found zk is SUSPENDED", executorName);
					final long sessionId = getSessionId(client);
					executor.submit(new Runnable() {
						@Override
						public void run() {
							try {
								do {
									try {
										Thread.sleep(1000L);
									} catch (InterruptedException e) {
									}
									if (isShutdown) {
										return;
									}
									long newSessionId = getSessionId(client);
									if (sessionId != newSessionId) {
										LOGGER.warn("The executor {} is going to restart for zk lost, client: {}", executorName, client);
										restart();
										return;
									}
								} while (!isShutdown && !connected.get());
							} finally {
								restarting.set(false);
							}
						}
					});
				}
			} else if (ConnectionState.RECONNECTED == newState) {
				LOGGER.warn("The executor {} found zk is RECONNECTED", executorName);
				connected.set(true);
			}
		}
	}
	
	private void restart() {
		while (true) {
			try {
				execute();
				break;
			} catch (InterruptedException e) {
				LOGGER.warn("The executor {} restart is interrupted, and exit the restart process.", executorName);
				break;
			} catch (Throwable t) {
				LOGGER.error("The executor " + executorName + " restart failed, will retry again.", t);
			}

			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				LOGGER.warn("The executor {} restart is interrupted, and exit the restart process.", executorName);
				break;
			}
		}
	}

	private void execute() throws Exception {
		shutdownLock.lockInterruptibly();

		try {
			if (isShutdown) {
				return;
			}

			long startTime = System.currentTimeMillis();

			shutdown();

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
					doValidation(serverLists);
					
					zkConfig = new ZookeeperConfiguration(serverLists, namespace, 1000, 3000, 3);
					regCenter = new ZookeeperRegistryCenter(zkConfig);
					saturnExecutorService = new SaturnExecutorService(regCenter, executorName);
					saturnExecutorService.setJobClassLoader(jobClassLoader);
					saturnExecutorService.setExecutorClassLoader(executorClassLoader);
		
					// 初始化注册中心
					LOGGER.info("start to init reg center.");
					regCenter.init();
					ConnectionLostListener connectionLostListener = new ConnectionLostListener();
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
					while(iterator.hasNext()) {
						String jobName = iterator.next();
						if(scheduleJob(jobName)) {
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
		
				// 注册退出时资源清理回调
				if (shutdownHandler == null) {
					shutdownHandler = new Runnable() {
		
						@Override
						public void run() {
							if (isShutdown) {
								return;
							}
							try {
								shutdownLock.lockInterruptibly();
								try {
									shutdownGracefully();
									executor.shutdownNow();
									isShutdown = true;
								} finally {
									shutdownLock.unlock();
								}
							} catch (Exception e) {
								LOGGER.error(e.getMessage(), e);
							}
						}
		
					};
					ShutdownHandler.addShutdownCallback(shutdownHandler);
				}

				LOGGER.info("The executor {} start successfully which used {} ms", executorName, System.currentTimeMillis() - startTime);
			} catch (Throwable t) {
				LOGGER.error("Fail to start executor {}", executorName);
				shutdown();
				throw t;
			}
		} finally {
			shutdownLock.unlock();
		}
	}
	
	/**
	 * 执行入口。注意，仅仅能执行一次，请不要执行多次。
	 */
	public void execute(ClassLoader executorClassLoader, ClassLoader jobClassLoader) throws Exception {
		this.executorClassLoader = executorClassLoader;
		this.jobClassLoader = jobClassLoader;
		execute();
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
	public void shutdown() throws Exception {
		shutdownLock.lockInterruptibly();
		try {
			LOGGER.info("Try to stop executor {}", executorName);
			shutdownUnfinishJob();
			if(saturnExecutorService != null) {
				saturnExecutorService.shutdown();
			}
			if (regCenter != null) {
				regCenter.close();
			}
			JobRegistry.clearExecutor(executorName);
			if(resetCountService != null) {
				resetCountService.shutdownRestCountTimer();
			}
			TimeoutSchedulerExecutor.shutdownScheduler(executorName);
			LOGGER.info("The executor {} is stopped", executorName);
		} finally {
			shutdownLock.unlock();
		}
	}
	
	/**
	 * Executor优雅退出：
	 * 把自己从集群中拿掉，现有的作业不停； 一直到全部作业都执行完毕，再真正退出； 
	 * 设置一定超时时间，如果超过这个时间仍未退出，则强行中止
	 */
	public void shutdownGracefully() throws Exception {
		shutdownLock.lockInterruptibly();
		try {
			LOGGER.info("Try to stop executor {} gracefully", executorName);
			shutdownUnfinishJob();
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
		if(schdMap == null){
			return;
		}
		Set<Entry<String, JobScheduler>> entries = schdMap.entrySet();
		
		if(CollectionUtils.isEmpty(entries)){
			return;
		}
		long start = System.currentTimeMillis();
		
		boolean hasRunning = false;
		do{		
			try {			
				Thread.sleep(200);
			} catch (InterruptedException e) {
				LOGGER.error(e.getMessage(), e);
			}
			for(Entry<String, JobScheduler> entry : entries){
				JobScheduler  jobScheduler = entry.getValue();
				if("JAVA_JOB".equals(jobScheduler.getCurrentConf().getJobType())){
					if (jobScheduler.getJob().isRunning()) {
						hasRunning = true;
						break;
					}else{
						hasRunning = false;
					}
				}else if("SHELL_JOB".equals(jobScheduler.getCurrentConf().getJobType())){
					if(jobScheduler.getCurrentConf().isEnabled()){
						if (jobScheduler.getJob().isRunning()) {
							hasRunning = true;
							break;
						}else{
							hasRunning = false;
						}
					}
				}else{
					jobScheduler.stopJob(false);
				}
			}
		}while(hasRunning && System.currentTimeMillis() - start < SystemEnvProperties.VIP_SATURN_SHUTDOWN_TIMEOUT * 1000);


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
