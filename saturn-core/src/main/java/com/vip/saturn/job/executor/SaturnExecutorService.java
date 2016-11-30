package com.vip.saturn.job.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

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
	
	private String executorName;
	
	private List<String> jobNames = new ArrayList<String>();

	private CoordinatorRegistryCenter coordinatorRegistryCenter;

	private TreeCache treeCache;

	private String ipNode;

	private ClassLoader jobClassLoader;

	private ClassLoader executorClassLoader;

	public static SaturnExecutorService init(String namespace, CoordinatorRegistryCenter coordinatorRegistryCenter,
			String executorName) {
		return new SaturnExecutorService(coordinatorRegistryCenter, executorName);
	}

	/**
	 * 实例化之前，请确保执行了SaturnExecutorService.initExecutorName()方法
	 * @param coordinatorRegistryCenter zk registry
	 */
	private SaturnExecutorService(CoordinatorRegistryCenter coordinatorRegistryCenter, String executorName) {
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

	public void addNewJobListenerCallback(final ScheduleNewJobCallback callback) throws Exception {
		treeCache = new TreeCache((CuratorFramework) coordinatorRegistryCenter.getRawClient(),
				"/" + JobNodePath.$JOBS_NODE_NAME);
		treeCache.start();
		treeCache.getListenable().addListener(new TreeCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
				if (event != null) {
					Type type = event.getType();
					ChildData data = event.getData();
					if (type != null && data != null) {
						String path = data.getPath();
						if (path != null) {
							if (type.equals(Type.NODE_ADDED) && isJobNameNodePath(path)) { // 增加作业
								String newJob = getJobNameByPath(path);
								if (!jobNames.contains(newJob)) {
									jobNames.add(newJob);
									callback.call(newJob);
								}
							} else if (type.equals(Type.NODE_REMOVED) && isJobNameNodePath(path)) { // 删除作业
								String deleteJob = getJobNameByPath(path);
								if (jobNames.contains(deleteJob)) {
									jobNames.remove(deleteJob);
								}
							}
						}
					}
				}
			}

			private boolean isJobNameNodePath(String path) {
				String regex = "/" + "\\" + JobNodePath.$JOBS_NODE_NAME + "/" + "[^/]*/" + ConfigurationNode.JOB_CLASS;
				return path.matches(regex);
			}

			private String getJobNameByPath(String path) {
				String temp = "/" + JobNodePath.$JOBS_NODE_NAME + "/";
				String substring = path.substring(temp.length());
				return substring.split("/")[0];
			}
		});

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

	public TreeCache getTreeCache() {
		return treeCache;
	}

	public void setTreeCache(TreeCache treeCache) {
		this.treeCache = treeCache;
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
