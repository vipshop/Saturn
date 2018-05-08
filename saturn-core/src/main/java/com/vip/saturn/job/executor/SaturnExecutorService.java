package com.vip.saturn.job.executor;

import com.vip.saturn.job.exception.TimeDiffIntolerableException;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.LocalHostService;
import com.vip.saturn.job.utils.SaturnVersionUtils;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.CloseableExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * @author xiaopeng.he
 */
public class SaturnExecutorService {

	public static final int WAIT_JOBCLASS_ADDED_COUNT = 25;

	private static final int WAIT_FOR_IP_NODE_DISAPPEAR_COUNT = 150;

	private static Logger log = LoggerFactory.getLogger(SaturnExecutorService.class);

	private String executorName;
	private CoordinatorRegistryCenter coordinatorRegistryCenter;
	private SaturnExecutorExtension saturnExecutorExtension;

	private List<String> jobNames = new ArrayList<>();
	private TreeCache jobsTreeCache;
	private String ipNode;
	private ClassLoader jobClassLoader;
	private ClassLoader executorClassLoader;
	private ExecutorConfigService executorConfigService;
	private RestartAndDumpService restartExecutorService;

	public SaturnExecutorService(CoordinatorRegistryCenter coordinatorRegistryCenter, String executorName,
			SaturnExecutorExtension saturnExecutorExtension) {
		this.coordinatorRegistryCenter = coordinatorRegistryCenter;
		this.executorName = executorName;
		this.saturnExecutorExtension = saturnExecutorExtension;
		if (coordinatorRegistryCenter != null) {
			coordinatorRegistryCenter.setExecutorName(executorName);
		}
	}

	private void registerExecutor0() throws Exception {
		String executorNode = SaturnExecutorsNode.EXECUTORS_ROOT + "/" + executorName;
		ipNode = executorNode + "/ip";
		String lastBeginTimeNode = executorNode + "/lastBeginTime";
		String versionNode = executorNode + "/version";
		String executorCleanNode = executorNode + "/clean";
		String executorTaskNode = executorNode + "/task";

		// 持久化最近启动时间
		coordinatorRegistryCenter.persist(lastBeginTimeNode, String.valueOf(System.currentTimeMillis()));
		String executorVersion = SaturnVersionUtils.getVersion();
		// 持久化版本
		if (executorVersion != null) {
			coordinatorRegistryCenter.persist(versionNode, executorVersion);
		}

		// 持久化clean
		coordinatorRegistryCenter
				.persist(executorCleanNode, String.valueOf(SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN));

		// 持久task
		if (StringUtils.isNotBlank(SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID)) {
			log.info("persist znode '/task': {}", SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID);
			coordinatorRegistryCenter.persist(executorTaskNode, SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID);
		}

		// 获取配置并添加watcher
		if (executorConfigService != null) {
			executorConfigService.stop();
		}
		executorConfigService = new ExecutorConfigService(executorName, coordinatorRegistryCenter,
				saturnExecutorExtension.getExecutorConfigClass());
		executorConfigService.start();

		// add watcher for restart
		if (restartExecutorService != null) {
			restartExecutorService.stop();
		}
		restartExecutorService = new RestartAndDumpService(executorName, coordinatorRegistryCenter);
		restartExecutorService.start();

		// 持久化ip
		coordinatorRegistryCenter.persistEphemeral(ipNode, LocalHostService.cachedIpAddress);
	}

	public void registerExecutor() throws Exception {
		checkExecutor();
		registerExecutor0();
	}

	/**
	 * 启动前先检查本机与注册中心的时间误差秒数是否在允许范围和Executor是否已启用
	 */
	private void checkExecutor() throws Exception {
		// 启动时检查本机与注册中心的时间误差秒数是否在允许范围
		String executorNode = SaturnExecutorsNode.EXECUTORS_ROOT + "/" + executorName;
		try {
			long timeDiff = Math.abs(System.currentTimeMillis() - coordinatorRegistryCenter
					.getRegistryCenterTime(executorNode + "/systemTime/current"));
			int maxTimeDiffSeconds = 60;
			if (timeDiff > maxTimeDiffSeconds * 1000L) {
				Long timeDiffSeconds = Long.valueOf(timeDiff / 1000);
				throw new TimeDiffIntolerableException(timeDiffSeconds.intValue(), maxTimeDiffSeconds);
			}
		} finally {
			String executorSystemTimePath = executorNode + "/systemTime";
			coordinatorRegistryCenter.remove(executorSystemTimePath);
		}
		// 启动时检查Executor是否已启用（ExecutorName为判断的唯一标识）
		if (coordinatorRegistryCenter.isExisted(executorNode)) {
			int count = 0;
			do {
				if (!coordinatorRegistryCenter.isExisted(executorNode + "/ip")) {
					return;
				}

				log.warn("{}/ip node found. Try to sleep and wait for this node disappear.", executorNode);
				Thread.sleep(100L);
			} while (++count <= WAIT_FOR_IP_NODE_DISAPPEAR_COUNT);

			throw new Exception("The executor (" + executorName + ") is running, cannot running the instance twice.");
		} else {
			coordinatorRegistryCenter.persist(executorNode, "");
		}
	}

	/**
	 * Register NewJobCallback, and async start existing jobs. The TreeCache will publish the create events for already
	 * existing jobs.
	 */
	public void registerCallbackAndStartExistingJob(final ScheduleNewJobCallback callback) throws Exception {
		jobsTreeCache = TreeCache
				.newBuilder((CuratorFramework) coordinatorRegistryCenter.getRawClient(), JobNodePath.ROOT).setExecutor(
						new CloseableExecutorService(Executors.newSingleThreadExecutor(
								new SaturnThreadFactory(executorName + "-$Jobs-watcher", false)), true)).setMaxDepth(1)
				.build();
		jobsTreeCache.getListenable().addListener(new TreeCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
				if (event == null) {
					return;
				}

				ChildData data = event.getData();
				if (data == null) {
					return;
				}

				String path = data.getPath();
				if (path == null || path.equals(JobNodePath.ROOT)) {
					return;
				}

				Type type = event.getType();
				if (type == null || !type.equals(Type.NODE_ADDED)) {
					return;
				}

				String jobName = StringUtils.substringAfterLast(path, "/");
				String jobClassPath = JobNodePath.getNodeFullPath(jobName, ConfigurationNode.JOB_CLASS);
				// wait 5 seconds at most until jobClass created.
				for (int i = 0; i < WAIT_JOBCLASS_ADDED_COUNT; i++) {
					if (client.checkExists().forPath(jobClassPath) == null) {
						Thread.sleep(200);
						continue;
					}

					log.info("new job: {} 's jobClass created event received", jobName);
					if (!jobNames.contains(jobName)) {
						if (callback.call(jobName)) {
							jobNames.add(jobName);
							log.info("the job {} initialize successfully", jobName);
						} else {
							log.warn("the job {} initialize fail", jobName);
						}
					} else {
						log.warn("the job {} is unnecessary to initialize, because it's already existing", jobName);
					}
					break;
				}
			}
		});
		jobsTreeCache.start();
	}

	public void removeJobName(String jobName) {
		if (jobNames.contains(jobName)) {
			jobNames.remove(jobName);
		}
	}

	private void stopRestartExecutorService() {
		try {
			if (restartExecutorService != null) {
				restartExecutorService.stop();
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	private void stopExecutorConfigService() {
		try {
			if (executorConfigService != null) {
				executorConfigService.stop();
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	private void removeIpNode() {
		try {
			if (coordinatorRegistryCenter != null && ipNode != null && coordinatorRegistryCenter.isConnected()) {
				log.info("{} is going to delete its ip node {}", executorName, ipNode);
				coordinatorRegistryCenter.remove(ipNode);
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	private void closeJobsTreeCache() {
		try {
			if (jobsTreeCache != null) {
				jobsTreeCache.close();
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	// Attention, catch Throwable and not throw it.
	public void shutdown() {
		stopRestartExecutorService();
		stopExecutorConfigService();
		removeIpNode();
		closeJobsTreeCache();
	}

	public CoordinatorRegistryCenter getCoordinatorRegistryCenter() {
		return coordinatorRegistryCenter;
	}

	public String getIpNode() {
		return ipNode;
	}

	public ClassLoader getJobClassLoader() {
		return jobClassLoader;
	}

	public void setJobClassLoader(ClassLoader jobClassLoader) {
		this.jobClassLoader = jobClassLoader;
	}

	public ClassLoader getExecutorClassLoader() {
		return executorClassLoader;
	}

	public void setExecutorClassLoader(ClassLoader executorClassLoader) {
		this.executorClassLoader = executorClassLoader;
	}

	public String getExecutorName() {
		return executorName;
	}

	public List<String> getJobNames() {
		return jobNames;
	}

	public void setJobNames(List<String> jobNames) {
		this.jobNames = jobNames;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public void setCoordinatorRegistryCenter(CoordinatorRegistryCenter coordinatorRegistryCenter) {
		this.coordinatorRegistryCenter = coordinatorRegistryCenter;
	}

	public void setIpNode(String ipNode) {
		this.ipNode = ipNode;
	}

	public ExecutorConfig getExecutorConfig() {
		return executorConfigService == null ? new ExecutorConfig() : executorConfigService.getExecutorConfig();
	}

}
