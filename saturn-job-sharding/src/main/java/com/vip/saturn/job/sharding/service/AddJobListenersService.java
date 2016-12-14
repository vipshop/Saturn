/**
 *
 */
package com.vip.saturn.job.sharding.service;

import com.vip.saturn.job.sharding.listener.JobConfigTriggerShardingListener;
import com.vip.saturn.job.sharding.listener.JobServersTriggerShardingListener;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author chembo.huang
 */
public class AddJobListenersService {
	static Logger log = LoggerFactory.getLogger(AddJobListenersService.class);

	private CuratorFramework curatorFramework;
	private NamespaceShardingService namespaceShardingService;
	private String namespace;
	private ShardingTreeCacheService shardingTreeCacheService;

	public AddJobListenersService(String namespace, CuratorFramework curatorFramework, NamespaceShardingService namespaceShardingService, ShardingTreeCacheService shardingTreeCacheService) {
		this.curatorFramework = curatorFramework;
		this.namespaceShardingService = namespaceShardingService;
		this.namespace = namespace;
		this.shardingTreeCacheService = shardingTreeCacheService;
	}

	public void addExistJobPathListener() throws Exception {
		if (null != curatorFramework.checkExists().forPath(SaturnExecutorsNode.$JOBSNODE_PATH)) {
			List<String> jobs = curatorFramework.getChildren().forPath(SaturnExecutorsNode.$JOBSNODE_PATH);
			log.info("namespaceSharding: addExistJobPathListener, jobs = {}", jobs);
			if (jobs != null) {
				for (String job : jobs) {
					addJobPathListener(job);
				}
			}
		}
	}

	public void addJobPathListener(String jobName) {
		addJobConfigPathListener(jobName);
		addJobServersPathListener(jobName);
	}

	public void removeJobPathTreeCache(String jobName) {
		removeJobConfigPathTreeCache(jobName);
		removeJobServersPathTreeCache(jobName);
	}

	private void removeJobConfigPathTreeCache(String jobName) {
		String path = SaturnExecutorsNode.$JOBSNODE_PATH + "/" + jobName + "/config";
		int depth = 1;
		shardingTreeCacheService.removeTreeCache(path, depth);
	}

	private void removeJobServersPathTreeCache(String jobName) {
		String path = SaturnExecutorsNode.$JOBSNODE_PATH + "/" + jobName + "/servers";
		int depth = 2;
		shardingTreeCacheService.removeTreeCache(path, depth);
	}

	private void addJobConfigPathListener(String jobName) {
		try {
			String path = SaturnExecutorsNode.$JOBSNODE_PATH + "/" + jobName + "/config";
			int depth = 1;
			String fullPath = namespace + path;

			int waitConfigPathCreatedCounts = 50;
			while (waitConfigPathCreatedCounts-- != 0) {
				if (null != curatorFramework.checkExists().forPath(path)) {
					break;
				}
				if (waitConfigPathCreatedCounts == 0) {
					log.error("create TreeCache failed, the path does not exists, full path is {}, depth is {}", fullPath, depth);
					return;
				}
				Thread.sleep(100);
			}

			shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
			shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new JobConfigTriggerShardingListener(jobName, namespaceShardingService));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void addJobServersPathListener(String jobName) {
		try {
			String path = SaturnExecutorsNode.$JOBSNODE_PATH + "/" + jobName + "/servers";
			int depth = 2;
			try {
				// create servers if not exists
				if (curatorFramework.checkExists().forPath(path) == null) {
					curatorFramework.create().forPath(path);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
			shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new JobServersTriggerShardingListener(jobName, namespaceShardingService));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
