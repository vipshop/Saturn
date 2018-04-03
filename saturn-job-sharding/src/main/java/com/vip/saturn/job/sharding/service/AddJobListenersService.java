package com.vip.saturn.job.sharding.service;

import com.google.common.collect.Lists;
import com.vip.saturn.job.sharding.exception.ShardingException;
import com.vip.saturn.job.sharding.listener.JobConfigTriggerShardingListener;
import com.vip.saturn.job.sharding.listener.JobServersTriggerShardingListener;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author chembo.huang
 */
public class AddJobListenersService {

	private static final Logger log = LoggerFactory.getLogger(AddJobListenersService.class);

	private CuratorFramework curatorFramework;
	private NamespaceShardingService namespaceShardingService;
	private String namespace;
	private ShardingTreeCacheService shardingTreeCacheService;

	public AddJobListenersService(String namespace, CuratorFramework curatorFramework,
			NamespaceShardingService namespaceShardingService, ShardingTreeCacheService shardingTreeCacheService) {
		this.curatorFramework = curatorFramework;
		this.namespaceShardingService = namespaceShardingService;
		this.namespace = namespace;
		this.shardingTreeCacheService = shardingTreeCacheService;
	}

	public void addExistJobPathListener() throws Exception {
		if (null != curatorFramework.checkExists().forPath(SaturnExecutorsNode.JOBSNODE_PATH)) {
			List<String> jobs = curatorFramework.getChildren().forPath(SaturnExecutorsNode.JOBSNODE_PATH);
			log.info("addExistJobPathListener, jobs = {}", jobs);
			if (jobs != null) {
				for (String job : jobs) {
					addJobPathListener(job);
				}
			}
		}
	}

	public void addJobPathListener(String jobName) throws Exception {
		if (addJobConfigPathListener(jobName)) {
			addJobServersPathListener(jobName);
		}
	}

	public void removeJobPathTreeCache(String jobName) throws ShardingException {
		removeJobConfigPathTreeCache(jobName);
		removeJobServersPathTreeCache(jobName);
	}

	private void removeJobConfigPathTreeCache(String jobName) throws ShardingException {
		int depth = 0;
		List<String> configPath2AddTreeCacheList = Lists
				.newArrayList(SaturnExecutorsNode.getJobConfigEnableNodePath(jobName), SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName));
		for (String path2AddTreeCache : configPath2AddTreeCacheList) {
			shardingTreeCacheService.removeTreeCache(path2AddTreeCache, depth);
		}
	}

	private void removeJobServersPathTreeCache(String jobName) throws ShardingException {
		String path = SaturnExecutorsNode.getJobServersNodePath(jobName);
		int depth = 2;
		shardingTreeCacheService.removeTreeCache(path, depth);
	}

	/**
	 * Add job config path listener
	 * @return false, if the job/config path is not existing
	 */
	private boolean addJobConfigPathListener(String jobName) throws Exception {
		String path = SaturnExecutorsNode.JOBSNODE_PATH + "/" + jobName + "/config";
		String fullPath = namespace + path;

		int waitConfigPathCreatedCounts = 50;
		do {
			waitConfigPathCreatedCounts--;
			if (curatorFramework.checkExists().forPath(path) != null) {
				break;
			}
			if (waitConfigPathCreatedCounts == 0) {
				log.warn("the path {} is not existing", fullPath);
				return false;
			}
			Thread.sleep(100L);
		} while (true);

		int depth = 0;
		List<String> configPath2AddTreeCacheList = Lists
				.newArrayList(SaturnExecutorsNode.getJobConfigEnableNodePath(jobName),
						SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName));
		for (String path2AddTreeCache : configPath2AddTreeCacheList) {
			shardingTreeCacheService.addTreeCacheIfAbsent(path2AddTreeCache, depth);
			shardingTreeCacheService.addTreeCacheListenerIfAbsent(path2AddTreeCache, depth,
					new JobConfigTriggerShardingListener(jobName, namespaceShardingService));
		}

		return true;
	}

	private void addJobServersPathListener(String jobName) throws Exception {
		String path = SaturnExecutorsNode.getJobServersNodePath(jobName);
		if (curatorFramework.checkExists().forPath(path) == null) {
			try {
				curatorFramework.create().creatingParentsIfNeeded().forPath(path);
			} catch (KeeperException.NodeExistsException e) {// NOSONAR
				log.info("node {} already existed, so skip creation", path);
			}
		}
		int depth = 2;
		shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
		shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new JobServersTriggerShardingListener(jobName, namespaceShardingService));
	}

}
