package com.vip.saturn.job.sharding.task;

import com.google.common.collect.Lists;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingContentService;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAsyncShardingTask implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(AbstractAsyncShardingTask.class);

	private static final int LOAD_LEVEL_DEFAULT = 1;

	protected NamespaceShardingService namespaceShardingService;

	protected CuratorFramework curatorFramework;

	protected NamespaceShardingContentService namespaceShardingContentService;

	protected ExecutorService executorService;

	protected ReportAlarmService reportAlarmService;

	public AbstractAsyncShardingTask(NamespaceShardingService namespaceShardingService) {
		this.namespaceShardingService = namespaceShardingService;
		this.curatorFramework = namespaceShardingService.getCuratorFramework();
		this.namespaceShardingContentService = namespaceShardingService.getNamespaceShardingContentService();
		this.executorService = namespaceShardingService.getExecutorService();
		this.reportAlarmService = namespaceShardingService.getReportAlarmService();
	}

	protected abstract void logStartInfo();

	/**
	 * Special enable jobs that need to be notified prior, not consider whether whose shards are changed.
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
			if (!namespaceShardingService.isLeadershipOnly()) {
				return;
			}

			// 如果需要全量分片，且当前线程不是全量分片线程，则直接返回，没必要做分片
			if (namespaceShardingService.isNeedAllSharding() && !isAllShardingTask) {
				log.info("the {} will be ignored, because there will be {}", this.getClass().getSimpleName(),
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
				if (!namespaceShardingService.isLeadershipOnly()) {
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
			log.info("{}-{} {} is interrupted", namespaceShardingService.getNamespace(),
					namespaceShardingService.getHostValue(), this.getClass().getSimpleName());
			Thread.currentThread().interrupt();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			if (!isAllShardingTask) { // 如果当前不是全量分片，则需要全量分片来拯救异常
				namespaceShardingService.setNeedAllSharding(true);
				namespaceShardingService.shardingCountIncrementAndGet();
				executorService.submit(new ExecuteAllShardingTask(namespaceShardingService));
			} else { // 如果当前是全量分片，则告警并关闭当前服务，重选leader来做事情
				raiseAlarm();
				shutdownNamespaceShardingService();
			}
		} finally {
			if (isAllShardingTask) { // 如果是全量分片，不再进行全量分片
				namespaceShardingService.setNeedAllSharding(false);
			}
			namespaceShardingService.shardingCountDecrementAndGet();
		}
	}

	private void shutdownNamespaceShardingService() {
		try {
			namespaceShardingService.shutdownInner(false);
		} catch (InterruptedException e) {
			log.info("{}-{} {}-shutdownInner is interrupted", namespaceShardingService.getNamespace(),
					namespaceShardingService.getHostValue(),
					this.getClass().getSimpleName());
			Thread.currentThread().interrupt();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	private void raiseAlarm() {
		if (reportAlarmService != null) {
			try {
				reportAlarmService.allShardingError(namespaceShardingService.getNamespace(),
						namespaceShardingService.getHostValue());
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}
	}

	private boolean shardingContentIsChanged(List<Executor> oldOnlineExecutorList,
			List<Executor> lastOnlineExecutorList) {
		return !namespaceShardingContentService.toShardingContent(oldOnlineExecutorList)
				.equals(namespaceShardingContentService
						.toShardingContent(lastOnlineExecutorList));
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
			if (curatorFramework.checkExists().forPath(jobServersExecutorStatusNodePath)
					!= null) {
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
		Integer shardingCount = 1;
		if (null != curatorFramework.checkExists()
				.forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH)) {
			byte[] shardingCountData = curatorFramework.getData()
					.forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH);
			if (shardingCountData != null) {
				try {
					shardingCount = Integer.parseInt(new String(shardingCountData, StandardCharsets.UTF_8.name())) + 1;
				} catch (NumberFormatException e) {
					log.error("parse shardingCount error", e);
				}
			}
			curatorFramework.setData().forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH,
					shardingCount.toString().getBytes(StandardCharsets.UTF_8.name()));
		} else {
			curatorFramework.create().creatingParentsIfNeeded()
					.forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH,
							shardingCount.toString().getBytes(StandardCharsets.UTF_8.name()));
		}
	}

	/**
	 * Get the jobs, that are enabled, and whose shards are changed. Specially, return all enabled jobs when the current
	 * thread is all-shard-task
	 * Return the jobs and their shardContent.
	 */
	private Map<String, Map<String, List<Integer>>> getEnabledAndShardsChangedJobShardContent(
			boolean isAllShardingTask, List<String> allEnableJobs, List<Executor> oldOnlineExecutorList,
			List<Executor> lastOnlineExecutorList) {
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
			Iterator<Entry<String, List<Integer>>> oldIterator = oldShardingItems.entrySet().iterator();
			wl_loop:
			while (oldIterator.hasNext()) {
				Entry<String, List<Integer>> next = oldIterator.next();
				String executorName = next.getKey();
				if (!lastShardingItems.containsKey(executorName)) {
					isChanged = true;
					break;
				}
				List<Integer> shards = next.getValue();
				List<Integer> newShard = lastShardingItems.get(executorName);
				if ((shards == null && newShard != null) || (shards != null && newShard == null)) {
					isChanged = true;
					break;
				}

				if (shards == null || newShard == null) {
					continue;
				}

				for (Integer shard : shards) {
					if (!newShard.contains(shard)) {
						isChanged = true;
						break wl_loop;
					}
				}
			}
			if (!isChanged) {
				Iterator<Entry<String, List<Integer>>> newIterator = lastShardingItems.entrySet().iterator();
				while (newIterator.hasNext()) {
					Entry<String, List<Integer>> next = newIterator.next();
					String executorName = next.getKey();
					if (!oldShardingItems.containsKey(executorName)) {
						isChanged = true;
						break;
					}
					List<Integer> shards = next.getValue();
					List<Integer> oldShard = oldShardingItems.get(executorName);
					if ((shards == null && oldShard != null) || (shards != null && oldShard == null)) {
						isChanged = true;
						break;
					}

					if (shards == null || oldShard == null) {
						continue;
					}

					if (hasShardChanged(shards, oldShard)) {
						isChanged = true;
						break;
					}
				}
			}

			if (isChanged) {
				jobShardContent.put(enableJob, lastShardingItems);
			}
		}
		return jobShardContent;
	}

	private boolean hasShardChanged(List<Integer> shards, List<Integer> oldShard) {
		for (Integer shard : shards) {
			if (!oldShard.contains(shard)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isLocalMode(String jobName) throws Exception {
		String localNodePath = SaturnExecutorsNode.getJobConfigLocalModeNodePath(jobName);
		if (curatorFramework.checkExists().forPath(localNodePath) != null) {
			byte[] data = curatorFramework.getData().forPath(localNodePath);
			if (data != null) {
				return Boolean.parseBoolean(new String(data, StandardCharsets.UTF_8.name()));
			}
		}
		return false;
	}

	protected int getShardingTotalCount(String jobName) throws Exception {
		int shardingTotalCount = 0;
		String jobConfigShardingTotalCountNodePath = SaturnExecutorsNode
				.getJobConfigShardingTotalCountNodePath(jobName);
		if (curatorFramework.checkExists().forPath(jobConfigShardingTotalCountNodePath)
				!= null) {
			byte[] shardingTotalCountData = curatorFramework.getData()
					.forPath(jobConfigShardingTotalCountNodePath);
			if (shardingTotalCountData != null) {
				try {
					shardingTotalCount = Integer
							.parseInt(new String(shardingTotalCountData, StandardCharsets.UTF_8.name()));
				} catch (NumberFormatException e) {
					log.error("parse shardingTotalCount error, will use the default value", e);
				}
			}
		}
		return shardingTotalCount;
	}

	protected int getLoadLevel(String jobName) throws Exception {
		int loadLevel = LOAD_LEVEL_DEFAULT;
		String jobConfigLoadLevelNodePath = SaturnExecutorsNode.getJobConfigLoadLevelNodePath(jobName);
		if (curatorFramework.checkExists().forPath(jobConfigLoadLevelNodePath) != null) {
			byte[] loadLevelData = curatorFramework.getData()
					.forPath(jobConfigLoadLevelNodePath);
			try {
				if (loadLevelData != null) {
					loadLevel = Integer.parseInt(new String(loadLevelData, StandardCharsets.UTF_8.name()));
				}
			} catch (NumberFormatException e) {
				log.error("parse loadLevel error, will use the default value", e);
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
	 *
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
		// if CONTAINER_ALIGN_WITH_PHYSICAL = false, return all executors; otherwise, return all non-container executors.
		if (NamespaceShardingService.CONTAINER_ALIGN_WITH_PHYSICAL) {
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
			log
					.warn("Unnecessary to put shards back to executors balanced because of no executor");
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

		Iterator<Shard> iterator = shardList.iterator();
		while (iterator.hasNext()) {
			String jobName = iterator.next().getJobName();

			checkAndPutIntoMap(jobName,
					filterExecutorsByJob(notDockerExecutors, jobName), noDockerTrafficExecutorsMapByJob);

			checkAndPutIntoMap(jobName, filterExecutorsByJob(lastOnlineTrafficExecutorList, jobName),
					lastOnlineTrafficExecutorListMapByJob);

			checkAndPutIntoMap(jobName, isLocalMode(jobName), localModeMap);

			checkAndPutIntoMap(jobName, preferListIsConfigured(jobName), preferListIsConfiguredMap);

			checkAndPutIntoMap(jobName, getPreferListConfigured(jobName), preferListConfiguredMap);

			checkAndPutIntoMap(jobName, useDispreferList(jobName), useDispreferListMap);

		}

		// 整体算法放回算法：拿取Shard，放进负荷最小的executor

		// 1、放回localMode的Shard
		// 如果配置了preferList，则选取preferList中的executor。
		// 如果preferList中的executor都挂了，则不转移；否则，选取没有接管该作业的executor列表的loadLevel最小的一个。
		// 如果没有配置preferList，则选取没有接管该作业的executor列表的loadLevel最小的一个。
		putBackShardWithLocalMode(shardList, noDockerTrafficExecutorsMapByJob,
				lastOnlineTrafficExecutorListMapByJob,
				localModeMap, preferListIsConfiguredMap, preferListConfiguredMap);

		// 2、放回配置了preferList的Shard
		putBackShardWithPreferList(shardList, lastOnlineTrafficExecutorListMapByJob, preferListIsConfiguredMap,
				preferListConfiguredMap, useDispreferListMap);

		// 3、放回没有配置preferList的Shard
		putBackShardWithoutPreferlist(shardList, noDockerTrafficExecutorsMapByJob);
	}

	private <T> void checkAndPutIntoMap(String key, T value, Map<String, T> targetMap) {
		if (!targetMap.containsKey(key)) {
			targetMap.put(key, value);
		}
	}

	private void putBackShardWithoutPreferlist(List<Shard> shardList,
			Map<String, List<Executor>> noDockerTrafficExecutorsMapByJob) {
		Iterator<Shard> iterator = shardList.iterator();
		while (iterator.hasNext()) {
			Shard shard = iterator.next();
			Executor executor = getExecutorWithMinLoadLevel(
					noDockerTrafficExecutorsMapByJob.get(shard.getJobName()));
			putShardIntoExecutor(shard, executor);
			iterator.remove();
		}
	}

	private void putBackShardWithPreferList(List<Shard> shardList,
			Map<String, List<Executor>> lastOnlineTrafficExecutorListMapByJob,
			Map<String, Boolean> preferListIsConfiguredMap, Map<String, List<String>> preferListConfiguredMap,
			Map<String, Boolean> useDispreferListMap) {
		Iterator<Shard> iterator = shardList.iterator();
		while (iterator.hasNext()) {
			Shard shard = iterator.next();
			String jobName = shard.getJobName();
			if (preferListIsConfiguredMap.get(jobName)) { // fix,
				// preferList为空不能作为判断是否配置preferList的依据，比如说配置了容器资源，但是全部下线了。
				List<String> preferList = preferListConfiguredMap.get(jobName);
				List<Executor> preferExecutorList = getPreferExecutors(
						lastOnlineTrafficExecutorListMapByJob, jobName, preferList);
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

	private void putBackShardWithLocalMode(List<Shard> shardList,
			Map<String, List<Executor>> noDockerTrafficExecutorsMapByJob,
			Map<String, List<Executor>> lastOnlineTrafficExecutorListMapByJob, Map<String, Boolean> localModeMap,
			Map<String, Boolean> preferListIsConfiguredMap, Map<String, List<String>> preferListConfiguredMap) {
		Iterator<Shard> iterator = shardList.iterator();
		while (iterator.hasNext()) {
			Shard shard = iterator.next();
			String jobName = shard.getJobName();
			if (!localModeMap.get(jobName)) {
				continue;
			}

			if (preferListIsConfiguredMap.get(jobName)) {
				List<String> preferListConfigured = preferListConfiguredMap.get(jobName);
				if (!preferListConfigured.isEmpty()) {
					List<Executor> preferExecutorList = getPreferExecutors(lastOnlineTrafficExecutorListMapByJob,
							jobName, preferListConfigured);
					if (!preferExecutorList.isEmpty()) {
						Executor executor = getExecutorWithMinLoadLevelAndNoThisJob(preferExecutorList,
								jobName);
						putShardIntoExecutor(shard, executor);
					}
				}
			} else {
				Executor executor = getExecutorWithMinLoadLevelAndNoThisJob(
						noDockerTrafficExecutorsMapByJob.get(jobName),
						jobName);
				putShardIntoExecutor(shard, executor);
			}
			iterator.remove();
		}
	}

	private List<Executor> getPreferExecutors(Map<String, List<Executor>> lastOnlineTrafficExecutorListMapByJob,
			String jobName, List<String> preferListConfigured) {
		List<Executor> preferExecutorList = new ArrayList<>();
		List<Executor> lastOnlineTrafficExecutorListByJob = lastOnlineTrafficExecutorListMapByJob
				.get(jobName);
		for (int i = 0; i < lastOnlineTrafficExecutorListByJob.size(); i++) {
			Executor executor = lastOnlineTrafficExecutorListByJob.get(i);
			if (preferListConfigured.contains(executor.getExecutorName())) {
				preferExecutorList.add(executor);
			}
		}
		return preferExecutorList;
	}


	/**
	 * 是否使用非preferList:
	 * 1、存在结点，并且该结点值为false，返回false；
	 * 2、其他情况，返回true
	 */
	protected boolean useDispreferList(String jobName) throws Exception {
		String jobConfigUseDispreferListNodePath = SaturnExecutorsNode
				.getJobConfigUseDispreferListNodePath(jobName);
		if (curatorFramework.checkExists().forPath(jobConfigUseDispreferListNodePath)
				!= null) {
			byte[] useDispreferListData = curatorFramework.getData()
					.forPath(jobConfigUseDispreferListNodePath);
			if (useDispreferListData != null && !Boolean
					.parseBoolean(new String(useDispreferListData, StandardCharsets.UTF_8.name()))) {
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
				log.error("The shard({}-{}) is running in the executor of {}, cannot be put again",
						shard.getJobName(), shard.getItem(), executor.getExecutorName());
			} else {
				executor.getShardList().add(shard);
				executor.setTotalLoadLevel(executor.getTotalLoadLevel() + shard.getLoadLevel());
			}
		} else {
			log.info("No executor to take over the shard: {}-{}", shard.getJobName(), shard.getItem());
		}
	}

	/**
	 * 获取该域下的所有作业
	 */
	private List<String> getAllJobs() throws Exception {
		List<String> allJob = new ArrayList<>();
		if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.JOBSNODE_PATH)
				== null) {
			curatorFramework.create().creatingParentsIfNeeded()
					.forPath(SaturnExecutorsNode.JOBSNODE_PATH);
		}
		List<String> tmp = curatorFramework.getChildren()
				.forPath(SaturnExecutorsNode.JOBSNODE_PATH);
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
				if (enableData != null && Boolean.parseBoolean(new String(enableData, StandardCharsets.UTF_8.name()))) {
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
				return new String(preferListData, StandardCharsets.UTF_8.name()).trim().length() > 0;
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
				String[] split = new String(preferListData, StandardCharsets.UTF_8.name()).split(",");
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
		if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorsNodePath())
				!= null) {
			List<String> executors = curatorFramework.getChildren()
					.forPath(SaturnExecutorsNode.getExecutorsNodePath());
			if (executors != null) {
				allExistsExecutors.addAll(executors);
			}
		}
		return allExistsExecutors;
	}

	/**
	 * 如果prefer不是docker容器，并且preferList不包含，则直接添加；
	 *
	 * 如果prefer是docker容器（以@开头），则prefer为task，获取该task下的所有executor，如果不包含，添加进preferList。
	 */
	private void fillRealPreferListIfIsDockerOrNot(List<String> preferList, String prefer,
			List<String> allExistsExecutors) throws Exception {
		if (!prefer.startsWith("@")) { // not docker server
			if (!preferList.contains(prefer)) {
				preferList.add(prefer);
			}
			return;
		}

		String task = prefer.substring(1);
		for (int i = 0; i < allExistsExecutors.size(); i++) {
			String executor = allExistsExecutors.get(i);
			if (curatorFramework.checkExists()
					.forPath(SaturnExecutorsNode.getExecutorTaskNodePath(executor)) != null) {
				byte[] taskData = curatorFramework.getData()
						.forPath(SaturnExecutorsNode.getExecutorTaskNodePath(executor));
				if (taskData != null && task.equals(new String(taskData, StandardCharsets.UTF_8.name())) && !preferList
						.contains(executor)) {
					preferList.add(executor);
				}
			}
		}
	}

	protected List<Executor> filterExecutorsByJob(List<Executor> executorList, String jobName) {
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
		return curatorFramework.checkExists()
				.forPath(SaturnExecutorsNode.getExecutorNoTrafficNodePath(executorName)) != null;
	}

}
