package com.vip.saturn.job.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

import com.vip.saturn.job.threads.SaturnThreadFactory;
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

import com.google.common.base.Strings;
import com.vip.saturn.job.exception.TimeDiffIntolerableException;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.utils.LocalHostService;
import com.vip.saturn.job.utils.ResourceUtils;
import com.vip.saturn.job.utils.SystemEnvProperties;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class SaturnExecutorService {

	static Logger log = LoggerFactory.getLogger(SaturnExecutorService.class);
	
	private String executorName;
	
	private List<String> jobNames = new ArrayList<String>();

	private CoordinatorRegistryCenter coordinatorRegistryCenter;

	private TreeCache $JobsTreeCache;

	private String ipNode;

	private ClassLoader jobClassLoader;

	private ClassLoader executorClassLoader;

	public static final int WAIT_JOBCLASS_ADDED_COUNT =  25;

	public SaturnExecutorService(CoordinatorRegistryCenter coordinatorRegistryCenter, String executorName) {
		this.coordinatorRegistryCenter = coordinatorRegistryCenter;
		this.executorName = executorName;
		if (coordinatorRegistryCenter != null) {
			coordinatorRegistryCenter.setExecutorName(executorName);
		}
	}

	/**
	 * 获取该域下所有作业名
	 */
	public List<String> registerJobNames() {
		jobNames.clear();
		// be careful, coordinatorRegistryCenter.getChildrenKeys maybe return Collections.emptyList(), it's immutable
		jobNames.addAll(coordinatorRegistryCenter.getChildrenKeys("/" + JobNodePath.$JOBS_NODE_NAME));

		return jobNames;
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
		// 持久化版本
		final Properties props = ResourceUtils.getResource("properties/saturn-core.properties");
		if (props != null) {
			String executorVersion = props.getProperty("build.version");
			if (!Strings.isNullOrEmpty(executorVersion)) {
				coordinatorRegistryCenter.persist(versionNode, executorVersion);
			}
		}
		// 持久化clean
		coordinatorRegistryCenter.persist(executorCleanNode,
				String.valueOf(SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN));

		// 持久task
		if (SystemEnvProperties.VIP_SATURN_DCOS_TASK != null) {
			coordinatorRegistryCenter.persist(executorTaskNode, SystemEnvProperties.VIP_SATURN_DCOS_TASK);
		}

		coordinatorRegistryCenter.persistEphemeral(ipNode, LocalHostService.cachedIpAddress);

	}

	public void reRegister() throws Exception {
		registerJobNames();
		registerExecutor0();
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
			long timeDiff = Math.abs(System.currentTimeMillis()
					- coordinatorRegistryCenter.getRegistryCenterTime(executorNode + "/systemTime/current"));
			int maxTimeDiffSeconds = 60;
			if (timeDiff > maxTimeDiffSeconds * 1000L) {
				Long timeDiffSeconds = Long.valueOf(timeDiff / 1000);
				throw new TimeDiffIntolerableException(timeDiffSeconds.intValue(), maxTimeDiffSeconds);
			}
		} finally {
			String executorSystemTimePath = executorNode + "/systemTime";
			if (coordinatorRegistryCenter.isExisted(executorSystemTimePath)) {
				coordinatorRegistryCenter.remove(executorSystemTimePath);
			}
		}
		// 启动时检查Executor是否已启用（ExecutorName为判断的唯一标识）
		if (coordinatorRegistryCenter.isExisted(executorNode)) {
			if (coordinatorRegistryCenter.isExisted(executorNode + "/ip")) { // is running
				throw new Exception(
						"The executor name(" + executorName + ") is running, cannot running the instance twice.");
			}
		} else {
			coordinatorRegistryCenter.persist(executorNode, "");
		}
	}

	private TreeCache buildAndStart$JobsTreeCache(CuratorFramework client) throws Exception {
		TreeCache tc = TreeCache.newBuilder(client, "/" + JobNodePath.$JOBS_NODE_NAME)
				.setExecutor(new CloseableExecutorService(Executors.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-$Jobs-watcher", false)), true))
				.setMaxDepth(1)
				.build();
		tc.start();
		return tc;
	}

	public void addNewJobListenerCallback(final ScheduleNewJobCallback callback) throws Exception {
		$JobsTreeCache = buildAndStart$JobsTreeCache((CuratorFramework) coordinatorRegistryCenter.getRawClient());
		$JobsTreeCache.getListenable().addListener(new TreeCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
				if (event != null) {
					Type type = event.getType();
					ChildData data = event.getData();
					if (type != null && data != null) {
						String path = data.getPath();
						if (path != null && !path.equals("/" + JobNodePath.$JOBS_NODE_NAME)) {
							if (type.equals(Type.NODE_ADDED)) { // add a job
								String jobName = StringUtils.substringAfterLast(path, "/");
								String jobClassPath = JobNodePath.getNodeFullPath(jobName, ConfigurationNode.JOB_CLASS);
								// wait 5 seconds at most until jobClass created.
								for (int i = 0; i < WAIT_JOBCLASS_ADDED_COUNT; i ++) {
									if (client.checkExists().forPath(jobClassPath) == null) {
										Thread.sleep(200);
									} else {
										log.info("new job: {} 's jobClass created event received", jobName);
										if (!jobNames.contains(jobName)) {
											if(callback.call(jobName)) {
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
							}
						}
					}
				}
			}
		});
	}

	public void removeJobName(String jobName) {
		if (jobNames.contains(jobName)) {
			jobNames.remove(jobName);
		}
	}

	private void removeIpNode() {
		try {
			if (coordinatorRegistryCenter != null && ipNode != null && coordinatorRegistryCenter.isConnected()) {
				log.info(" {} is going to delete its ip node {}", executorName, ipNode);
				coordinatorRegistryCenter.remove(ipNode);
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	private void close$JobsTreeCache() {
		try {
			if ($JobsTreeCache != null) {
				$JobsTreeCache.close();
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	// Attention, catch Throwable and not throw it.
	public void shutdown() {
		removeIpNode();
		close$JobsTreeCache();
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

	public TreeCache get$JobsTreeCache() {
		return $JobsTreeCache;
	}

	public void set$JobsTreeCache(TreeCache $JobsTreeCache) {
		this.$JobsTreeCache = $JobsTreeCache;
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

}
