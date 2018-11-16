package com.vip.saturn.it.base;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.it.utils.NestedZkUtils;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.RequestResultHelper;
import com.vip.saturn.job.console.springboot.SaturnConsoleApp;
import com.vip.saturn.job.console.utils.SaturnConstants;
import com.vip.saturn.job.console.vo.UpdateJobConfigVo;
import com.vip.saturn.job.executor.ExecutorConfig;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.executor.SaturnExecutor;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.failover.FailoverNode;
import com.vip.saturn.job.internal.sharding.ShardingNode;
import com.vip.saturn.job.internal.sharding.ShardingService;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.reg.zookeeper.ZookeeperConfiguration;
import com.vip.saturn.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.io.FileUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vip.saturn.it.utils.HttpClientUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hebelala
 */
public class AbstractSaturnIT {

	protected static final String NAMESPACE = "it-saturn";
	protected static Logger log;
	protected static ZookeeperRegistryCenter regCenter;
	protected static NestedZkUtils nestedZkUtils;
	protected static List<Main> saturnExecutorList = new ArrayList<>();
	protected static List<SaturnConsoleInstance> saturnConsoleInstanceList = new ArrayList<>();

	protected static final int DEFAULT_JOB_CAN_BE_DELETE_TIME_LIMIT = SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT;

	@BeforeClass
	public static void beforeClass() throws Exception {
		initSysEnv();
		initZK();
		prepare4Console();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		regCenter.close();
		nestedZkUtils.stopServer();
		stopConsoleDb();
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
		setJOB_CAN_BE_DELETE_TIME_LIMIT();
		prepareForItSql();
		SaturnConsoleApp.startEmbeddedDb();
	}

	private static void prepareForItSql() throws IOException {
		String itSqlFilePath = "/db/h2/custom.sql";
		URL resource = AbstractSaturnIT.class.getResource(itSqlFilePath);
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

	protected static void setJOB_CAN_BE_DELETE_TIME_LIMIT() {
		SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT = 0;
	}

	protected static void resetJOB_CAN_BE_DELETE_TIME_LIMIT() {
		SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT = DEFAULT_JOB_CAN_BE_DELETE_TIME_LIMIT;
	}

	public static void stopConsoleDb() {
		SaturnConsoleApp.stopEmbeddedDb();
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

	public Main startExecutor(int index) throws Exception {
		assertThat(nestedZkUtils.isStarted());
		Main main = saturnExecutorList.get(index);
		if (main != null) {
			// 如果节点正在运行则退出
			log.warn("the executor{} already exist.", index);
			return main;
		} else {
			main = new Main();
			String executorName = "executorName" + index;
			String[] args = {"-namespace", NAMESPACE, "-executorName", executorName};
			main.launchInner(args, Main.class.getClassLoader(), Main.class.getClassLoader());
			saturnExecutorList.set(index, main);
			Thread.sleep(1000);
			return main;
		}
	}

	public void stopExecutor(int index) throws Exception {
		assertThat(saturnExecutorList.size()).isGreaterThan(index);
		Main main = saturnExecutorList.get(index);
		if (main != null) {
			main.shutdown();
		} else {
			log.warn("the {} Executor has stopped", index);
		}
		saturnExecutorList.set(index, null);
		for (Main tmp : saturnExecutorList) {
			if (tmp != null) {
				return;
			}
		}
		saturnExecutorList.clear();
	}

	public static void stopExecutorGracefully(int index) throws Exception {
		assertThat(saturnExecutorList.size()).isGreaterThan(index);
		Main main = saturnExecutorList.get(index);
		if (main != null) {
			main.shutdownGracefully();
		} else {
			log.warn("the {} Executor has stopped", index);
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
			Main main = saturnExecutorList.get(i);
			if (main != null) {
				main.shutdown();
			}
		}
		saturnExecutorList.clear();
	}

	public static void stopExecutorListGracefully() throws Exception {
		for (int i = 0; i < saturnExecutorList.size(); i++) {
			Main main = saturnExecutorList.get(i);
			if (main != null) {
				main.shutdownGracefully();
			}
		}
		saturnExecutorList.clear();
	}

	public ExecutorConfig getExecutorConfig(int index) {
		assertThat(saturnExecutorList.size()).isGreaterThan(index);
		Main main = saturnExecutorList.get(index);
		SaturnExecutor saturnExecutor = (SaturnExecutor) main.getSaturnExecutor();
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

	private static void refreshRegCenter(String url) throws Exception {
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

	public static void stopSaturnConsole(int index) {
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

	public CoordinatorRegistryCenter getExecutorRegistryCenter(Main executorMain) {
		return ((SaturnExecutor) executorMain.getSaturnExecutor()).getSaturnExecutorService()
				.getCoordinatorRegistryCenter();
	}

	public void stopZkServer() throws IOException {
		nestedZkUtils.stopServer();
	}

	public void killSession(CuratorFramework client) throws Exception {
		nestedZkUtils.killSession(client.getZookeeperClient().getZooKeeper());
	}

	private SaturnConsoleInstance getAvailableSaturnConsoleInstance() {
		for (SaturnConsoleInstance temp : saturnConsoleInstanceList) {
			if (temp != null) {
				return temp;
			}
		}
		return null;
	}

	private void checkGuiResponseEntity(GuiResponseEntity guiResponseEntity) throws Exception {
		if (guiResponseEntity.getStatusCode() != 200 || guiResponseEntity.getRequestResult().getStatus() != 0) {
			throw new Exception(guiResponseEntity.getRequestResult().getMessage());
		}
	}

	public void enableJob(String jobName) throws Exception {
		checkGuiResponseEntity(toGuiResponseEntity(sendPostRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/jobs/" + jobName
						+ "/enable", null)));
	}

	public void disableJob(String jobName) throws Exception {
		checkGuiResponseEntity(toGuiResponseEntity(sendPostRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/jobs/" + jobName
						+ "/disable", null)));
	}

	public void removeJob(String jobName) throws Exception {
		checkGuiResponseEntity(toGuiResponseEntity(sendDeleteRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/jobs/" + jobName)));
	}

	public void updateJob(UpdateJobConfigVo updateJobConfigVo) throws Exception {
		checkGuiResponseEntity(toGuiResponseEntity(sendPostRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/jobs/"
						+ updateJobConfigVo.getJobName() + "/config", toMap(updateJobConfigVo))));
	}

	public void enableReport(String jobName) throws Exception {
		UpdateJobConfigVo updateJobConfigVo = new UpdateJobConfigVo();
		updateJobConfigVo.setJobName(jobName);
		updateJobConfigVo.setEnabledReport(true);
		updateJob(updateJobConfigVo);
	}

	public void disableReport(String jobName) throws Exception {
		UpdateJobConfigVo updateJobConfigVo = new UpdateJobConfigVo();
		updateJobConfigVo.setJobName(jobName);
		updateJobConfigVo.setEnabledReport(false);
		updateJob(updateJobConfigVo);
	}

	public void runAtOnce(String jobName) throws Exception {
		checkGuiResponseEntity(toGuiResponseEntity(sendPostRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/jobs/" + jobName
						+ "/config/runAtOnce", null)));
	}

	public void forceStopJob(String jobName) throws Exception {
		checkGuiResponseEntity(toGuiResponseEntity(sendPostRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/jobs/" + jobName
						+ "/config/stopAtOnce", null)));
	}

	public void runAtOnceAndWaitShardingCompleted(final String jobName) throws Exception {
		runAtOnce(jobName);
		waitForFinish(new FinishCheck() {

			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}

		}, 10);
	}

	public void extractTraffic(String executorName) throws Exception {
		Map<String, Object> params = new HashMap<>();
		params.put("operation", "extract");
		checkGuiResponseEntity(toGuiResponseEntity(sendPostRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/executors/"
						+ executorName + "/traffic", params)));
	}

	public void recoverTraffic(String executorName) throws Exception {
		Map<String, Object> params = new HashMap<>();
		params.put("operation", "recover");
		checkGuiResponseEntity(toGuiResponseEntity(sendPostRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/executors/"
						+ executorName + "/traffic", params)));
	}

	public void doReport(String jobName) throws Exception {
		checkGuiResponseEntity(toGuiResponseEntity(sendGetRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/jobs/" + jobName
						+ "/execution/status")));
	}

	public void addJob(JobConfig jobConfig) throws Exception {
		checkGuiResponseEntity(toGuiResponseEntity(sendPostRequest(
				getAvailableSaturnConsoleInstance().url + "/console/namespaces/" + NAMESPACE + "/jobs/jobs",
				toMap(jobConfig))));
	}

	private Map<String, Object> toMap(JobConfig jobConfig) {
		Map<String, Object> params = new HashMap<>();
		putIfNotNull(params, "jobName", jobConfig.getJobName());
		putIfNotNull(params, "jobClass", jobConfig.getJobClass());
		putIfNotNull(params, "shardingTotalCount", jobConfig.getShardingTotalCount());
		putIfNotNull(params, "timeZone", jobConfig.getTimeZone());
		putIfNotNull(params, "cron", jobConfig.getCron());
		putIfNotNull(params, "pausePeriodDate", jobConfig.getPausePeriodDate());
		putIfNotNull(params, "pausePeriodTime", jobConfig.getPausePeriodTime());
		putIfNotNull(params, "shardingItemParameters", jobConfig.getShardingItemParameters());
		putIfNotNull(params, "jobParameter", jobConfig.getJobParameter());
		putIfNotNull(params, "processCountIntervalSeconds", jobConfig.getProcessCountIntervalSeconds());
		putIfNotNull(params, "description", jobConfig.getDescription());
		putIfNotNull(params, "timeout4AlarmSeconds", jobConfig.getTimeout4AlarmSeconds());
		putIfNotNull(params, "timeoutSeconds", jobConfig.getTimeoutSeconds());
		putIfNotNull(params, "showNormalLog", jobConfig.getShowNormalLog());
		putIfNotNull(params, "channelName", jobConfig.getChannelName());
		putIfNotNull(params, "jobType", jobConfig.getJobType());
		putIfNotNull(params, "queueName", jobConfig.getQueueName());
		putIfNotNull(params, "loadLevel", jobConfig.getLoadLevel());
		putIfNotNull(params, "jobDegree", jobConfig.getJobDegree());
		putIfNotNull(params, "enabledReport", jobConfig.getEnabledReport());
		putIfNotNull(params, "enabled", jobConfig.getEnabled());
		putIfNotNull(params, "preferList", jobConfig.getPreferList());
		putIfNotNull(params, "useDispreferList", jobConfig.getUseDispreferList());
		putIfNotNull(params, "localMode", jobConfig.getLocalMode());
		putIfNotNull(params, "useSerial", jobConfig.getUseSerial());
		putIfNotNull(params, "failover", jobConfig.getFailover());
		putIfNotNull(params, "jobMode", jobConfig.getJobMode());
		putIfNotNull(params, "dependencies", jobConfig.getDependencies());
		putIfNotNull(params, "groups", jobConfig.getGroups());
		putIfNotNull(params, "rerun", jobConfig.getRerun());
		putIfNotNull(params, "downStream", jobConfig.getDownStream());
		return params;
	}

	private Map<String, Object> toMap(UpdateJobConfigVo updateJobConfigVo) {
		Map<String, Object> params = new HashMap<>();
		putIfNotNull(params, "jobName", updateJobConfigVo.getJobName());
		putIfNotNull(params, "jobClass", updateJobConfigVo.getJobClass());
		putIfNotNull(params, "shardingTotalCount", updateJobConfigVo.getShardingTotalCount());
		putIfNotNull(params, "timeZone", updateJobConfigVo.getTimeZone());
		putIfNotNull(params, "cron", updateJobConfigVo.getCron());
		putIfNotNull(params, "pausePeriodDate", updateJobConfigVo.getPausePeriodDate());
		putIfNotNull(params, "pausePeriodTime", updateJobConfigVo.getPausePeriodTime());
		putIfNotNull(params, "shardingItemParameters", updateJobConfigVo.getShardingItemParameters());
		putIfNotNull(params, "jobParameter", updateJobConfigVo.getJobParameter());
		putIfNotNull(params, "processCountIntervalSeconds", updateJobConfigVo.getProcessCountIntervalSeconds());
		putIfNotNull(params, "description", updateJobConfigVo.getDescription());
		putIfNotNull(params, "timeout4AlarmSeconds", updateJobConfigVo.getTimeout4AlarmSeconds());
		putIfNotNull(params, "timeoutSeconds", updateJobConfigVo.getTimeoutSeconds());
		putIfNotNull(params, "showNormalLog", updateJobConfigVo.getShowNormalLog());
		putIfNotNull(params, "channelName", updateJobConfigVo.getChannelName());
		putIfNotNull(params, "jobType", updateJobConfigVo.getJobType());
		putIfNotNull(params, "queueName", updateJobConfigVo.getQueueName());
		putIfNotNull(params, "loadLevel", updateJobConfigVo.getLoadLevel());
		putIfNotNull(params, "jobDegree", updateJobConfigVo.getJobDegree());
		putIfNotNull(params, "enabledReport", updateJobConfigVo.getEnabledReport());
		putIfNotNull(params, "enabled", updateJobConfigVo.getEnabled());
		putIfNotNull(params, "preferList", updateJobConfigVo.getPreferList());
		putIfNotNull(params, "onlyUsePreferList", updateJobConfigVo.getOnlyUsePreferList());
		putIfNotNull(params, "localMode", updateJobConfigVo.getLocalMode());
		putIfNotNull(params, "useSerial", updateJobConfigVo.getUseSerial());
		putIfNotNull(params, "failover", updateJobConfigVo.getFailover());
		putIfNotNull(params, "jobMode", updateJobConfigVo.getJobMode());
		putIfNotNull(params, "dependencies", updateJobConfigVo.getDependencies());
		putIfNotNull(params, "groups", updateJobConfigVo.getGroups());
		putIfNotNull(params, "rerun", updateJobConfigVo.getRerun());
		putIfNotNull(params, "downStream", updateJobConfigVo.getDownStream());
		return params;
	}

	private void putIfNotNull(Map<String, Object> map, String key, Object value) {
		if (key != null && value != null) {
			map.put(key, value);
		}
	}

	public void zkAddJob(JobConfig jobConfig) {
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.SHARDING_TOTAL_COUNT),
				jobConfig.getShardingTotalCount());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.TIMEZONE),
				jobConfig.getTimeZone());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.CRON),
				jobConfig.getCron());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.PAUSE_PERIOD_DATE),
				jobConfig.getPausePeriodDate());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.PAUSE_PERIOD_TIME),
				jobConfig.getPausePeriodTime());
		persistIfNotNull(
				JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.SHARDING_ITEM_PARAMETERS),
				jobConfig.getShardingItemParameters());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.JOB_PARAMETER),
				jobConfig.getJobParameter());
		persistIfNotNull(
				JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.PROCESS_COUNT_INTERVAL_SECONDS),
				jobConfig.getProcessCountIntervalSeconds());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.DESCRIPTION),
				jobConfig.getDescription());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), "config/timeout4AlarmSeconds"),
				jobConfig.getTimeout4AlarmSeconds());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.TIMEOUTSECONDS),
				jobConfig.getTimeZone());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.SHOW_NORMAL_LOG),
				jobConfig.getShowNormalLog());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.CHANNEL_NAME),
				jobConfig.getChannelName());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.JOB_TYPE),
				jobConfig.getJobType());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.QUEUE_NAME),
				jobConfig.getQueueName());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.LOAD_LEVEL),
				jobConfig.getLoadLevel());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), "config/jobDegree"),
				jobConfig.getJobDegree());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.ENABLED_REPORT),
				jobConfig.getEnabledReport());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.ENABLED),
				jobConfig.getEnabled());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.PREFER_LIST),
				jobConfig.getPreferList());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.USE_DISPREFER_LIST),
				jobConfig.getUseDispreferList());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.LOCAL_MODE),
				jobConfig.getLocalMode());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.USE_SERIAL),
				jobConfig.getUseSerial());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.FAILOVER),
				jobConfig.getFailover());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), "config/jobMode"), jobConfig.getJobMode());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), "config/dependencies"),
				jobConfig.getDependencies());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), "config/groups"), jobConfig.getGroups());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), "config/rerun"), jobConfig.getRerun());
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.DOWN_STREAM),
				jobConfig.getDownStream());

		// be careful, should persist jobClass at last
		persistIfNotNull(JobNodePath.getNodeFullPath(jobConfig.getJobName(), ConfigurationNode.JOB_CLASS),
				jobConfig.getJobClass());
	}

	private void persistIfNotNull(String node, Object value) {
		if (value != null) {
			regCenter.persist(node, String.valueOf(value));
		}
	}

	public void zkRemoveJob(String jobName) {
		regCenter.persist(JobNodePath.getNodeFullPath(jobName, ConfigurationNode.TO_DELETE), "1");
	}

	public String zkGetJobNode(String jobName, String node) {
		return regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName, node));
	}

	public List<String> zkGetJobNodeChildren(String jobName, String node) {
		return regCenter.getChildrenKeys(JobNodePath.getNodeFullPath(jobName, node));
	}

	public boolean zkExistingJobNode(String jobName, String node) {
		return regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, node));
	}

	public void zkUpdateJobNode(String jobName, String node, String value) {
		regCenter.persist(JobNodePath.getNodeFullPath(jobName, node), value);
	}

	public void zkRemoveJobNode(String jobName, String node) {
		regCenter.remove(JobNodePath.getNodeFullPath(jobName, node));
	}

	public boolean isNeedSharding(String jobName) {
		return zkExistingJobNode(jobName, ShardingNode.NECESSARY) && !ShardingService.SHARDING_UN_NECESSARY
				.equals(zkGetJobNode(jobName, ShardingNode.NECESSARY));
	}

	public boolean isOnline(String executorName) {
		String path = "/$SaturnExecutors/executors/" + executorName + "/ip";
		return regCenter.isExisted(path);
	}

	public boolean isFailoverAssigned(String jobName, Integer item) {
		return zkExistingJobNode(jobName, FailoverNode.getExecutionFailoverNode(item));
	}

	public boolean noFailoverItems(String jobName) {
		return zkGetJobNodeChildren(jobName, FailoverNode.getFailoverItemsNode()).isEmpty();
	}

	public boolean hasCompletedZnodeForAllShards(String jobName, int shardCount) {
		for (int i = 0; i < shardCount; i++) {
			if (!zkExistingJobNode(jobName, ExecutionNode.getCompletedNode(i))) {
				return false;
			}
		}
		return true;
	}

	public void waitForFinish(FinishCheck finishCheck, int timeOutInSeconds) throws Exception {
		int count = 0;
		while (true) {
			if (finishCheck.isOk()) {
				break;
			}
			Thread.sleep(500);
			count++;
			if (count > timeOutInSeconds * 2) {
				throw new Exception("time is up. check failed.");
			}
		}
	}

}
