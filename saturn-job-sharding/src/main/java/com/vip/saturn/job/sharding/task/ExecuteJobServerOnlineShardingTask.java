package com.vip.saturn.job.sharding.task;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 作业的executor上线，executor级别平衡摘取，但是只能摘取该作业的shard；添加的新的shard
 */
public class ExecuteJobServerOnlineShardingTask extends AbstractAsyncShardingTask {

	private static final Logger log = LoggerFactory.getLogger(ExecuteJobServerOnlineShardingTask.class);

	private String jobName;

	private String executorName;

	public ExecuteJobServerOnlineShardingTask(NamespaceShardingService namespaceShardingService, String jobName,
			String executorName) {
		super(namespaceShardingService);
		this.jobName = jobName;
		this.executorName = executorName;
	}

	@Override
	protected void logStartInfo() {
		log.info("Execute the {}, jobName is {}, executorName is {}", this.getClass().getSimpleName(), jobName,
				executorName);
	}

	private String getExecutorIp() throws Exception {
		String ip = null;
		String executorIpNodePath = SaturnExecutorsNode.getExecutorIpNodePath(executorName);
		if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorIpNodePath(executorName)) != null) {
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
			int itemListSize = itemList.size();
			boolean[] flags = new boolean[itemListSize + 1];
			for (int i = 0; i < itemListSize; i++) {
				Integer itemAlreadyExists = itemList.get(i);
				if (itemAlreadyExists <= itemListSize) {
					flags[itemAlreadyExists] = true;
				}
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
		for (Executor executor : executorList) {
			pickBalanceOnAExecutor(shardList, executor, averageTotalLoad);
		}
	}

	private void pickBalanceOnAExecutor(List<Shard> shardList, Executor executor, int averageTotalLoad) {
		int pickLoadLevel = executor.getTotalLoadLevel() - averageTotalLoad;
		while (pickLoadLevel > 0 && !executor.getShardList().isEmpty()) {
			// 摘取现在totalLoad > 平均值的executor里面的shard
			Shard pickShard = null;
			for (int j = 0; j < executor.getShardList().size(); j++) {
				Shard shard = executor.getShardList().get(j);
				if (!shard.getJobName().equals(jobName)) { // 如果当前Shard不属于该作业，则不摘取，继续下一个
					continue;
				}
				if (pickShard == null) {
					pickShard = shard;
					continue;
				}
				if (pickShard.getLoadLevel() >= pickLoadLevel) {
					if (shard.getLoadLevel() >= pickLoadLevel && shard.getLoadLevel() < pickShard.getLoadLevel()) {
						pickShard = shard;
					}
					continue;
				}
				if (shard.getLoadLevel() >= pickLoadLevel) {
					pickShard = shard;
				} else {
					if (shard.getLoadLevel() > pickShard.getLoadLevel()) {
						pickShard = shard;
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
			pickLoadLevel = executor.getTotalLoadLevel() - averageTotalLoad;
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
		List<String> preferListConfigured = getPreferListConfigured(jobName); // 配置态的preferList
		boolean localMode = isLocalMode(jobName);
		int loadLevel = getLoadLevel(jobName);
		if (localMode) {
			if ((!preferListIsConfigured || preferListConfigured.contains(executorName))
					&& allEnableJobs.contains(jobName)) {
				shardList.add(createLocalShard(lastOnlineTrafficExecutorList, loadLevel));
			}
			return;
		}

		int shardingTotalCount = getShardingTotalCount(jobName);
		boolean hasShardRunning = hasShardRunning(lastOnlineTrafficExecutorList);
		if (preferListIsConfigured) {
			pickIntelligentWithPreferListConfigured(allEnableJobs, shardList, lastOnlineTrafficExecutorList,
					preferListConfigured, hasShardRunning, shardingTotalCount, loadLevel);

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

	public void pickIntelligentWithPreferListConfigured(List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineTrafficExecutorList, List<String> preferListConfigured, boolean hasShardRunning,
			int shardingTotalCount, int loadLevel) throws Exception {
		boolean useDispreferList = useDispreferList(jobName); // 是否useDispreferList

		if (preferListConfigured.contains(executorName)) {
			// 如果有分片正在运行，摘取全部运行在非优先节点上的分片，还可以平衡摘取
			if (hasShardRunning) {
				shardList.addAll(pickShardsRunningInDispreferList(preferListConfigured, lastOnlineTrafficExecutorList));
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
					boolean shardsAllRunningInDispreferList = shardsAllRunningInDispreferList(preferListConfigured,
							lastOnlineTrafficExecutorList);
					if (shardsAllRunningInDispreferList) {
						pickBalance(shardList, lastOnlineTrafficExecutorList);
					} else {
						shardList.addAll(
								pickShardsRunningInDispreferList(preferListConfigured, lastOnlineTrafficExecutorList));
					}
				} else {
					// 如果没有分片正在运行，则需要新建，无需平衡摘取
					if (allEnableJobs.contains(jobName)) {
						shardList.addAll(createUnLocalShards(shardingTotalCount, loadLevel));
					}
				}
			} else { // 不能再平衡摘取
				// 摘取全部运行在非优先节点上的分片
				shardList.addAll(pickShardsRunningInDispreferList(preferListConfigured, lastOnlineTrafficExecutorList));
			}
		}
	}

	@Override
	protected boolean pick(List<String> allJobs, List<String> allEnableJobs, List<Shard> shardList,
			List<Executor> lastOnlineExecutorList, List<Executor> lastOnlineTrafficExecutorList) throws Exception {
		// 很小的可能性：status的新增事件先于ip的新增事件
		// 那么，如果lastOnlineExecutorList不包含executorName，则添加一个新的Executor
		// 添加当前作业至jobNameList
		Executor targetExecutor = null;
		for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
			Executor executor = lastOnlineExecutorList.get(i);
			if (executor.getExecutorName().equals(executorName)) {
				targetExecutor = executor;
				break;
			}
		}
		if (targetExecutor == null) {
			targetExecutor = new Executor();
			targetExecutor.setExecutorName(executorName);
			targetExecutor.setIp(getExecutorIp());
			targetExecutor.setNoTraffic(getExecutorNoTraffic(executorName));
			targetExecutor.setShardList(new ArrayList<Shard>());
			targetExecutor.setJobNameList(new ArrayList<String>());
			targetExecutor.setTotalLoadLevel(0);
			lastOnlineExecutorList.add(targetExecutor);
			if (!targetExecutor.isNoTraffic()) {
				lastOnlineTrafficExecutorList.add(targetExecutor);
			}
		}
		if (!targetExecutor.getJobNameList().contains(jobName)) {
			targetExecutor.getJobNameList().add(jobName);
		}

		// 如果该Executor流量被摘取，则无需摘取，返回true
		if (targetExecutor.isNoTraffic()) {
			return true;
		}

		pickIntelligent(allEnableJobs, shardList, lastOnlineTrafficExecutorList);

		return true;
	}

}
