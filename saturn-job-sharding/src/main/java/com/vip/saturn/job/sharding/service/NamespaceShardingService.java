package com.vip.saturn.job.sharding.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.vip.saturn.job.integrate.service.ReportAlarmService;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;

/**
 *
 * @author hebelala
 */
public class NamespaceShardingService {
	private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceShardingService.class);

	private static final int LOAD_LEVEL_DEFAULT = 1;

	private static final String NAME_IS_CONTAINER_ALIGN_WITH_PHYSICAL = "VIP_SATURN_CONTAINER_ALIGN_WITH_PHYSICAL";

	private static boolean isContainerAlignWithPhysical;

	private String namespace;

	private String hostValue;

	private CuratorFramework curatorFramework;

	private AtomicInteger shardingCount;

	private AtomicBoolean needAllSharding;

	private ExecutorService executorService;

	private NamespaceShardingContentService namespaceShardingContentService;

	private ReportAlarmService reportAlarmService;

	private ReentrantLock lock;

	static {
		String isContainerAlignWithPhysicalStr = System.getProperty(NAME_IS_CONTAINER_ALIGN_WITH_PHYSICAL,
				System.getenv(NAME_IS_CONTAINER_ALIGN_WITH_PHYSICAL));

		isContainerAlignWithPhysical = StringUtils.isBlank(isContainerAlignWithPhysicalStr)
				|| Boolean.parseBoolean(isContainerAlignWithPhysicalStr);
	}

	public NamespaceShardingService(CuratorFramework curatorFramework, String hostValue,
									ReportAlarmService reportAlarmService) {
		this.curatorFramework = curatorFramework;
		this.hostValue = hostValue;
		this.reportAlarmService = reportAlarmService;
		this.shardingCount = new AtomicInteger(0);
		this.needAllSharding = new AtomicBoolean(false);
		this.executorService = newSingleThreadExecutor();
		this.namespace = curatorFramework.getNamespace();
		this.namespaceShardingContentService = new NamespaceShardingContentService(curatorFramework);
		this.lock = new ReentrantLock();
	}

	private ExecutorService newSingleThreadExecutor() {
		return Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, namespace + "-" + r.getClass().getSimpleName());
			}
		});
	}

	private abstract class AbstractAsyncShardingTask implements Runnable {

		protected abstract void logStartInfo();

		/**
		 * Special enable jobs that need to be notified prior, not consider whether whose shards are changed.<br/>
		 * By default, notify enable jobs whose shards are changed.
		 */
		protected List<String> notifyEnableJobsPrior() {
			return null;
		}

		@Override
		public void run() {
			logStartInfo();
			boolean isAllShardingTask = this instanceof ExecuteAllShardingTask;
			try {
				// 如果当前变为非leader，则直接返回
				if (!isLeadershipOnly()) {
					return;
				}

				// 如果需要全量分片，且当前线程不是全量分片线程，则直接返回，没必要做分片
				if (needAllSharding.get() && !isAllShardingTask) {
					LOGGER.info("the {} will be ignored, because there will be {}", this.getClass().getSimpleName(),
							ExecuteAllShardingTask.class.getSimpleName());
					return;
				}

				List<String> allJobs = getAllJobs();
				List<String> allEnableJobs = getAllEnableJobs(allJobs);
				List<Executor> oldOnlineExecutorList = getLastOnlineExecutorList();
				List<Executor> customLastOnlineExecutorList = customLastOnlineExecutorList();
				List<Executor> lastOnlineExecutorList = customLastOnlineExecutorList == null
						? copyOnlineExecutorList(oldOnlineExecutorList) : customLastOnlineExecutorList;
				List<Executor> lastOnlineTrafficExecutorList = getTrafficExecutorList(lastOnlineExecutorList);
				List<Shard> shardList = new ArrayList<>();
				// 摘取
				if (pick(allJobs, allEnableJobs, shardList, lastOnlineExecutorList, lastOnlineTrafficExecutorList)) {
					// 放回
					putBackBalancing(allEnableJobs, shardList, lastOnlineExecutorList, lastOnlineTrafficExecutorList);
					// 如果当前变为非leader，则返回
					if (!isLeadershipOnly()) {
						return;
					}
					// 持久化分片结果
					if (shardingContentIsChanged(oldOnlineExecutorList, lastOnlineExecutorList)) {
						namespaceShardingContentService.persistDirectly(lastOnlineExecutorList);
					}
					// notify the shards-changed jobs of all enable jobs.
					Map<String, Map<String, List<Integer>>> enabledAndShardsChangedJobShardContent = getEnabledAndShardsChangedJobShardContent(
							isAllShardingTask, allEnableJobs, oldOnlineExecutorList, lastOnlineExecutorList);
					namespaceShardingContentService
							.persistJobsNecessaryInTransaction(enabledAndShardsChangedJobShardContent);
					// sharding count ++
					increaseShardingCount();
				}
			} catch (InterruptedException e) {
				LOGGER.info("{}-{} {} is interrupted", namespace, hostValue, this.getClass().getSimpleName());
				Thread.currentThread().interrupt();
			} catch (Throwable t) {
				LOGGER.error(t.getMessage(), t);
				if (!isAllShardingTask) { // 如果当前不是全量分片，则需要全量分片来拯救异常
					needAllSharding.set(true);
					shardingCount.incrementAndGet();
					executorService.submit(new ExecuteAllShardingTask());
				} else { // 如果当前是全量分片，则告警并关闭当前服务，重选leader来做事情
					if (reportAlarmService != null) {
						try {
							reportAlarmService.allShardingError(namespace, hostValue);
						} catch (Throwable t2) {
							if (t2 instanceof InterruptedException) { // NOSONAR
								LOGGER.info("{}-{} {}-allShardingError is interrupted", namespace, hostValue,
										this.getClass().getSimpleName());
								Thread.currentThread().interrupt();
							} else {
								LOGGER.error(t2.getMessage(), t2);
							}
						}
					}
					try {
						shutdownInner(false);
					} catch (InterruptedException e) {
						LOGGER.info("{}-{} {}-shutdownInner is interrupted", namespace, hostValue,
								this.getClass().getSimpleName());
						Thread.currentThread().interrupt();
					} catch (Throwable t3) {
						LOGGER.error(t3.getMessage(), t3);
					}
				}
			} finally {
				if (isAllShardingTask) { // 如果是全量分片，不再进行全量分片
					needAllSharding.set(false);
				}
				shardingCount.decrementAndGet();
			}
		}

		private boolean shardingContentIsChanged(List<Executor> oldOnlineExecutorList,
				List<Executor> lastOnlineExecutorList) {
			return !namespaceShardingContentService.toShardingContent(oldOnlineExecutorList)
					.equals(namespaceShardingContentService.toShardingContent(lastOnlineExecutorList));
		}

		private List<Executor> copyOnlineExecutorList(List<Executor> oldOnlineExecutorList) {
			List<Executor> newOnlineExecutorList = new ArrayList<>();
			for (Executor oldExecutor : oldOnlineExecutorList) {
				Executor newExecutor = new Executor();
				newExecutor.setTotalLoadLevel(oldExecutor.getTotalLoadLevel());
				newExecutor.setIp(oldExecutor.getIp());
				newExecutor.setNoTraffic(oldExecutor.isNoTraffic());
				newExecutor.setExecutorName(oldExecutor.getExecutorName());
				if (oldExecutor.getJobNameList() != null) {
					newExecutor.setJobNameList(new ArrayList<String>());
					for (String jobName : oldExecutor.getJobNameList()) {
						newExecutor.getJobNameList().add(jobName);
					}
				}
				if (oldExecutor.getShardList() != null) {
					newExecutor.setShardList(new ArrayList<Shard>());
					for (Shard oldShard : oldExecutor.getShardList()) {
						Shard newShard = new Shard();
						newShard.setItem(oldShard.getItem());
						newShard.setJobName(oldShard.getJobName());
						newShard.setLoadLevel(oldShard.getLoadLevel());
						newExecutor.getShardList().add(newShard);
					}
				}
				newOnlineExecutorList.add(newExecutor);
			}
			return newOnlineExecutorList;
		}

		/**
		 * 修正lastOnlineExecutorList中的jobNameList
		 */
		protected boolean fixJobNameList(List<Executor> lastOnlineExecutorList, String jobName) throws Exception {
			boolean fixed = false;
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				if (executor.getJobNameList() == null) {
					executor.setJobNameList(new ArrayList<String>());
				}
				List<String> jobNameList = executor.getJobNameList();
				String jobServersExecutorStatusNodePath = SaturnExecutorsNode
						.getJobServersExecutorStatusNodePath(jobName, executor.getExecutorName());
				if (curatorFramework.checkExists().forPath(jobServersExecutorStatusNodePath) != null) {
					if (!jobNameList.contains(jobName)) {
						jobNameList.add(jobName);
						fixed = true;
					}
				} else {
					if (jobNameList.contains(jobName)) {
						jobNameList.remove(jobName);
						fixed = true;
					}
				}
			}
			return fixed;
		}

		private void increaseShardingCount() throws Exception {
			Integer _shardingCount = 1;
			if (null != curatorFramework.checkExists().forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH)) {
				byte[] shardingCountData = curatorFramework.getData().forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH);
				if (shardingCountData != null) {
					try {
						_shardingCount = Integer.parseInt(new String(shardingCountData, "UTF-8")) + 1;
					} catch (NumberFormatException e) {
						LOGGER.error("parse shardingCount error", e);
					}
				}
				curatorFramework.setData().forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH,
						_shardingCount.toString().getBytes("UTF-8"));
			} else {
				curatorFramework.create().creatingParentsIfNeeded().forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH,
						_shardingCount.toString().getBytes("UTF-8"));
			}
		}

		/**
		 * Get the jobs, that are enabled, and whose shards are changed. Specially, return all enabled jobs when the
		 * current thread is all-shard-task<br/>
		 * Return the jobs and their shardContent.
		 */
		private Map<String, Map<String, List<Integer>>> getEnabledAndShardsChangedJobShardContent(
				boolean isAllShardingTask, List<String> allEnableJobs, List<Executor> oldOnlineExecutorList,
				List<Executor> lastOnlineExecutorList) throws Exception {
			Map<String, Map<String, List<Integer>>> jobShardContent = new HashMap<>();
			if (isAllShardingTask) {
				for (String enableJob : allEnableJobs) {
					Map<String, List<Integer>> lastShardingItems = namespaceShardingContentService
							.getShardingItems(lastOnlineExecutorList, enableJob);
					jobShardContent.put(enableJob, lastShardingItems);
				}
				return jobShardContent;
			}
			List<String> enableJobsPrior = notifyEnableJobsPrior();
			for (String enableJob : allEnableJobs) {
				Map<String, List<Integer>> lastShardingItems = namespaceShardingContentService
						.getShardingItems(lastOnlineExecutorList, enableJob);
				// notify prior jobs that are in all enable jobs
				if (enableJobsPrior != null && enableJobsPrior.contains(enableJob)) {
					jobShardContent.put(enableJob, lastShardingItems);
					continue;
				}
				Map<String, List<Integer>> oldShardingItems = namespaceShardingContentService
						.getShardingItems(oldOnlineExecutorList, enableJob);
				// just compare whether or not contains the same executorName, and it's shardList
				boolean isChanged = false;
				Iterator<Map.Entry<String, List<Integer>>> oldIterator = oldShardingItems.entrySet().iterator();
				wl_loop: while (oldIterator.hasNext()) {
					Map.Entry<String, List<Integer>> next = oldIterator.next();
					String executorName = next.getKey();
					if (!lastShardingItems.containsKey(executorName)) {
						isChanged = true;
						break;
					}
					List<Integer> shards = next.getValue();
					List<Integer> newShard = lastShardingItems.get(executorName);
					if (shards == null && newShard != null || shards != null && newShard == null) {
						isChanged = true;
						break;
					}
					if (shards != null && newShard != null) {
						for (Integer shard : shards) {
							if (!newShard.contains(shard)) {
								isChanged = true;
								break wl_loop;
							}
						}
					}
				}
				if (!isChanged) {
					Iterator<Map.Entry<String, List<Integer>>> newIterator = lastShardingItems.entrySet().iterator();
					wl_loop2: while (newIterator.hasNext()) {
						Map.Entry<String, List<Integer>> next = newIterator.next();
						String executorName = next.getKey();
						if (!oldShardingItems.containsKey(executorName)) {
							isChanged = true;
							break;
						}
						List<Integer> shards = next.getValue();
						List<Integer> oldShard = oldShardingItems.get(executorName);
						if (shards == null && oldShard != null || shards != null && oldShard == null) {
							isChanged = true;
							break;
						}
						if (shards != null && oldShard != null) {
							for (Integer shard : shards) {
								if (!oldShard.contains(shard)) {
									isChanged = true;
									break wl_loop2;
								}
							}
						}
					}
				}
				if (isChanged) {
					jobShardContent.put(enableJob, lastShardingItems);
				}
			}
			return jobShardContent;
		}

		protected boolean isLocalMode(String jobName) throws Exception {
			String localNodePath = SaturnExecutorsNode.getJobConfigLocalModeNodePath(jobName);
			if (curatorFramework.checkExists().forPath(localNodePath) != null) {
				byte[] data = curatorFramework.getData().forPath(localNodePath);
				if (data != null) {
					return Boolean.valueOf(new String(data, "UTF-8"));
				}
			}
			return false;
		}

		protected int getShardingTotalCount(String jobName) throws Exception {
			int shardingTotalCount = 0;
			String jobConfigShardingTotalCountNodePath = SaturnExecutorsNode
					.getJobConfigShardingTotalCountNodePath(jobName);
			if (curatorFramework.checkExists().forPath(jobConfigShardingTotalCountNodePath) != null) {
				byte[] shardingTotalCountData = curatorFramework.getData().forPath(jobConfigShardingTotalCountNodePath);
				if (shardingTotalCountData != null) {
					try {
						shardingTotalCount = Integer.parseInt(new String(shardingTotalCountData, "UTF-8"));
					} catch (NumberFormatException e) {
						LOGGER.error("parse shardingTotalCount error, will use the default value", e);
					}
				}
			}
			return shardingTotalCount;
		}

		protected int getLoadLevel(String jobName) throws Exception {
			int loadLevel = LOAD_LEVEL_DEFAULT;
			String jobConfigLoadLevelNodePath = SaturnExecutorsNode.getJobConfigLoadLevelNodePath(jobName);
			if (curatorFramework.checkExists().forPath(jobConfigLoadLevelNodePath) != null) {
				byte[] loadLevelData = curatorFramework.getData().forPath(jobConfigLoadLevelNodePath);
				try {
					if (loadLevelData != null) {
						loadLevel = Integer.parseInt(new String(loadLevelData, "UTF-8"));
					}
				} catch (NumberFormatException e) {
					LOGGER.error("parse loadLevel error, will use the default value", e);
				}
			}
			return loadLevel;
		}

		/**
		 * 获取Executor集合，默认从sharding/content获取
		 */
		private List<Executor> getLastOnlineExecutorList() throws Exception {
			return namespaceShardingContentService.getExecutorList();
		}

		/**
		 * Custom the lastOnlineExecutorList, attention, cannot return null
		 */
		protected List<Executor> customLastOnlineExecutorList() throws Exception {
			return null;
		}
		
		private List<Executor> getTrafficExecutorList(List<Executor> executorList) {
			List<Executor> trafficExecutorList = new ArrayList<>();
			for (Executor executor : executorList) {
				if (!executor.isNoTraffic()) {
					trafficExecutorList.add(executor);
				}
			}
			return trafficExecutorList;
		}

		/**
		 * 摘取
		 * @param allJobs 该域下所有作业
		 * @param allEnableJobs 该域下所有启用的作业
		 * @param shardList 默认为空集合
		 * @param lastOnlineExecutorList 默认为当前存储的数据，如果不想使用存储数据，请重写{@link #customLastOnlineExecutorList()}}方法
		 * @param lastOnlineTrafficExecutorList lastOnlineExecutorList中所有noTraffic为false的Executor，注意Executor是同一个对象
		 * @return true摘取成功；false摘取失败，不需要继续下面的逻辑
		 */
		protected abstract boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception;

		/**
		 * 按照loadLevel降序排序，如果loadLevel相同，按照作业名降序排序
		 */
		protected void sortShardList(List<Shard> shardList) {
			Collections.sort(shardList, new Comparator<Shard>() {
				@Override
				public int compare(Shard o1, Shard o2) {
					int loadLevelSub = o2.getLoadLevel() - o1.getLoadLevel();
					return loadLevelSub == 0 ? o2.getJobName().compareTo(o1.getJobName()) : loadLevelSub;
				}
			});
		}

		private List<Executor> getNotDockerExecutors(List<Executor> lastOnlineExecutorList) throws Exception {
			// if isContainerAlignWithPhysical = false, return all executors; otherwise, return all non-container executors.
			if (isContainerAlignWithPhysical) {
				return lastOnlineExecutorList;
			}

			List<Executor> nonDockerExecutors = new ArrayList<>();
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				String executorName = executor.getExecutorName();
				if (curatorFramework.checkExists()
						.forPath(SaturnExecutorsNode.getExecutorTaskNodePath(executorName)) == null) {
					nonDockerExecutors.add(executor);
				}
			}
			return nonDockerExecutors;
		}

		protected void putBackBalancing(List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			if (lastOnlineExecutorList.isEmpty()) {
				LOGGER.warn("Unnecessary to put shards back to executors balanced because of no executor");
				return;
			}

			sortShardList(shardList);

			// 获取所有非容器的executors
			List<Executor> notDockerExecutors = getNotDockerExecutors(lastOnlineTrafficExecutorList);

			// 获取shardList中的作业能够被接管的executors
			Map<String, List<Executor>> noDockerTrafficExecutorsMapByJob = new HashMap<>();
			Map<String, List<Executor>> lastOnlineTrafficExecutorListMapByJob = new HashMap<>();
			// 是否为本地模式作业的映射
			Map<String, Boolean> localModeMap = new HashMap<>();
			// 是否配置优先节点的作业的映射
			Map<String, Boolean> preferListIsConfiguredMap = new HashMap<>();
			// 优先节点的作业的映射
			Map<String, List<String>> preferListConfiguredMap = new HashMap<>();
			// 是否使用非优先节点的作业的映射
			Map<String, Boolean> useDispreferListMap = new HashMap<>();
			{
				Iterator<Shard> iterator = shardList.iterator();
				while (iterator.hasNext()) {
					String jobName = iterator.next().getJobName();
					if (!noDockerTrafficExecutorsMapByJob.containsKey(jobName)) {
						noDockerTrafficExecutorsMapByJob.put(jobName, filterExecutorsByJob(notDockerExecutors, jobName));
					}
					if (!lastOnlineTrafficExecutorListMapByJob.containsKey(jobName)) {
						lastOnlineTrafficExecutorListMapByJob.put(jobName, filterExecutorsByJob(lastOnlineTrafficExecutorList, jobName));
					}
					if (!localModeMap.containsKey(jobName)) {
						localModeMap.put(jobName, isLocalMode(jobName));
					}
					if (!preferListIsConfiguredMap.containsKey(jobName)) {
						preferListIsConfiguredMap.put(jobName, preferListIsConfigured(jobName));
					}
					if (!preferListConfiguredMap.containsKey(jobName)) {
						preferListConfiguredMap.put(jobName, getPreferListConfigured(jobName));
					}
					if (!useDispreferListMap.containsKey(jobName)) {
						useDispreferListMap.put(jobName, useDispreferList(jobName));
					}
				}
			}

			// 整体算法放回算法：拿取Shard，放进负荷最小的executor

			// 1、放回localMode的Shard
			// 如果配置了preferList，则选取preferList中的executor。
			// 如果preferList中的executor都挂了，则不转移；否则，选取没有接管该作业的executor列表的loadLevel最小的一个。
			// 如果没有配置preferList，则选取没有接管该作业的executor列表的loadLevel最小的一个。
			{
				Iterator<Shard> iterator = shardList.iterator();
				while (iterator.hasNext()) {
					Shard shard = iterator.next();
					String jobName = shard.getJobName();
					if (localModeMap.get(jobName)) {
						if (preferListIsConfiguredMap.get(jobName)) {
							List<String> preferListConfigured = preferListConfiguredMap.get(jobName);
							if (!preferListConfigured.isEmpty()) {
								List<Executor> preferExecutorList = new ArrayList<>();
								List<Executor> lastOnlineTrafficExecutorListByJob = lastOnlineTrafficExecutorListMapByJob.get(jobName);
								for (int i = 0; i < lastOnlineTrafficExecutorListByJob.size(); i++) {
									Executor executor = lastOnlineTrafficExecutorListByJob.get(i);
									if (preferListConfigured.contains(executor.getExecutorName())) {
										preferExecutorList.add(executor);
									}
								}
								if (!preferExecutorList.isEmpty()) {
									Executor executor = getExecutorWithMinLoadLevelAndNoThisJob(preferExecutorList,
											jobName);
									putShardIntoExecutor(shard, executor);
								}
							}
						} else {
							Executor executor = getExecutorWithMinLoadLevelAndNoThisJob(noDockerTrafficExecutorsMapByJob.get(jobName),
									jobName);
							putShardIntoExecutor(shard, executor);
						}
						iterator.remove();
					}
				}
			}

			// 2、放回配置了preferList的Shard
			{
				Iterator<Shard> iterator = shardList.iterator();
				while (iterator.hasNext()) {
					Shard shard = iterator.next();
					String jobName = shard.getJobName();
					if (preferListIsConfiguredMap.get(jobName)) { // fix,
																	// preferList为空不能作为判断是否配置preferList的依据，比如说配置了容器资源，但是全部下线了。
						List<String> preferList = preferListConfiguredMap.get(jobName);
						List<Executor> preferExecutorList = new ArrayList<>();
						List<Executor> lastOnlineTrafficExecutorListByJob = lastOnlineTrafficExecutorListMapByJob.get(jobName);
						for (int i = 0; i < lastOnlineTrafficExecutorListByJob.size(); i++) {
							Executor executor = lastOnlineTrafficExecutorListByJob.get(i);
							if (preferList.contains(executor.getExecutorName())) {
								preferExecutorList.add(executor);
							}
						}
						// 如果preferList的Executor都offline，则放回到全部online的Executor中某一个。如果是这种情况，则后续再操作，避免不均衡的情况
						// 如果存在preferExecutor，择优放回
						if (!preferExecutorList.isEmpty()) {
							Executor executor = getExecutorWithMinLoadLevel(preferExecutorList);
							putShardIntoExecutor(shard, executor);
							iterator.remove();
						} else { // 如果不存在preferExecutor
							// 如果“只使用preferExecutor”，则丢弃；否则，等到后续（在第3步）进行放回操作，避免不均衡的情况
							if (!useDispreferListMap.get(jobName)) {
								iterator.remove();
							}
						}
					}
				}
			}

			// 3、放回没有配置preferList的Shard
			{
				Iterator<Shard> iterator = shardList.iterator();
				while (iterator.hasNext()) {
					Shard shard = iterator.next();
					Executor executor = getExecutorWithMinLoadLevel(noDockerTrafficExecutorsMapByJob.get(shard.getJobName()));
					putShardIntoExecutor(shard, executor);
					iterator.remove();
				}
			}
		}

		/**
		 * 是否使用非preferList<br>
		 * 1、存在结点，并且该结点值为false，返回false；<br>
		 * 2、其他情况，返回true
		 */
		protected boolean useDispreferList(String jobName) throws Exception {
			String jobConfigUseDispreferListNodePath = SaturnExecutorsNode
					.getJobConfigUseDispreferListNodePath(jobName);
			if (curatorFramework.checkExists().forPath(jobConfigUseDispreferListNodePath) != null) {
				byte[] useDispreferListData = curatorFramework.getData().forPath(jobConfigUseDispreferListNodePath);
				if (useDispreferListData != null && !Boolean.valueOf(new String(useDispreferListData, "UTF-8"))) {
					return false;
				}
			}
			return true;
		}

		private Executor getExecutorWithMinLoadLevel(List<Executor> executorList) {
			Executor minLoadLevelExecutor = null;
			for (int i = 0; i < executorList.size(); i++) {
				Executor executor = executorList.get(i);
				if (minLoadLevelExecutor == null
						|| minLoadLevelExecutor.getTotalLoadLevel() > executor.getTotalLoadLevel()) {
					minLoadLevelExecutor = executor;
				}
			}
			return minLoadLevelExecutor;
		}

		private Executor getExecutorWithMinLoadLevelAndNoThisJob(List<Executor> executorList, String jobName) {
			Executor minLoadLevelExecutor = null;
			for (int i = 0; i < executorList.size(); i++) {
				Executor executor = executorList.get(i);
				List<Shard> shardList = executor.getShardList();
				boolean containThisJob = false;
				for (int j = 0; j < shardList.size(); j++) {
					Shard shard = shardList.get(j);
					if (shard.getJobName().equals(jobName)) {
						containThisJob = true;
						break;
					}
				}
				if (!containThisJob && (minLoadLevelExecutor == null
						|| minLoadLevelExecutor.getTotalLoadLevel() > executor.getTotalLoadLevel())) {
					minLoadLevelExecutor = executor;
				}
			}
			return minLoadLevelExecutor;
		}

		private void putShardIntoExecutor(Shard shard, Executor executor) {
			if (executor != null) {
				if (isIn(shard, executor.getShardList())) {
					LOGGER.error("The shard({}-{}) is running in the executor of {}, cannot be put again",
							shard.getJobName(), shard.getItem(), executor.getExecutorName());
				} else {
					executor.getShardList().add(shard);
					executor.setTotalLoadLevel(executor.getTotalLoadLevel() + shard.getLoadLevel());
				}
			} else {
				LOGGER.info("No executor to take over the shard: {}-{}", shard.getJobName(), shard.getItem());
			}
		}

		/**
		 * 获取该域下的所有作业
		 */
		private List<String> getAllJobs() throws Exception {
			List<String> allJob = new ArrayList<>();
			if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.$JOBSNODE_PATH) == null) {
				curatorFramework.create().creatingParentsIfNeeded().forPath(SaturnExecutorsNode.$JOBSNODE_PATH);
			}
			List<String> tmp = curatorFramework.getChildren().forPath(SaturnExecutorsNode.$JOBSNODE_PATH);
			if (tmp != null) {
				allJob.addAll(tmp);
			}
			return allJob;
		}

		/**
		 * 获取该域下的所有enable的作业
		 */
		protected List<String> getAllEnableJobs(List<String> allJob) throws Exception {
			List<String> allEnableJob = new ArrayList<>();
			for (int i = 0; i < allJob.size(); i++) {
				String job = allJob.get(i);
				if (curatorFramework.checkExists()
						.forPath(SaturnExecutorsNode.getJobConfigEnableNodePath(job)) != null) {
					byte[] enableData = curatorFramework.getData()
							.forPath(SaturnExecutorsNode.getJobConfigEnableNodePath(job));
					if (enableData != null && Boolean.valueOf(new String(enableData, "UTF-8"))) {
						allEnableJob.add(job);
					}
				}
			}
			return allEnableJob;
		}

		protected boolean isIn(Shard shard, List<Shard> shardList) {
			for (int i = 0; i < shardList.size(); i++) {
				Shard tmp = shardList.get(i);
				if (tmp.getJobName().equals(shard.getJobName()) && tmp.getItem() == shard.getItem()) {
					return true;
				}
			}
			return false;
		}

		protected boolean preferListIsConfigured(String jobName) throws Exception {
			if (curatorFramework.checkExists()
					.forPath(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName)) != null) {
				byte[] preferListData = curatorFramework.getData()
						.forPath(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName));
				if (preferListData != null) {
					return new String(preferListData, "UTF-8").trim().length() > 0;
				}
			}
			return false;
		}

		/**
		 * 获取配置态的preferList，即使配置的executor不存在，也会返回。 特别的是，对于docker task，如果存在，才去解析出executor列表。
		 */
		protected List<String> getPreferListConfigured(String jobName) throws Exception {
			List<String> preferList = new ArrayList<>();
			if (curatorFramework.checkExists()
					.forPath(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName)) != null) {
				byte[] preferListData = curatorFramework.getData()
						.forPath(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName));
				if (preferListData != null) {
					List<String> allExistsExecutors = getAllExistingExecutors();
					String[] split = new String(preferListData, "UTF-8").split(",");
					for (String tmp : split) {
						String tmpTrim = tmp.trim();
						if (!"".equals(tmpTrim)) {
							fillRealPreferListIfIsDockerOrNot(preferList, tmpTrim, allExistsExecutors);
						}
					}
				}
			}
			return preferList;
		}

		private List<String> getAllExistingExecutors() throws Exception {
			List<String> allExistsExecutors = new ArrayList<>();
			if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorsNodePath()) != null) {
				List<String> executors = curatorFramework.getChildren()
						.forPath(SaturnExecutorsNode.getExecutorsNodePath());
				if (executors != null) {
					allExistsExecutors.addAll(executors);
				}
			}
			return allExistsExecutors;
		}

		/**
		 * 如果prefer不是docker容器，并且preferList不包含，则直接添加；<br>
		 * 如果prefer是docker容器（以@开头），则prefer为task，获取该task下的所有executor，如果不包含，添加进preferList。
		 */
		private void fillRealPreferListIfIsDockerOrNot(List<String> preferList, String prefer,
				List<String> allExistsExecutors) throws Exception {
			if (!prefer.startsWith("@")) { // not docker server
				if (!preferList.contains(prefer)) {
					preferList.add(prefer);
				}
			} else { // docker server, get the real executorList by task
				String task = prefer.substring(1);
				for (int i = 0; i < allExistsExecutors.size(); i++) {
					String executor = allExistsExecutors.get(i);
					if (curatorFramework.checkExists()
							.forPath(SaturnExecutorsNode.getExecutorTaskNodePath(executor)) != null) {
						byte[] taskData = curatorFramework.getData()
								.forPath(SaturnExecutorsNode.getExecutorTaskNodePath(executor));
						if (taskData != null && task.equals(new String(taskData, "UTF-8"))) {
							if (!preferList.contains(executor)) {
								preferList.add(executor);
							}
						}
					}
				}
			}
		}

		protected List<Executor> filterExecutorsByJob(List<Executor> executorList, String jobName) throws Exception {
			List<Executor> executorListByJob = new ArrayList<>();
			for (int i = 0; i < executorList.size(); i++) {
				Executor executor = executorList.get(i);
				List<String> jobNameList = executor.getJobNameList();
				if (jobNameList != null && jobNameList.contains(jobName)) {
					executorListByJob.add(executor);
				}
			}
			return executorListByJob;
		}

		private List<Executor> getPreferListOnlineByJob(String jobName, List<String> preferListConfigured,
				List<Executor> lastOnlineExecutorList) {
			List<Executor> preferListOnlineByJob = new ArrayList<>();
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				if (preferListConfigured.contains(executor.getExecutorName())
						&& executor.getJobNameList().contains(jobName)) {
					preferListOnlineByJob.add(executor);
				}
			}
			return preferListOnlineByJob;
		}

		private List<Shard> createShards(String jobName, int number, int loadLevel) {
			List<Shard> shards = new ArrayList<>();
			for (int i = 0; i < number; i++) {
				Shard shard = new Shard();
				shard.setJobName(jobName);
				shard.setItem(i);
				shard.setLoadLevel(loadLevel);
				shards.add(shard);
			}
			return shards;
		}

		protected List<Shard> createShards(String jobName, List<Executor> lastOnlineExecutorList) throws Exception {
			List<Shard> shardList = new ArrayList<>();
			boolean preferListIsConfigured = preferListIsConfigured(jobName);
			List<String> preferListConfigured = getPreferListConfigured(jobName);
			List<Executor> preferListOnlineByJob = getPreferListOnlineByJob(jobName, preferListConfigured,
					lastOnlineExecutorList);
			boolean localMode = isLocalMode(jobName);
			int shardingTotalCount = getShardingTotalCount(jobName);
			int loadLevel = getLoadLevel(jobName);

			if (localMode) {
				if (preferListIsConfigured) {
					// 如果当前存在优先节点在线，则新建在线的优先节点的数量的分片
					if (!preferListOnlineByJob.isEmpty()) {
						shardList.addAll(createShards(jobName, preferListOnlineByJob.size(), loadLevel));
					}
				} else {
					// 新建在线的executor的数量的分片
					shardList.addAll(createShards(jobName, lastOnlineExecutorList.size(), loadLevel));
				}
			} else {
				// 新建shardingTotalCount数量的分片
				shardList.addAll(createShards(jobName, shardingTotalCount, loadLevel));
			}
			return shardList;
		}
		
		protected boolean getExecutorNoTraffic(String executorName) throws Exception {
			return curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorNoTrafficNodePath(executorName)) != null;
		}

	}

	/**
	 * 域下重排，移除已经存在所有executor，重新获取executors，重新获取作业shards
	 */
	private class ExecuteAllShardingTask extends AbstractAsyncShardingTask {

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {} ", this.getClass().getSimpleName());
		}

		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJob, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			// 修正所有executor对所有作业的jobNameList
			for (int j = 0; j < allJobs.size(); j++) {
				fixJobNameList(lastOnlineExecutorList, allJobs.get(j));
			}

			// 获取该域下所有enable作业的所有分片
			for (int i = 0; i < allEnableJob.size(); i++) {
				String jobName = allEnableJob.get(i);
				shardList.addAll(createShards(jobName, lastOnlineTrafficExecutorList));
			}

			return true;
		}

		@Override
		protected List<Executor> customLastOnlineExecutorList() throws Exception {
			// 从$SaturnExecutors节点下，获取所有正在运行的Executor
			List<Executor> lastOnlineExecutorList = new ArrayList<>();
			if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorsNodePath()) != null) {
				List<String> zkExecutors = curatorFramework.getChildren()
						.forPath(SaturnExecutorsNode.getExecutorsNodePath());
				if (zkExecutors != null) {
					for (int i = 0; i < zkExecutors.size(); i++) {
						String zkExecutor = zkExecutors.get(i);
						if (curatorFramework.checkExists()
								.forPath(SaturnExecutorsNode.getExecutorIpNodePath(zkExecutor)) != null) {
							byte[] ipData = curatorFramework.getData()
									.forPath(SaturnExecutorsNode.getExecutorIpNodePath(zkExecutor));
							if (ipData != null) {
								Executor executor = new Executor();
								executor.setExecutorName(zkExecutor);
								executor.setIp(new String(ipData, "UTF-8"));
								executor.setNoTraffic(getExecutorNoTraffic(zkExecutor));
								executor.setShardList(new ArrayList<Shard>());
								executor.setJobNameList(new ArrayList<String>());
								lastOnlineExecutorList.add(executor);
							}
						}
					}
				}
			}
			return lastOnlineExecutorList;
		}

	}

	/**
	 * executor上线，仅仅添加executor空壳，如果其不存在；如果已经存在，重新设置下ip，防止ExecuteJobServerOnlineShardingTask先于执行而没设ip<br/>
	 * 特别的，如果当前没有executor，也就是这是第一台executor上线，则需要域全量分片，因为可能已经有作业处理启用状态了。
	 */
	private class ExecuteOnlineShardingTask extends AbstractAsyncShardingTask {

		private String executorName;
		private String ip;

		public ExecuteOnlineShardingTask(String executorName, String ip) {
			this.executorName = executorName;
			this.ip = ip;
		}

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {} with {} online", this.getClass().getSimpleName(), executorName);
		}

		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			// 如果没有Executor在运行，则需要进行全量分片
			if (lastOnlineExecutorList.isEmpty()) {
				LOGGER.warn("There are no running executors, need all sharding");
				needAllSharding.set(true);
				shardingCount.incrementAndGet();
				executorService.submit(new ExecuteAllShardingTask());
				return false;
			}

			Executor theExecutor = null;
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				Executor tmp = lastOnlineExecutorList.get(i);
				if (tmp.getExecutorName().equals(executorName)) {
					theExecutor = tmp;
					break;
				}
			}
			if (theExecutor == null) {
				theExecutor = new Executor();
				theExecutor.setExecutorName(executorName);
				theExecutor.setIp(ip);
				theExecutor.setNoTraffic(getExecutorNoTraffic(executorName));
				theExecutor.setShardList(new ArrayList<Shard>());
				theExecutor.setJobNameList(new ArrayList<String>());
				lastOnlineExecutorList.add(theExecutor);
				if(!theExecutor.isNoTraffic()) {
					lastOnlineTrafficExecutorList.add(theExecutor);
				}
			} else { // 重新设置下ip
				theExecutor.setIp(ip);
			}

			return true;
		}

	}

	/**
	 * executor下线，摘取该executor运行的所有非本地模式作业，移除该executor
	 */
	private class ExecuteOfflineShardingTask extends AbstractAsyncShardingTask {

		private String executorName;

		public ExecuteOfflineShardingTask(String executorName) {
			this.executorName = executorName;
		}

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {} with {} offline", this.getClass().getSimpleName(), executorName);
		}

		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			/**
			 * 摘取下线的executor全部Shard
			 */
			Executor theExecutor = null;
			{
				Iterator<Executor> iterator = lastOnlineExecutorList.iterator();
				while (iterator.hasNext()) {
					Executor executor = iterator.next();
					if (executor.getExecutorName().equals(executorName)) {
						theExecutor = executor;
						iterator.remove();
						shardList.addAll(executor.getShardList());
						break;
					}
				}
			}

			if (theExecutor != null) {
				Iterator<Executor> iterator = lastOnlineTrafficExecutorList.iterator();
				while (iterator.hasNext()) {
					Executor executor = iterator.next();
					if (theExecutor.equals(executor)) {
						iterator.remove();
						break;
					}
				}
			}
			
			// 如果该executor实际上已经在此之前下线，则摘取失败
			if (theExecutor == null) {
				return false;
			}

			// 移除本地模式的作业分片
			Iterator<Shard> iterator = shardList.iterator();
			while (iterator.hasNext()) {
				Shard shard = iterator.next();
				if (isLocalMode(shard.getJobName())) {
					iterator.remove();
				}
			}

			return true;
		}

	}
	
	/**
	 * 摘取executor流量，标记该executor的noTraffic为true，并移除其所有作业分片，只摘取所有非本地作业分片，设置totalLoadLevel为0
	 */
	private class ExecuteExtractTrafficShardingTask extends AbstractAsyncShardingTask {

		private String executorName;

		public ExecuteExtractTrafficShardingTask(String executorName) {
			this.executorName = executorName;
		}

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {} with {} extract traffic", this.getClass().getSimpleName(), executorName);
		}

		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
							   List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			// 摘取该executor的所有作业分片
			Executor theExecutor = null;
			{
				Iterator<Executor> iterator = lastOnlineTrafficExecutorList.iterator();
				while (iterator.hasNext()) {
					Executor executor = iterator.next();
					if (executor.getExecutorName().equals(executorName)) {
						shardList.addAll(executor.getShardList());
						executor.getShardList().clear();
						executor.setNoTraffic(true);
						executor.setTotalLoadLevel(0);
						theExecutor = executor;
						iterator.remove();
						break;
					}
				}
			}

			if (theExecutor == null) {
				LOGGER.warn("The executor {} maybe offline, unnecessary to extract traffic", executorName);
				return false;
			}

			// 移除本地模式的作业分片
			Iterator<Shard> iterator = shardList.iterator();
			while (iterator.hasNext()) {
				Shard shard = iterator.next();
				if (isLocalMode(shard.getJobName())) {
					iterator.remove();
				}
			}

			return true;
		}
	}

	/**
	 * 恢复executor流量，标记该executor的noTraffic为false，平衡摘取分片
	 */
	private class ExecuteRecoverTrafficShardingTask extends AbstractAsyncShardingTask {

		private String executorName;

		public ExecuteRecoverTrafficShardingTask(String executorName) {
			this.executorName = executorName;
		}

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {} with {} recover traffic", this.getClass().getSimpleName(), executorName);
		}

		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
							   List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			// 设置该executor的noTraffic为false
			Executor theExecutor = null;
			Iterator<Executor> iterator = lastOnlineExecutorList.iterator();
			while (iterator.hasNext()) {
				Executor executor = iterator.next();
				if (executor.getExecutorName().equals(executorName)) {
					executor.setNoTraffic(false);
					lastOnlineTrafficExecutorList.add(executor);
					theExecutor = executor;
					break;
				}
			}
			if (theExecutor == null) {
				LOGGER.warn("The executor {} maybe offline, unnecessary to recover traffic", executorName);
				return false;
			}

			// 平衡摘取每个作业能够运行的分片，可以视为jobNameList中每个作业的jobServerOnline
			final List<String> jobNameList = theExecutor.getJobNameList();
			for(String jobName : jobNameList) {
				new ExecuteJobServerOnlineShardingTask(jobName, executorName)
						.pickIntelligent(allEnableJobs, shardList, lastOnlineTrafficExecutorList);
			}

			return true;
		}
	}

	/**
	 * 作业启用，获取该作业的shards，注意要过滤不能运行该作业的executors
	 */
	private class ExecuteJobEnableShardingTask extends AbstractAsyncShardingTask {

		private String jobName;

		public ExecuteJobEnableShardingTask(String jobName) {
			this.jobName = jobName;
		}

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {} with {} enable", this.getClass().getSimpleName(), jobName);
		}

		@Override
		protected List<String> notifyEnableJobsPrior() {
			List<String> notifyEnableJobsPrior = new ArrayList<>();
			notifyEnableJobsPrior.add(jobName);
			return notifyEnableJobsPrior;
		}

		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			// 移除已经在Executor运行的该作业的所有Shard
			for (int i = 0; i < lastOnlineTrafficExecutorList.size(); i++) {
				Executor executor = lastOnlineTrafficExecutorList.get(i);
				Iterator<Shard> iterator = executor.getShardList().iterator();
				while (iterator.hasNext()) {
					Shard shard = iterator.next();
					if (jobName.equals(shard.getJobName())) {
						executor.setTotalLoadLevel(executor.getTotalLoadLevel() - shard.getLoadLevel());
						iterator.remove();
					}
				}
			}

			// 修正该所有executor的对该作业的jobNameList
			fixJobNameList(lastOnlineExecutorList, jobName);

			// 获取该作业的Shard
			shardList.addAll(createShards(jobName, lastOnlineTrafficExecutorList));

			return true;
		}

	}

	/**
	 * 作业禁用，摘取所有executor运行的该作业的shard，注意要相应地减loadLevel，不需要放回
	 */
	private class ExecuteJobDisableShardingTask extends AbstractAsyncShardingTask {

		private String jobName;

		public ExecuteJobDisableShardingTask(String jobName) {
			this.jobName = jobName;
		}

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {} with {} disable", this.getClass().getSimpleName(), jobName);
		}

		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) {
			// 摘取所有该作业的Shard
			for (int i = 0; i < lastOnlineTrafficExecutorList.size(); i++) {
				Executor executor = lastOnlineTrafficExecutorList.get(i);
				Iterator<Shard> iterator = executor.getShardList().iterator();
				while (iterator.hasNext()) {
					Shard shard = iterator.next();
					if (shard.getJobName().equals(jobName)) {
						executor.setTotalLoadLevel(executor.getTotalLoadLevel() - shard.getLoadLevel());
						iterator.remove();
						shardList.add(shard);
					}
				}
			}

			// 如果shardList为空，则没必要进行放回等操作，摘取失败
			if (shardList.isEmpty()) {
				return false;
			}

			return true;
		}

		@Override
		protected void putBackBalancing(List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) {
			// 不做操作
		}

	}

	/**
	 * 作业重排，移除所有executor的该作业shard，重新获取该作业的shards，finally删除forceShard结点
	 */
	private class ExecuteJobForceShardShardingTask extends AbstractAsyncShardingTask {

		private String jobName;

		public ExecuteJobForceShardShardingTask(String jobName) {
			this.jobName = jobName;
		}

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {} with {} forceShard", this.getClass().getSimpleName(), jobName);
		}

		@Override
		public void run() {
			try {
				super.run();
			} finally {
				deleteForceShardNode();
			}
		}

		private void deleteForceShardNode() {
			try {
				String jobConfigForceShardNodePath = SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName);
				if (curatorFramework.checkExists().forPath(jobConfigForceShardNodePath) != null) {
					curatorFramework.delete().forPath(jobConfigForceShardNodePath);
				}
			} catch (Throwable t) {
				LOGGER.error("delete forceShard node error", t);
			}
		}

		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			// 移除已经在Executor运行的该作业的所有Shard
			for (int i = 0; i < lastOnlineTrafficExecutorList.size(); i++) {
				Executor executor = lastOnlineTrafficExecutorList.get(i);
				Iterator<Shard> iterator = executor.getShardList().iterator();
				while (iterator.hasNext()) {
					Shard shard = iterator.next();
					if (jobName.equals(shard.getJobName())) {
						executor.setTotalLoadLevel(executor.getTotalLoadLevel() - shard.getLoadLevel());
						iterator.remove();
					}
				}
			}
			// 修正所有executor对该作业的jobNameList
			fixJobNameList(lastOnlineExecutorList, jobName);
			// 如果该作业是启用状态，则创建该作业的Shard
			if (allEnableJobs.contains(jobName)) {
				shardList.addAll(createShards(jobName, lastOnlineTrafficExecutorList));
			}

			return true;
		}
	}

	/**
	 * 作业的executor上线，executor级别平衡摘取，但是只能摘取该作业的shard；添加的新的shard
	 */
	private class ExecuteJobServerOnlineShardingTask extends AbstractAsyncShardingTask {

		private String jobName;
		private String executorName;

		public ExecuteJobServerOnlineShardingTask(String jobName, String executorName) {
			this.jobName = jobName;
			this.executorName = executorName;
		}

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {}, jobName is {}, executorName is {}", this.getClass().getSimpleName(), jobName,
					executorName);
		}

		private String getExecutorIp() throws Exception {
			String ip = null;
			String executorIpNodePath = SaturnExecutorsNode.getExecutorIpNodePath(executorName);
			if (curatorFramework.checkExists()
					.forPath(SaturnExecutorsNode.getExecutorIpNodePath(executorName)) != null) {
				byte[] ipBytes = curatorFramework.getData().forPath(executorIpNodePath);
				if (ipBytes != null) {
					ip = new String(ipBytes, "UTF-8");
				}
			}
			return ip;
		}

		private Shard createLocalShard(List<Executor> lastOnlineExecutorList, int loadLevel) {
			List<Integer> itemList = new ArrayList<>();
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				List<Shard> shardList = lastOnlineExecutorList.get(i).getShardList();
				for (int j = 0; j < shardList.size(); j++) {
					Shard shardAlreadyExists = shardList.get(j);
					if (shardAlreadyExists.getJobName().equals(jobName)) {
						itemList.add(shardAlreadyExists.getItem());
					}
				}
			}
			int item = 0;
			if (!itemList.isEmpty()) {
				boolean[] flags = new boolean[itemList.size() + 1];
				for (int i = 0; i < itemList.size(); i++) {
					flags[itemList.get(i)] = true;
				}
				for (int i = 0; i < flags.length; i++) {
					if (!flags[i]) {
						item = i;
						break;
					}
				}
			}
			Shard shard = new Shard();
			shard.setJobName(jobName);
			shard.setItem(item);
			shard.setLoadLevel(loadLevel);
			return shard;
		}

		private boolean hasShardRunning(List<Executor> lastOnlineExecutorList) {
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				List<Shard> shardList = lastOnlineExecutorList.get(i).getShardList();
				for (int j = 0; j < shardList.size(); j++) {
					if (shardList.get(j).getJobName().equals(jobName)) {
						return true;
					}
				}
			}
			return false;
		}

		private List<Shard> pickShardsRunningInDispreferList(List<String> preferListConfigured,
				List<Executor> lastOnlineExecutorList) {
			List<Shard> shards = new ArrayList<>();
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				if (!preferListConfigured.contains(executor.getExecutorName())) {
					Iterator<Shard> iterator = executor.getShardList().iterator();
					while (iterator.hasNext()) {
						Shard shard = iterator.next();
						if (shard.getJobName().equals(jobName)) {
							executor.setTotalLoadLevel(executor.getTotalLoadLevel() - shard.getLoadLevel());
							iterator.remove();
							shards.add(shard);
						}
					}
				}
			}
			return shards;
		}

		private int getTotalLoadLevel(List<Shard> shardList, List<Executor> executorList) {
			int total = 0;
			for (int i = 0; i < shardList.size(); i++) {
				total += shardList.get(i).getLoadLevel();
			}
			for (int i = 0; i < executorList.size(); i++) {
				total += executorList.get(i).getTotalLoadLevel();
			}
			return total;
		}

		// 计算平均load，然后摘取最接近平均负载的shard。
		private void pickBalance(List<Shard> shardList, List<Executor> executorList) {
			int totalLoadLevel = getTotalLoadLevel(shardList, executorList);
			int averageTotalLoad = totalLoadLevel / (executorList.size());
			for (int i = 0; i < executorList.size(); i++) {
				Executor executor = executorList.get(i);
				while (true) {
					int pickLoadLevel = executor.getTotalLoadLevel() - averageTotalLoad;
					// 摘取现在totalLoad > 平均值的executor里面的shard
					if (pickLoadLevel > 0 && !executor.getShardList().isEmpty()) {
						Shard pickShard = null;
						for (int j = 0; j < executor.getShardList().size(); j++) {
							Shard shard = executor.getShardList().get(j);
							if (!shard.getJobName().equals(jobName)) { // 如果当前Shard不属于该作业，则不摘取，继续下一个
								continue;
							}
							if (pickShard == null) {
								pickShard = shard;
							} else {
								if (pickShard.getLoadLevel() >= pickLoadLevel) {
									if (shard.getLoadLevel() >= pickLoadLevel
											&& shard.getLoadLevel() < pickShard.getLoadLevel()) {
										pickShard = shard;
									}
								} else {
									if (shard.getLoadLevel() >= pickLoadLevel) {
										pickShard = shard;
									} else {
										if (shard.getLoadLevel() > pickShard.getLoadLevel()) {
											pickShard = shard;
										}
									}
								}
							}
						}
						if (pickShard != null) {
							executor.setTotalLoadLevel(executor.getTotalLoadLevel() - pickShard.getLoadLevel());
							executor.getShardList().remove(pickShard);
							shardList.add(pickShard);
						} else { // 没有符合摘取条件的，无需再选择摘取
							break;
						}
					} else { // 无需再选择摘取
						break;
					}
				}
			}
		}

		private List<Shard> createUnLocalShards(int shardingTotalCount, int loadLevel) {
			List<Shard> shards = new ArrayList<>();
			for (int i = 0; i < shardingTotalCount; i++) {
				Shard shard = new Shard();
				shard.setJobName(jobName);
				shard.setItem(i);
				shard.setLoadLevel(loadLevel);
				shards.add(shard);
			}
			return shards;
		}

		private boolean shardsAllRunningInDispreferList(List<String> preferListConfigured,
				List<Executor> lastOnlineExecutorList) {
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				if (preferListConfigured.contains(executorName)) {
					List<Shard> shardList = executor.getShardList();
					for (int j = 0; j < shardList.size(); j++) {
						if (shardList.get(j).getJobName().equals(jobName)) {
							return false;
						}
					}
				}
			}
			return true;
		}

		public void pickIntelligent(List<String> allEnableJobs, List<Shard> shardList,
                List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			boolean preferListIsConfigured = preferListIsConfigured(jobName); // 是否配置了preferList
			boolean useDispreferList = useDispreferList(jobName); // 是否useDispreferList
			List<String> preferListConfigured = getPreferListConfigured(jobName); // 配置态的preferList
			boolean localMode = isLocalMode(jobName);
			int shardingTotalCount = getShardingTotalCount(jobName);
			int loadLevel = getLoadLevel(jobName);

			if (localMode) {
				if (!preferListIsConfigured || preferListConfigured.contains(executorName)) {
					if (allEnableJobs.contains(jobName)) {
						shardList.add(createLocalShard(lastOnlineTrafficExecutorList, loadLevel));
					}
				}
			} else {
				boolean hasShardRunning = hasShardRunning(lastOnlineTrafficExecutorList);
				if (preferListIsConfigured) {
					if (preferListConfigured.contains(executorName)) {
						// 如果有分片正在运行，摘取全部运行在非优先节点上的分片，还可以平衡摘取
						if (hasShardRunning) {
							shardList.addAll(
									pickShardsRunningInDispreferList(preferListConfigured, lastOnlineTrafficExecutorList));
							pickBalance(shardList, lastOnlineTrafficExecutorList);
						} else {
							// 如果没有分片正在运行，则需要新建，无需平衡摘取
							if (allEnableJobs.contains(jobName)) {
								shardList.addAll(createUnLocalShards(shardingTotalCount, loadLevel));
							}
						}
					} else {
						if (useDispreferList) {
							// 如果有分片正在运行，并且都是运行在非优先节点上，可以平衡摘取分片
							// 如果有分片正在运行，并且有运行在优先节点上，则摘取全部运行在非优先节点上的分片，不能再平衡摘取
							if (hasShardRunning) {
								boolean shardsAllRunningInDispreferList = shardsAllRunningInDispreferList(
										preferListConfigured, lastOnlineTrafficExecutorList);
								if (shardsAllRunningInDispreferList) {
									pickBalance(shardList, lastOnlineTrafficExecutorList);
								} else {
									shardList.addAll(pickShardsRunningInDispreferList(preferListConfigured,
											lastOnlineTrafficExecutorList));
								}
							} else {
								// 如果没有分片正在运行，则需要新建，无需平衡摘取
								if (allEnableJobs.contains(jobName)) {
									shardList.addAll(createUnLocalShards(shardingTotalCount, loadLevel));
								}
							}
						} else { // 不能再平衡摘取
							// 摘取全部运行在非优先节点上的分片
							shardList.addAll(
									pickShardsRunningInDispreferList(preferListConfigured, lastOnlineTrafficExecutorList));
						}
					}
				} else {
					// 如果有分片正在运行，则平衡摘取
					if (hasShardRunning) {
						pickBalance(shardList, lastOnlineTrafficExecutorList);
					} else {
						// 如果没有分片正在运行，则需要新建，无需平衡摘取
						if (allEnableJobs.contains(jobName)) {
							shardList.addAll(createUnLocalShards(shardingTotalCount, loadLevel));
						}
					}
				}
			}
		}
		
		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			// 很小的可能性：status的新增事件先于ip的新增事件
			// 那么，如果lastOnlineExecutorList不包含executorName，则添加一个新的Executor
			// 添加当前作业至jobNameList
			Executor theExecutor = null;
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				if (executor.getExecutorName().equals(executorName)) {
					theExecutor = executor;
					break;
				}
			}
			if (theExecutor == null) {
				theExecutor = new Executor();
				theExecutor.setExecutorName(executorName);
				theExecutor.setIp(getExecutorIp());
				theExecutor.setNoTraffic(getExecutorNoTraffic(executorName));
				theExecutor.setShardList(new ArrayList<Shard>());
				theExecutor.setJobNameList(new ArrayList<String>());
				theExecutor.setTotalLoadLevel(0);
				lastOnlineExecutorList.add(theExecutor);
				if(!theExecutor.isNoTraffic()) {
					lastOnlineTrafficExecutorList.add(theExecutor);
				}
			}
			if (!theExecutor.getJobNameList().contains(jobName)) {
				theExecutor.getJobNameList().add(jobName);
			}
			
			// 如果该Executor流量被摘取，则无需摘取，返回true
			if(theExecutor.isNoTraffic()) {
				return true;
			}
			
			pickIntelligent(allEnableJobs, shardList, lastOnlineTrafficExecutorList);
			
			return true;
		}

	}

	/**
	 * 作业的executor下线，将该executor运行的该作业分片都摘取，如果是本地作业，则移除
	 */
	private class ExecuteJobServerOfflineShardingTask extends AbstractAsyncShardingTask {

		private String jobName;
		private String executorName;

		@Override
		protected void logStartInfo() {
			LOGGER.info("Execute the {}, jobName is {}, executorName is {}", this.getClass().getSimpleName(), jobName,
					executorName);
		}

		public ExecuteJobServerOfflineShardingTask(String jobName, String executorName) {
			this.jobName = jobName;
			this.executorName = executorName;
		}

		@Override
		protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
				List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
			boolean localMode = isLocalMode(jobName);

			// Should use lastOnlineExecutorList, because jobName should be removed from jobNameList.
			// But use lastOnlineTrafficExecutorList, the executor maybe cannot be found.
			for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				if (executor.getExecutorName().equals(executorName)) {
					Iterator<Shard> iterator = executor.getShardList().iterator();
					while (iterator.hasNext()) {
						Shard shard = iterator.next();
						if (shard.getJobName().equals(jobName)) {
							if (!localMode) {
								shardList.add(shard);
							}
							iterator.remove();
						}
					}
					executor.getJobNameList().remove(jobName);
					break;
				}
			}

			return true;
		}

	}

	/**
	 * 进行全量分片
	 * @throws Exception
	 */
	public void asyncShardingWhenExecutorAll() throws Exception {
		if (isLeadership()) {
			needAllSharding.set(true);
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteAllShardingTask());
			String shardAllAtOnce = SaturnExecutorsNode.getExecutorShardingNodePath("shardAllAtOnce");
			if (curatorFramework.checkExists().forPath(shardAllAtOnce) != null) {
				curatorFramework.delete().deletingChildrenIfNeeded().forPath(shardAllAtOnce);
			}
		}
	}

	/**
	 * 结点上线处理
	 * @param executorName
	 * @throws Exception
	 */
	public void asyncShardingWhenExecutorOnline(String executorName, String ip) throws Exception {
		if (isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteOnlineShardingTask(executorName, ip));
		}
	}

	/**
	 * 结点掉线处理
	 * @param executorName
	 * @throws Exception
	 */
	public void asyncShardingWhenExecutorOffline(String executorName) throws Exception {
		if (isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteOfflineShardingTask(executorName));
		}
	}
	
	/**
	 * 摘取流量
	 */
	public void asyncShardingWhenExtractExecutorTraffic(String executorName) throws Exception {
		if (isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteExtractTrafficShardingTask(executorName));
		}
	}

	/**
	 * 恢复流量
	 */
	public void asyncShardingWhenRecoverExecutorTraffic(String executorName) throws Exception {
		if (isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteRecoverTrafficShardingTask(executorName));
		}
	}

	/**
	 * 作业启用事件
	 * @param jobName
	 * @throws Exception
	 */
	public void asyncShardingWhenJobEnable(String jobName) throws Exception {
		if (isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteJobEnableShardingTask(jobName));
		}
	}

	/**
	 * 处理作业禁用事件
	 * @param jobName
	 * @throws Exception
	 */
	public void asyncShardingWhenJobDisable(String jobName) throws Exception {
		if (isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteJobDisableShardingTask(jobName));
		}
	}

	/**
	 * 处理作业全排
	 */
	public void asyncShardingWhenJobForceShard(String jobName) throws Exception {
		if (isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteJobForceShardShardingTask(jobName));
		}
	}

	/**
	 * 处理作业executor上线
	 */
	public void asyncShardingWhenJobServerOnline(String jobName, String executorName) throws Exception {
		if (isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteJobServerOnlineShardingTask(jobName, executorName));
		}
	}

	/**
	 * 处理作业executor下线
	 */
	public void asyncShardingWhenJobServerOffline(String jobName, String executorName) throws Exception {
		if (isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteJobServerOfflineShardingTask(jobName, executorName));
		}
	}

	/**
	 * 选举
	 * @throws Exception
	 */
	public void leaderElection() throws Exception {
		lock.lockInterruptibly();
		try {
			if (hasLeadership()) {
				return;
			}
			LOGGER.info("{}-{} leadership election", namespace, hostValue);
			LeaderLatch leaderLatch = new LeaderLatch(curatorFramework, SaturnExecutorsNode.LEADER_LATCHNODE_PATH);
			try {
				leaderLatch.start();
				int timeoutSeconds = 60;
				if (leaderLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
					if (!hasLeadership()) {
						// 清理、重置变量
						executorService.shutdownNow();
						while (!executorService.isTerminated()) { // 等待全部任务已经退出
							Thread.sleep(100L); // NOSONARA
							executorService.shutdownNow();
						}
						needAllSharding.set(false);
						shardingCount.set(0);
						executorService = newSingleThreadExecutor();

						// 持久化$Jobs节点
						if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.$JOBSNODE_PATH) == null) {
							curatorFramework.create().creatingParentsIfNeeded()
									.forPath(SaturnExecutorsNode.$JOBSNODE_PATH);
						}
						// 持久化LeaderValue
						curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
								.forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH, hostValue.getBytes("UTF-8"));

						// 提交全量分片线程
						needAllSharding.set(true);
						shardingCount.incrementAndGet();
						executorService.submit(new ExecuteAllShardingTask());
						LOGGER.info("{}-{} become leadership", namespace, hostValue);
					}
				} else {
					LOGGER.error("{}-{} leadership election is timeout({}s)", namespace, hostValue, timeoutSeconds);
				}
			} catch (InterruptedException e) {
				LOGGER.info("{}-{} leadership election is interrupted", namespace, hostValue);
				throw e;
			} catch (Exception e) {
				LOGGER.error(namespace + "-" + hostValue + " leadership election error", e);
				throw e;
			} finally {
				try {
					leaderLatch.close();
				} catch (IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		} finally {
			lock.unlock();
		}
	}

	private boolean hasLeadership() throws Exception {
		return curatorFramework.checkExists().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH) != null;
	}

	private boolean isLeadership() throws Exception {
		while (!hasLeadership()) {
			leaderElection();
		}
		return new String(curatorFramework.getData().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH), "UTF-8")
				.equals(hostValue);
	}

	private boolean isLeadershipOnly() throws Exception {
		if (hasLeadership()) {
			return new String(curatorFramework.getData().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH), "UTF-8")
					.equals(hostValue);
		} else {
			return false;
		}
	}

	private void releaseMyLeadership() throws Exception {
		if (isLeadershipOnly()) {
			curatorFramework.delete().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH);
		}
	}

	/**
	 * Firstly, shutdown thread pool. Secondly, release my leadership.
	 * @param shutdownNow true, the thread pool will be shutdownNow; false, it will be shutdown
	 */
	private void shutdownInner(boolean shutdownNow) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (executorService != null) {
				if(shutdownNow) {
					executorService.shutdownNow();
				} else {
					executorService.shutdown();
				}
			}
			try {
				if (curatorFramework.getZookeeperClient().isConnected()) {
					releaseMyLeadership();
				}
			} catch (Exception e) {
				LOGGER.error(namespace + "-" + hostValue + " delete leadership error", e);
			}
		} finally {
			lock.unlock();
		}
	}

	public void shutdown() throws InterruptedException {
		shutdownInner(true);
	}

	public NamespaceShardingContentService getNamespaceShardingContentService() {
		return namespaceShardingContentService;
	}

}
