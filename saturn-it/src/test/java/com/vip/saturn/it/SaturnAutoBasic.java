package com.vip.saturn.it;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.it.utils.NestedZkUtils;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.RequestResultHelper;
import com.vip.saturn.job.console.springboot.SaturnConsoleApp;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.executor.ExecutorConfig;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.executor.SaturnExecutor;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.failover.FailoverNode;
import com.vip.saturn.job.internal.server.ServerNode;
import com.vip.saturn.job.internal.sharding.ShardingNode;
import com.vip.saturn.job.internal.sharding.ShardingService;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.internal.storage.JobNodeStorage;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.reg.zookeeper.ZookeeperConfiguration;
import com.vip.saturn.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.vip.saturn.job.utils.ScriptPidUtils;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic for Saturn test automation
 */
public class SaturnAutoBasic {
	protected final static int timeout = 15;
	protected static final String NAMESPACE = "it-saturn";
	protected static Logger log;
	protected static ZookeeperRegistryCenter regCenter;

	protected static NestedZkUtils nestedZkUtils;

	protected static List<Main> saturnExecutorList = new ArrayList<>();
	protected static List<SaturnConsoleInstance> saturnConsoleInstanceList = new ArrayList<>();

	public static class SaturnConsoleInstance {

		public ApplicationContext applicationContext;
		public int port;
		public String url;

		public SaturnConsoleInstance(ApplicationContext applicationContext, int port, String url) {
			this.applicationContext = applicationContext;
			this.port = port;
			this.url = url;
		}

		public void stop() {
			SaturnConsoleApp.stop(applicationContext);
		}
	}

	protected static void initSysEnv() {
		System.setProperty("namespace", NAMESPACE);
		System.setProperty("saturn.stdout", "true");
		System.setProperty("saturn.debug", "true");
		System.setProperty("VIP_SATURN_CONTAINER_ALIGN_WITH_PHYSICAL", "false");
		// 必须在设置saturn.out后，才加载日志框架
		log = LoggerFactory.getLogger(AbstractSaturnIT.class);
	}

	protected static void initZK() throws Exception {
		nestedZkUtils = new NestedZkUtils();
		nestedZkUtils.startServer();
		assertThat(nestedZkUtils.isStarted());
		String zkString = nestedZkUtils.getZkString();

		regCenter = new ZookeeperRegistryCenter(new ZookeeperConfiguration(zkString, NAMESPACE, 1000, 3000, 3));
		regCenter.init();
	}

	public static void prepare4Console() throws Exception {
		// fix this exception
		// org.springframework.jmx.export.UnableToRegisterMBeanException: Unable to register MBean
		// [org.springframework.cloud.context.environment.EnvironmentManager@77fecd2c] with key 'environmentManager';
		System.setProperty("spring.jmx.enabled", "false");
		System.setProperty("authorization.enabled.default", "false");
		System.setProperty("authentication.enabled", "false");
		SaturnEnvProperties.VIP_SATURN_CONSOLE_CLUSTER_ID = "CONSOLE-IT";
		prepareForItSql();
		SaturnConsoleApp.startEmbeddedDb();
	}

	private static void prepareForItSql() throws IOException {
		String itSqlFilePath = "/db/h2/custom.sql";
		URL resource = SaturnAutoBasic.class.getResource(itSqlFilePath);
		File file = new File(resource.getFile());
		if (file.exists()) {
			List<String> readLines = FileUtils.readLines(file, Charset.forName("utf-8"));
			if (readLines != null && readLines.size() > 0) {
				readLines.set(0,
						"INSERT INTO `zk_cluster_info`(`zk_cluster_key`, `alias`, `connect_string`) VALUES('it_cluster', 'IT集群', '"
								+ nestedZkUtils.getZkString() + "');");
			}
			FileUtils.writeLines(file, readLines, false);
		} else {
			log.error("The {} is not existing", itSqlFilePath);
		}
	}

	public static void stopConsoleDb() {
		SaturnConsoleApp.stopEmbeddedDb();
	}

	protected static void refreshRegCenter(String url) throws Exception {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost post = new HttpPost(url + "/console/registryCenter/refresh");

			CloseableHttpResponse httpResponse = httpClient.execute(post);
			StatusLine statusLine = httpResponse.getStatusLine();
			if (statusLine != null && statusLine.getStatusCode() == 200) {
				log.info("refreshRegCenter one...");
			} else {
				String response = EntityUtils.toString(httpResponse.getEntity());
				throw new Exception(response);
			}

			// wait refreshRegCenter completed
			while (true) {
				httpResponse = httpClient.execute(post);
				String response = EntityUtils.toString(httpResponse.getEntity());
				statusLine = httpResponse.getStatusLine();
				if (statusLine != null && statusLine.getStatusCode() == 200) {
					if (RequestResultHelper.isSuccess(JSONObject.parseObject(response, RequestResult.class))) {
						return;
					}
				} else {
					throw new Exception(response);
				}
				Thread.sleep(1000L);
			}
		} finally {
			httpClient.close();
		}
	}

	public static URL[] getAppClassLoaderUrls(ClassLoader jobClassLoader) {
		URLClassLoader appClassLoader = (URLClassLoader) jobClassLoader;
		return appClassLoader.getURLs();
	}

	public static void startExecutorList(int count) throws Exception {
		assertThat(nestedZkUtils.isStarted());
		for (int i = 0; i < count; i++) {
			startOneNewExecutorList();
		}
		Thread.sleep(2000);
	}

	public static Main startOneNewExecutorList() throws Exception {
		assertThat(nestedZkUtils.isStarted());
		int size = saturnExecutorList.size();

		Main main = new Main();
		String executorName = "executorName" + size;
		/*
		 * ClassLoader saturnClassloader = classloaders.get(executorName); if(saturnClassloader == null){
		 * saturnClassloader = new SaturnClassLoader(getAppClassLoaderUrls(Main.class.getClassLoader()), null);
		 * classloaders.put(executorName,saturnClassloader); }
		 */
		String[] args = {"-namespace", NAMESPACE, "-executorName", executorName};
		main.launchInner(args, Main.class.getClassLoader(), Main.class.getClassLoader());
		saturnExecutorList.add(main);
		Thread.sleep(1000);
		return main;
	}

	public static Main startExecutor(int index) throws Exception {
		assertThat(nestedZkUtils.isStarted());
		Main saturnContainer = saturnExecutorList.get(index);
		if (saturnContainer != null) {
			// 如果节点正在运行则退出
			log.warn("the executor{} already exist.", index);
			return saturnContainer;
		} else {
			Main main = new Main();
			String executorName = "executorName" + index;
			String[] args = {"-namespace", NAMESPACE, "-executorName", executorName};
			main.launchInner(args, Main.class.getClassLoader(), Main.class.getClassLoader());
			saturnExecutorList.set(index, main);
			Thread.sleep(1000);
			return main;
		}
	}

	public static void stopExecutor(int index) throws Exception {
		assertThat(saturnExecutorList.size()).isGreaterThan(index);
		Main saturnContainer = saturnExecutorList.get(index);
		if (saturnContainer != null) {
			saturnContainer.shutdown();
		} else {
			log.warn("the {} SaturnContainer has stopped", index);
		}
		saturnExecutorList.set(index, null);
		for (Main tmp : saturnExecutorList) {
			if (tmp != null) {
				return;
			}
		}
		saturnExecutorList.clear();
	}

	public static void stopExecutorList() throws Exception {
		for (int i = 0; i < saturnExecutorList.size(); i++) {
			Main saturnContainer = saturnExecutorList.get(i);
			if (saturnContainer != null) {
				saturnContainer.shutdown();
			}
		}
		saturnExecutorList.clear();
	}

	public static ExecutorConfig getExecutorConfig(int index) {
		assertThat(saturnExecutorList.size()).isGreaterThan(index);
		Main saturnContainer = saturnExecutorList.get(index);
		SaturnExecutor saturnExecutor = (SaturnExecutor) saturnContainer.getSaturnExecutor();
		return saturnExecutor.getSaturnExecutorService().getExecutorConfig();
	}

	public static void startSaturnConsoleList(int count) throws Exception {
		assertThat(nestedZkUtils.isStarted());
		SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST = new ArrayList<>();
		String urlStr = "";
		int serverPort = 9088;
		for (int i = 0; i < count; i++) {
			System.setProperty("server.port", (++serverPort) + "");
			SaturnConsoleInstance saturnConsoleInstance = new SaturnConsoleInstance(SaturnConsoleApp.start(),
					serverPort, "http://localhost:" + serverPort);
			saturnConsoleInstanceList.add(saturnConsoleInstance);
			refreshRegCenter(saturnConsoleInstance.url);
			if (i == 0) {
				urlStr = saturnConsoleInstance.url;
			} else {
				urlStr = urlStr + "," + saturnConsoleInstance.url;
			}
			SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.add(saturnConsoleInstance.url);
		}
		SystemEnvProperties.VIP_SATURN_CONSOLE_URI = urlStr;
	}

	public static void stopSaturnConsole(int index) throws InterruptedException {
		assertThat(saturnConsoleInstanceList.size()).isGreaterThan(index);
		SaturnConsoleInstance saturnConsoleInstance = saturnConsoleInstanceList.get(index);
		if (saturnConsoleInstance != null) {
			saturnConsoleInstance.stop();
		} else {
			log.warn("the {} saturnConsoleInstance has stopped", index);
		}
		saturnConsoleInstanceList.set(index, null);
	}

	public static void stopSaturnConsoleList() throws InterruptedException {
		// 等待做完一些事情
		Thread.sleep(200);
		for (int i = 0; i < saturnConsoleInstanceList.size(); i++) {
			SaturnConsoleInstance saturnConsoleInstance = saturnConsoleInstanceList.get(i);
			if (saturnConsoleInstance != null) {
				saturnConsoleInstance.stop();
			}
			// 等待选举完成
			Thread.sleep(200);
		}
		saturnConsoleInstanceList.clear();
	}

	public static CoordinatorRegistryCenter getExecutorRegistryCenter(Main executorMain) {
		return ((SaturnExecutor) executorMain.getSaturnExecutor()).getSaturnExecutorService()
				.getCoordinatorRegistryCenter();
	}

	public static void stopZkServer() throws IOException {
		nestedZkUtils.stopServer();
	}

	public static void killSession(CuratorFramework client) throws Exception {
		nestedZkUtils.killSession(client.getZookeeperClient().getZooKeeper());
	}

	public static void enableJob(String jobName) {
		configJob(jobName, ConfigurationNode.ENABLED, true);
	}

	public static void disableJob(String jobName) {
		configJob(jobName, ConfigurationNode.ENABLED, false);
	}

	public static void removeJob(String jobName) {
		configJob(jobName, ConfigurationNode.TO_DELETE, 1);
	}

	public static void forceRemoveJob(String jobName) {
		try {
			regCenter.remove(JobNodePath.getJobNameFullPath(jobName));
		} catch (Exception e) {
		}
	}

	public static void runAtOnce(String jobName) {
		for (int i = 0; i < saturnExecutorList.size(); i++) {
			Main saturnContainer = saturnExecutorList.get(i);
			if (saturnContainer == null) {
				continue;
			}
			String path = JobNodePath
					.getNodeFullPath(jobName, String.format(ServerNode.RUNONETIME, saturnContainer.getExecutorName()));

			if (regCenter.isExisted(path)) {
				regCenter.remove(path);
			}
			regCenter.persist(path, "1");
		}
	}

	public static void forceStopJob(String jobName) {
		for (int i = 0; i < saturnExecutorList.size(); i++) {
			Main saturnContainer = saturnExecutorList.get(i);
			if (saturnContainer == null) {
				continue;
			}
			String path = JobNodePath
					.getNodeFullPath(jobName, String.format(ServerNode.STOPONETIME, saturnContainer.getExecutorName()));

			if (regCenter.isExisted(path)) {
				regCenter.remove(path);
			}
			regCenter.persist(path, "1");
		}
	}

	public static void runAtOnceAndWaitShardingCompleted(final JobConfiguration jobConfiguration) throws Exception {
		runAtOnce(jobConfiguration.getJobName());
		Thread.sleep(1000L);

		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration);
			}

		}, 10);
	}

	public static void extractTraffic(String executorName) {
		regCenter.persist(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName), "");
	}

	public static void recoverTraffic(String executorName) {
		regCenter.remove(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName));
	}

	protected static void configJob(String jobName, String configPath, Object value) {
		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(configPath, value);
	}

	protected static void updateJobNode(JobConfiguration jobConfiguration, String node, String value) {
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		jobNodeStorage.updateJobNode(node, value);
	}

	protected static void updateJobConfig(JobConfiguration jobConfiguration, String configKey, Object configValue) {
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		jobNodeStorage.updateJobNode("config/" + configKey, configValue);
	}

	protected static String getJobNode(JobConfiguration jobConfiguration, String node) {
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		return jobNodeStorage.getJobNodeData(node);
	}

	protected static void removeJobNode(JobConfiguration jobConfiguration, String node) {
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		jobNodeStorage.removeJobNodeIfExisted(node);
	}

	protected static void doReport(JobConfiguration jobConfiguration) {
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		jobNodeStorage.fillJobNodeIfNullOrOverwrite("control/report", System.currentTimeMillis());
	}

	protected static void addJob(JobConfiguration jobConfiguration) {
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.JOB_TYPE, jobConfiguration.getJobType());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.SHARDING_TOTAL_COUNT,
				jobConfiguration.getShardingTotalCount());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.SHARDING_ITEM_PARAMETERS,
				jobConfiguration.getShardingItemParameters());
		jobNodeStorage
				.fillJobNodeIfNullOrOverwrite(ConfigurationNode.JOB_PARAMETER, jobConfiguration.getJobParameter());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.TIMEZONE, jobConfiguration.getTimeZone());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.CRON, jobConfiguration.getCron());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.PREFER_LIST, jobConfiguration.getPreferList());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.PAUSE_PERIOD_DATE,
				jobConfiguration.getPausePeriodDate());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.PAUSE_PERIOD_TIME,
				jobConfiguration.getPausePeriodTime());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.PROCESS_COUNT_INTERVAL_SECONDS,
				jobConfiguration.getProcessCountIntervalSeconds());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.FAILOVER, jobConfiguration.isFailover());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.LOAD_LEVEL, jobConfiguration.getLoadLevel());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.DESCRIPTION, jobConfiguration.getDescription());
		jobNodeStorage
				.fillJobNodeIfNullOrOverwrite(ConfigurationNode.TIMEOUTSECONDS, jobConfiguration.getTimeoutSeconds());
		jobNodeStorage
				.fillJobNodeIfNullOrOverwrite(ConfigurationNode.SHOW_NORMAL_LOG, jobConfiguration.isShowNormalLog());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.QUEUE_NAME, jobConfiguration.getQueueName());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.CHANNEL_NAME, jobConfiguration.getChannelName());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.USE_DISPREFER_LIST,
				jobConfiguration.isUseDispreferList());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.LOCAL_MODE, jobConfiguration.isLocalMode());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.USE_SERIAL, jobConfiguration.isUseSerial());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.ENABLED, jobConfiguration.isEnabled());
		jobNodeStorage.fillJobNodeIfNullOrOverwrite(ConfigurationNode.JOB_CLASS, jobConfiguration.getJobClass());
	}

	public static boolean isNeedSharding(JobConfiguration jobConfiguration) {
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		return jobNodeStorage.isJobNodeExisted(ShardingNode.NECESSARY) && !ShardingService.SHARDING_UN_NECESSARY
				.equals(jobNodeStorage.getJobNodeDataDirectly(ShardingNode.NECESSARY));
	}

	public static boolean isOnline(String executorName) {
		String path = "/$SaturnExecutors/executors/" + executorName + "/ip";
		return regCenter.isExisted(path);
	}

	public static boolean isFailoverAssigned(JobConfiguration jobConfiguration, Integer item) {
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		return jobNodeStorage.isJobNodeExisted(FailoverNode.getExecutionFailoverNode(item));
	}

	public static boolean noFailoverItems(JobConfiguration jobConfiguration) {
		JobNodeStorage jobNodeStorage = new JobNodeStorage(regCenter, jobConfiguration);
		return jobNodeStorage.getJobNodeChildrenKeys(FailoverNode.getFailoverItemsNode()).isEmpty();
	}

	public interface FinishCheck {
		boolean docheck();
	}

	public static void waitForFinish(FinishCheck check, int timeOutInSeconds) throws Exception {
		int count = 0;
		while (true) {
			if (check.docheck()) {
				break;
			}
			// Thread.sleep(1000);
			Thread.sleep(500); // Poll value. Note that a lot checks are done with zkServer, increase this to avoid some
			// side-effect
			count++;
			if (count > timeOutInSeconds * 2) {
				throw new Exception("time is up. check failed.");
			}
		}
	}

	public static void cleanJobs(String... jobNames) throws InterruptedException {
		for (String job : jobNames) {
			disableJob(job);
			Thread.sleep(1000);
			removeJob(job);
			Thread.sleep(1000);
		}
	}

	private static JobConfiguration initConfigOfShellMsgJob(String jobName, String queue, int shardCount,
			String shardingItemParameters) {
		JobConfiguration jobCfg = new JobConfiguration(jobName);
		jobCfg.setJobType(JobType.VSHELL.toString());
		jobCfg.setQueueName(queue);
		jobCfg.setProcessCountIntervalSeconds(1); // for quickly get statistics
		jobCfg.setShardingTotalCount(shardCount);
		jobCfg.setShardingItemParameters(shardingItemParameters);
		return jobCfg;
	}

	private static JobConfiguration initConfigOfJavaMsgJob(String jobName, String queue, int shardCount,
			String shardingItemParameters) {
		JobConfiguration jobCfg = new JobConfiguration(jobName);
		jobCfg.setJobType(JobType.MSG_JOB.toString());
		jobCfg.setQueueName(queue);
		jobCfg.setProcessCountIntervalSeconds(1); // for quickly get statistics
		jobCfg.setShardingTotalCount(shardCount);
		jobCfg.setShardingItemParameters(shardingItemParameters);
		return jobCfg;
	}

	private static JobConfiguration initConfigOfshellCronJob(String jobName, String cron, int shardCount,
			String shardingItemParameters) {
		JobConfiguration jobCfg = new JobConfiguration(jobName);
		jobCfg.setJobType(JobType.SHELL_JOB.toString());
		jobCfg.setCron(cron);
		jobCfg.setProcessCountIntervalSeconds(1); // for quickly get statistics
		jobCfg.setShardingTotalCount(shardCount);
		jobCfg.setShardingItemParameters(shardingItemParameters);
		return jobCfg;
	}

	private static JobConfiguration initConfigOfjavaCronJob(String jobName, String cron, int shardCount,
			String shardingItemParameters) {
		JobConfiguration jobCfg = new JobConfiguration(jobName);
		jobCfg.setJobType(JobType.JAVA_JOB.toString());
		jobCfg.setCron(cron);
		jobCfg.setProcessCountIntervalSeconds(1); // for quickly get statistics
		jobCfg.setShardingTotalCount(shardCount);
		jobCfg.setShardingItemParameters(shardingItemParameters);
		return jobCfg;
	}

	public static JobConfiguration addShellCronJob(String jobName, String cron, int shardCount,
			String shardingItemParameters) throws InterruptedException {
		JobConfiguration jobConfiguration = initConfigOfshellCronJob(jobName, cron, shardCount, shardingItemParameters);
		addJob(jobConfiguration);
		Thread.sleep(1000);
		return jobConfiguration;
	}

	public static JobConfiguration addShellCronJobWithChannel(String jobName, String cron, int shardCount,
			String shardingItemParameters, String channel) throws InterruptedException {
		JobConfiguration jobConfiguration = initConfigOfshellCronJob(jobName, cron, shardCount, shardingItemParameters);
		jobConfiguration.setChannelName(channel);
		addJob(jobConfiguration);
		Thread.sleep(1000);
		return jobConfiguration;
	}

	public static JobConfiguration addShellMsgJobWithQueue(String jobName, String queue, int shardCount,
			String shardingItemParameters) throws InterruptedException {
		JobConfiguration jobCfg = initConfigOfShellMsgJob(jobName, queue, shardCount, shardingItemParameters);
		addJob(jobCfg);
		Thread.sleep(1000);
		// updateJobNode(jobConfiguration, "config/cron", "0/1 * * * * ?");
		return jobCfg;
	}

	public static JobConfiguration addShellMsgJobWithQueueAndChannel(String jobName, String queue, String channel,
			int shardCount, String shardingItemParameters) throws InterruptedException {
		JobConfiguration jobCfg = initConfigOfShellMsgJob(jobName, queue, shardCount, shardingItemParameters);
		jobCfg.setChannelName(channel);
		addJob(jobCfg);
		Thread.sleep(1000);
		return jobCfg;
	}

	public static void waitForExecutorHasSuccessCount(final JobConfiguration jobConfiguration, final Main executor) {
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					String successCount = getSuccessCountOfExecutor(jobConfiguration, executor);
					// is null when begin
					return successCount != null && Integer.parseInt(successCount) >= 1;
				}
			}, timeout);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void waitForOldShellJobPidNotRunning(final String jobName, final int shardCount) {
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {

					for (int i = 0; i < saturnExecutorList.size(); i++) {
						Main saturnContainer = saturnExecutorList.get(i);
						for (int j = 0; j < shardCount; j++) {
							long pid = ScriptPidUtils
									.getFirstPidFromFile(saturnContainer.getExecutorName(), jobName, "" + j);
							if (pid > 0 && ScriptPidUtils.isPidRunning(pid)) {
								return false;
							}
						}
					}
					return true;
				}

			}, timeout);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static boolean assertExecutorFailureCountIncreaseInSpanTime(final JobConfiguration jobConfiguration,
			final Main executor, int spanTime) {
		final String failureCountBefore = getFailureCountOfExecutor(jobConfiguration, executor);
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					String failureCount = getFailureCountOfExecutor(jobConfiguration, executor);
					return !(failureCount == null || failureCount.equals(failureCountBefore));
				}
			}, spanTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean assertExecutorFailureCountIncrease(JobConfiguration jobConfiguration, final Main executor,
			final String failureCountBefore) {
		String failureCount = getFailureCountOfExecutor(jobConfiguration, executor);
		return !(failureCount == null || Integer.parseInt(failureCount) > Integer.parseInt(failureCountBefore));
	}

	public static void waitForExecutorProcessCountChanged(final JobConfiguration jobConfiguration,
			final Main executor) {
		final String successCountBefore = getSuccessCountOfExecutor(jobConfiguration, executor);
		final String failureCountBefore = getFailureCountOfExecutor(jobConfiguration, executor);
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					String successCount = getSuccessCountOfExecutor(jobConfiguration, executor);
					String failureCount = getFailureCountOfExecutor(jobConfiguration, executor);
					if ((successCount == null) || (failureCount == null)) {
						return false;
					} else {
						return !(successCount.equals(successCountBefore) && failureCount.equals(failureCountBefore));
					}
					// successCount.equals("0") || failureCount.equals("0")
				}
			}, timeout);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Too fast, not applicable
	 */
	private static void waitForJobHasRunningZnode(final String jobName, final int shardCount) {
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return hasRunningZnode(jobName, shardCount);
				}
			}, timeout);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void waitForJobHasCompletedZnode(final String jobName, final int shardCount) {
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return hasCompletedZnode(jobName, shardCount);
				}
			}, timeout);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wait until all shards having completed znode and wait some time
	 */
	public static void waitForJobFinish(final String jobName, final int shardCount) {
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return hasCompletedZnodeForAllShards(jobName, shardCount);
				}
			}, timeout);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static String getSuccessCountOfExecutor(JobConfiguration jobConfiguration, final Main executor) {
		return getJobNode(jobConfiguration, "servers/" + executor.getExecutorName() + "/processSuccessCount");
	}

	public static String getFailureCountOfExecutor(JobConfiguration jobConfiguration, final Main executor) {
		return getJobNode(jobConfiguration, "servers/" + executor.getExecutorName() + "/processFailureCount");
	}

	/**
	 * return true if any shard has timeout znode
	 */
	private static boolean hasTimeoutZnode(String jobName, int shardCount) {
		for (int i = 0; i < shardCount; i++) {
			if (regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getTimeoutNode(i)))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * return true if any shard has "fail" znode
	 */
	private static boolean hasFailZnode(String jobName, int shardCount) {
		for (int i = 0; i < shardCount; i++) {
			if (regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getFailedNode(i)))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * return true if any shard has "completed" znode
	 */
	private static boolean hasCompletedZnode(String jobName, int shardCount) {
		for (int i = 0; i < shardCount; i++) {
			if (regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getCompletedNode(i)))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * return true if any shard has "completed" znode
	 */
	protected static boolean hasCompletedZnodeForAllShards(String jobName, int shardCount) {
		for (int i = 0; i < shardCount; i++) {
			if (!regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getCompletedNode(i)))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * return true if any shard has "running" znode
	 */
	private static boolean hasRunningZnode(String jobName, int shardCount) {
		for (int i = 0; i < shardCount; i++) {
			if (regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getRunningNode(i)))) {
				return true;
			}
		}
		return false;
	}

	public static boolean assertTimeoutZnodeExists(final String jobName, final int shardCount, int timeout)
			throws Exception {
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return hasTimeoutZnode(jobName, shardCount);
			}
		}, timeout);
		return false;
	}

	private static void waitForMsgTotalCountChanged(final String queueUrl, final String userName, final String password)
			throws Exception {
		final String msgTotalCountBefore = StringUtils
				.substringBetween(getRestData(queueUrl, userName, password), "messages\":", ",\"messages_details");
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					String msgTotalCountNow;
					try {
						msgTotalCountNow = StringUtils
								.substringBetween(getRestData(queueUrl, userName, password), "messages\":",
										",\"messages_details");
						return !msgTotalCountNow.equals(msgTotalCountBefore);
					} catch (IOException e) {
						log.error("Cannot get msgTotalCount!");
						e.printStackTrace();
					}
					return false;
				}
			}, timeout);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getRestData(String url, String userName, String password) throws IOException {
		return getRestData(url, userName, password, "GET");
	}

	public static String getRestData(String url, String userName, String password, String method) throws IOException {
		URL getUrl = new URL(url);
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		try {
			connection = (HttpURLConnection) getUrl.openConnection();

			if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(password)) {
				String authString = userName + ":" + password;
				connection.setRequestProperty("Authorization",
						"Basic " + SaturnBaseEncoder.getInstance().encodeToString(authString.getBytes()));
			}
			connection.setRequestMethod(method);
			connection.connect();

			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

			StringBuffer buffer = new StringBuffer();
			String temp;

			while ((temp = reader.readLine()) != null) {
				buffer.append(temp).append(System.lineSeparator());
			}
			return buffer.toString();
		} catch (Exception ex) {
			log.warn(getUrl + " in getData has error", ex.getMessage());
			return "";
		} finally {
			if (reader != null) {
				reader.close();
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
