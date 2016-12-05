package com.vip.saturn.job.executor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.CollectionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class SaturnExecutor {
	protected static Logger log;

	private String executorName;
	ClassLoader executorClassLoader;
	ClassLoader jobClassLoader;
	private Runnable shutdownHandler = null;

	private String namespace;
	private int monitorPort = -1;
	
	private Object shutdownLock = new Object();

	private ZookeeperConfiguration zkConfig;
	protected ZookeeperRegistryCenter regCenter;

	private SaturnExecutorService saturnExecutorService;

	private ResetCountService resetCountService;

	private boolean isShutdown;
	private ExecutorService executor;

	private static AtomicBoolean inited = new AtomicBoolean(false);

	private static Class<?> extClazz = null;

	private static void initZK() {
		if (extClazz == null) {
			return;
		}
		try {
			extClazz.getMethod("initZK").invoke(null);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 判断zk是否有该域
	 */
	private void doValidation() {
		if (extClazz == null) {
			return;
		}
		try {
			extClazz.getMethod("doValidation", String.class).invoke(null, namespace);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
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
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 日志目录
	 */
	private static String NAME_SATURN_LOG_DIR = "SATURN_LOG_DIR";

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
		log = LoggerFactory.getLogger(SaturnExecutor.class);
	}

	/**
	 * SaturnExecutor工厂入口
	 * @param namespace
	 * @param monitorPort
	 * @param _executorName
	 * @return
	 */
	public static SaturnExecutor buildExecutor(String namespace, int monitorPort, String _executorName) {
		if (_executorName == null || _executorName.isEmpty()) {
			String hostName = LocalHostService.getHostName();
			if ("localhost".equals(hostName) || "localhost6".equals(hostName)) {
				throw new RuntimeException(
						"You are using hostName as executorName, it cannot be localhost or localhost6, please configure hostName.");
			}
			_executorName = hostName;// NOSONAR
		}
		init(_executorName);
		return new SaturnExecutor(namespace, monitorPort, _executorName);
	}

	private SaturnExecutor(String namespace, int monitorPort, String executorName) {
		this.executorName = executorName;
		this.namespace = namespace;
		this.monitorPort = monitorPort;
		executor = Executors
				.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "zk-reconnect-thread", false));
	}

	/**
	 * 获取saturnExecutorService
	 * @return
	 */
	public SaturnExecutorService getSaturnExecutorService() {
		return saturnExecutorService;
	}

	protected void setSaturnExecutorService(SaturnExecutorService saturnExecutorService) {
		this.saturnExecutorService = saturnExecutorService;
	}

	protected String getExecutorName() {
		return executorName;
	}

	protected void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	protected String getNamespace() {
		return namespace;
	}

	protected void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	protected void scheduleJob(String jobName) {
		log.info("[{}] msg=add new job {} - {}", jobName, executorName, jobName);
		JobConfiguration jobConfig = new JobConfiguration(regCenter, jobName);
		if (jobConfig.getSaturnJobClass() == null) {
			return;
		}
		if (jobConfig.isDeleting()) {
			String serverNodePath = JobNodePath.getServerNodePath(jobName, executorName);
			if (regCenter.isExisted(serverNodePath)) {
				regCenter.remove(serverNodePath);
			}
			return;
		}
		JobScheduler scheduler = new JobScheduler(regCenter, jobConfig);
		scheduler.setSaturnExecutorService(saturnExecutorService);
		scheduler.init();
	}

	class ConnectionLostListener implements ConnectionStateListener {
		private AtomicBoolean connected = new AtomicBoolean(false);
		private AtomicBoolean stoped = new AtomicBoolean(false);

		private long getSessionId(CuratorFramework client) {
			long sessionId;
			try {
				sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
			} catch (Exception e) {// NOSONAR
				return -1;
			}
			return sessionId;
		}

		private void restart() {
			try {
				execute(executorClassLoader, jobClassLoader);
			} catch (Exception e) {
				log.error("", e);
			}
		}

		@Override
		public void stateChanged(final CuratorFramework client, final ConnectionState newState) {
			// 使用single thread executor严格保证ZK事件执行的顺序性，避免并发性问题
			if (ConnectionState.SUSPENDED == newState) {
				connected.set(false);
				final long sessionId = getSessionId(client);
				executor.submit(new Runnable() {
					@Override
					public void run() {
						do {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
							}
							if (isShutdown)
								return;
							long newSessionId = getSessionId(client);
							if (sessionId != newSessionId) {
								log.info(" {} is going to shutdown for zk lost", executorName);
								shutdown();
								stoped.set(true);
								return;
							}
						} while (!isShutdown && !connected.get());
					}
				});

			} else if (ConnectionState.RECONNECTED == newState) {
				connected.set(true);
				executor.submit(new Runnable() {
					@Override
					public void run() {

						if (stoped.compareAndSet(true, false)) {
							log.info(" {} is going to restart for zk reconnected", executorName);
							restart();
						}

					}

				});
			}
		}
	}

	/**
	 * 执行入口
	 * @param executorClassLoader
	 * @param jobClassLoader
	 * @throws Exception
	 */
	public void execute(ClassLoader executorClassLoader, ClassLoader jobClassLoader) throws Exception {
		this.executorClassLoader = executorClassLoader;
		this.jobClassLoader = jobClassLoader;
		StartCheckUtil.add2CheckList(StartCheckItem.ZK, StartCheckItem.UNIQUE, StartCheckItem.JOBKILL);

		initZK();
		// 验证namespace是否存在
		doValidation();
		String serverLists = SystemEnvProperties.VIP_SATURN_ZK_CONNECTION;
		zkConfig = new ZookeeperConfiguration(monitorPort, serverLists, namespace, 1000, 3000, 3);
		if (regCenter != null) {
			regCenter.close();
		}
		regCenter = new ZookeeperRegistryCenter(zkConfig);
		saturnExecutorService = SaturnExecutorService.init(namespace, regCenter, executorName);

		saturnExecutorService.setJobClassLoader(jobClassLoader);
		saturnExecutorService.setExecutorClassLoader(executorClassLoader);

		// 初始化注册中心
		try {
			regCenter.init();
			ConnectionLostListener connectionLostListener = new ConnectionLostListener();
			regCenter.addConnectionStateListener(connectionLostListener);

			StartCheckUtil.setOk(StartCheckUtil.StartCheckItem.ZK);
		} catch (Exception e) {
			e.printStackTrace();// NOSONAR
			StartCheckUtil.setError(StartCheckUtil.StartCheckItem.ZK);
			throw e;
		}

		// 注册作业名
		List<String> zkJobNames = saturnExecutorService.registerJobNames();
		try {
			ScriptPidUtils.checkAllExistJobs(regCenter, zkJobNames);
			StartCheckUtil.setOk(StartCheckUtil.StartCheckItem.JOBKILL);
		} catch (IllegalStateException ex) {
			StartCheckUtil.setError(StartCheckUtil.StartCheckItem.JOBKILL);
			System.out.println("Start error. Please check it first."); // NOSONAR
			System.out.println(ex.getMessage()); // NOSONAR
			throw ex;
		}

		// 初始化timeout scheduler
		TimeoutSchedulerExecutor.createScheduler(executorName);

		// 先注册Executor再启动作业，防止Executor因为一些配置限制而抛异常了，而作业线程已启动，导致作业还运行了一会
		// 注册Executor
		try {
			saturnExecutorService.registerExecutor();
			StartCheckUtil.setOk(StartCheckUtil.StartCheckItem.UNIQUE);
		} catch (Exception e) {
			e.printStackTrace();// NOSONAR
			StartCheckUtil.setError(StartCheckUtil.StartCheckItem.UNIQUE);
			throw e;
		}

		// 启动作业
		if (zkJobNames != null) {
			for (String jobName : zkJobNames) {
				scheduleJob(jobName);
			}
		}

		// 添加新增作业时的回调方法
		saturnExecutorService.addNewJobListenerCallback(new ScheduleNewJobCallback() {
			@Override
			public void call(String jobName) {
				scheduleJob(jobName);
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
					shutdownGracefully();
					executor.shutdown();
					isShutdown = true;
				}

			};
			ShutdownHandler.addShutdownCallback(shutdownHandler);
		}
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
						log.info("[{}] msg=job {} is enabled, force shutdown.", jobName, jobName);
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
	public void shutdown() {
		synchronized (shutdownLock) {
			shutdownUnfinishJob();
			// 清理Executor的IP运行Node
			String ipNode = saturnExecutorService.getIpNode();
			if (regCenter != null && ipNode != null && regCenter.isConnected()) {
				regCenter.remove(ipNode);
				regCenter.close();
			}
			JobRegistry.clearExecutor(executorName);
			// cancel零点清0成功数错误数线程
			resetCountService.shutdownRestCountTimer();
			// shutdown timeout-watchdog-threadpool
			TimeoutSchedulerExecutor.shutdownScheduler(executorName);
		}
	}
	
	/**
	 * Executor优雅退出：
	 * 把自己从集群中拿掉，现有的作业不停； 一直到全部作业都执行完毕，再真正退出； 
	 * 设置一定超时时间，如果超过这个时间仍未退出，则强行中止
	 */
	public void shutdownGracefully() {
		synchronized (shutdownLock) {
			shutdownAllCountThread();
			
			// 清理Executor的IP运行Node
			String ipNode = saturnExecutorService.getIpNode();
			if (regCenter != null && ipNode != null && regCenter.isConnected()) {
				regCenter.remove(ipNode);
			}		
			// cancel零点清0成功数错误数线程
			resetCountService.shutdownRestCountTimer();
			// shutdown timeout-watchdog-threadpool
			TimeoutSchedulerExecutor.shutdownScheduler(executorName);
			try{
				blockUntilJobCompletedIfNotTimeout();
				shutdownUnfinishJob();
				JobRegistry.clearExecutor(executorName);
			}finally{
				if (regCenter != null) {
					regCenter.close();
				}
			}
		}
	}

	/**
	 * block until all Job is completed if it is not timeout
	 * 
	 * @param jobSchedulerList
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
				e.printStackTrace();// NOSONAR
			}
			for(Entry<String, JobScheduler> entry : entries){
				JobScheduler  jobScheduler = entry.getValue();
				if (jobScheduler.getJob().isRunning()) {
					hasRunning = true;
					break;
				}
			}
		}while(hasRunning && System.currentTimeMillis() - start < SystemEnvProperties.VIP_SATURN_SHUTDOWN_TIMEOUT * 1000);
		
		
	}
}
