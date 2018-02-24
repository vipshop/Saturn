package com.vip.saturn.job.sharding.service;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;

/**
 * @author hebelala
 */
public class NamespaceShardingContentService {

	private static final Logger log = LoggerFactory.getLogger(NamespaceShardingContentService.class);

	private static final int SHARDING_CONTENT_SLICE_LEN = 1024 * 1023;

	private CuratorFramework curatorFramework;

	private Gson gson = new Gson();

	public NamespaceShardingContentService(CuratorFramework curatorFramework) {
		this.curatorFramework = curatorFramework;
	}

	public void persistDirectly(List<Executor> executorList) throws Exception {
		// sharding/content如果不存在，则新建
		if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.SHARDING_CONTENTNODE_PATH) == null) {
			curatorFramework.create().creatingParentsIfNeeded().forPath(SaturnExecutorsNode.SHARDING_CONTENTNODE_PATH);
		}
		// 删除sharding/content节点下的内容
		List<String> shardingContent = curatorFramework.getChildren()
				.forPath(SaturnExecutorsNode.SHARDING_CONTENTNODE_PATH);
		if (shardingContent != null && !shardingContent.isEmpty()) {
			for (String shardingConentElement : shardingContent) {
				curatorFramework.delete()
						.forPath(SaturnExecutorsNode.getShardingContentElementNodePath(shardingConentElement));
			}
		}

		// 持久化新的内容
		String shardingContentStr = toShardingContent(executorList);
		log.info("Persisit sharding content: {}", shardingContentStr);
		// 如果内容过大，分开节点存储。不能使用事务提交，因为即使使用事务、写多个节点，但是提交事务时，仍然会报长度过长的错误。
		byte[] shardingContentBytes = shardingContentStr.getBytes("UTF-8");
		int length = shardingContentBytes.length;
		int sliceCount = length / SHARDING_CONTENT_SLICE_LEN + 1;
		for (int i = 0; i < sliceCount; i++) {
			int start = SHARDING_CONTENT_SLICE_LEN * i;
			int end = start + SHARDING_CONTENT_SLICE_LEN;
			if (end > length) {
				end = length;
			}
			byte[] subBytes = Arrays.copyOfRange(shardingContentBytes, start, end);
			curatorFramework.create().forPath(SaturnExecutorsNode.getShardingContentElementNodePath(String.valueOf(i)),
					subBytes);
		}
	}

	public Map<String, List<Integer>> getShardingItems(List<Executor> executorList, String jobName) {
		if (executorList == null || executorList.isEmpty()) {
			return Maps.newHashMap();
		}

		Map<String, List<Integer>> shardingItems = new HashMap<>();
		for (Executor tmp : executorList) {
			if (tmp.getJobNameList() != null && tmp.getJobNameList().contains(jobName)) {
				List<Integer> items = new ArrayList<>();
				for (Shard shard : tmp.getShardList()) {
					if (shard.getJobName().equals(jobName)) {
						items.add(shard.getItem());
					}
				}
				shardingItems.put(tmp.getExecutorName(), items);
			}
		}
		return shardingItems;
	}

	/**
	 * @param jobName 作业名
	 * @return 返回Map数据，key值为executorName, value为分片项集合
	 */
	public Map<String, List<Integer>> getShardingItems(String jobName) throws Exception {
		List<Executor> executorList = getExecutorList();
		return getShardingItems(executorList, jobName);
	}

	/**
	 * 从sharding/content获取数据
	 */
	public List<Executor> getExecutorList() throws Exception {
		List<Executor> executorList = new ArrayList<>();
		// Sharding/content 内容多的时候，分多个节点存数据
		if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.SHARDING_CONTENTNODE_PATH) != null) {
			List<String> elementNodes = curatorFramework.getChildren()
					.forPath(SaturnExecutorsNode.SHARDING_CONTENTNODE_PATH);
			Collections.sort(elementNodes, new Comparator<String>() {
				@Override
				public int compare(String arg0, String arg1) {
					Integer a = Integer.valueOf(arg0);
					Integer b = Integer.valueOf(arg1);
					return a.compareTo(b);
				}
			});
			List<Byte> dataByteList = new ArrayList<>();
			for (String elementNode : elementNodes) {
				byte[] elementData = curatorFramework.getData()
						.forPath(SaturnExecutorsNode.getShardingContentElementNodePath(elementNode));
				for (int i = 0; i < elementData.length; i++) {
					dataByteList.add(elementData[i]);
				}
			}
			byte[] dataArray = new byte[dataByteList.size()];
			for (int i = 0; i < dataByteList.size(); i++) {
				dataArray[i] = dataByteList.get(i);
			}
			List<Executor> tmp = gson.fromJson(new String(dataArray, "UTF-8"), new TypeToken<List<Executor>>() {
			}.getType());
			if (tmp != null) {
				executorList.addAll(tmp);
			}
		}
		return executorList;
	}

	public String toShardingContent(List<Executor> executorList) {
		return gson.toJson(executorList);
	}

	public void persistJobsNecessaryInTransaction(
			Map<String/* jobName */, Map<String/* executorName */, List<Integer>/* items */>> jobShardContent)
			throws Exception {
		if (!jobShardContent.isEmpty()) {
			log.info("Notify jobs sharding necessary, jobs is {}", jobShardContent.keySet());
			CuratorTransactionFinal curatorTransactionFinal = curatorFramework.inTransaction().check().forPath("/")
					.and();
			Iterator<Map.Entry<String, Map<String, List<Integer>>>> iterator = jobShardContent.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Map<String, List<Integer>>> next = iterator.next();
				String jobName = next.getKey();
				Map<String, List<Integer>> shardContent = next.getValue();
				String shardContentJson = gson.toJson(shardContent);
				byte[] necessaryContent = shardContentJson.getBytes("UTF-8");
				// 更新$Jobs/xx/leader/sharding/neccessary 节点的内容为新分配的sharding 内容
				String jobLeaderShardingNodePath = SaturnExecutorsNode.getJobLeaderShardingNodePath(jobName);
				String jobLeaderShardingNecessaryNodePath = SaturnExecutorsNode
						.getJobLeaderShardingNecessaryNodePath(jobName);
				if (curatorFramework.checkExists().forPath(jobLeaderShardingNodePath) == null) {
					curatorFramework.create().creatingParentsIfNeeded().forPath(jobLeaderShardingNodePath);
				}
				if (curatorFramework.checkExists().forPath(jobLeaderShardingNecessaryNodePath) == null) {
					curatorTransactionFinal.create().forPath(jobLeaderShardingNecessaryNodePath, necessaryContent)
							.and();
				} else {
					curatorTransactionFinal.setData().forPath(jobLeaderShardingNecessaryNodePath, necessaryContent)
							.and();
				}
			}
			curatorTransactionFinal.commit();
		}
	}

	public Map<String, List<Integer>> getShardContent(String jobName, String jobNecessaryContent) throws Exception {
		Map<String, List<Integer>> shardContent = new HashMap<>();
		try {
			// TODO: can change to Jackson ?
			Map<String, List<Integer>> obj = gson.fromJson(jobNecessaryContent,
					new TypeToken<Map<String, List<Integer>>>() {
					}.getType());
			shardContent.putAll(obj);
		} catch (Exception e) {
			log.warn("get " + jobName + "'s shards from necessary failed, will try to get shards from sharding/content",
					e);
			shardContent.putAll(getShardingItems(jobName));
		}
		return shardContent;
	}

}
