package com.vip.saturn.job.sharding;

import com.vip.saturn.job.sharding.listener.AddOrRemoveJobListener;
import com.vip.saturn.job.sharding.listener.ExecutorOnlineOfflineTriggerShardingListener;
import com.vip.saturn.job.sharding.listener.LeadershipElectionListener;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.AddJobListenersService;
import com.vip.saturn.job.sharding.service.ExecutorCleanService;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import com.vip.saturn.job.sharding.service.ShardingTreeCacheService;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class NamespaceShardingManager {
	static Logger log = LoggerFactory.getLogger(NamespaceShardingManager.class);

	private NamespaceShardingService namespaceShardingService;
	private ExecutorCleanService executorCleanService;
	private CuratorFramework curatorFramework;
	private AddJobListenersService addJobListenersService;

	private ShardingTreeCacheService shardingTreeCacheService;
	
	public NamespaceShardingManager(CuratorFramework curatorFramework, String namespace, String hostValue) {
		this.curatorFramework = curatorFramework;
		this.shardingTreeCacheService = new ShardingTreeCacheService(namespace, curatorFramework);
		this.namespaceShardingService = new NamespaceShardingService(curatorFramework, hostValue);
		this.executorCleanService = new ExecutorCleanService(curatorFramework);
		this.addJobListenersService = new AddJobListenersService(namespace, curatorFramework, namespaceShardingService, shardingTreeCacheService);
	}

	/**
	 * leadership election, add listeners
	 */
	public void start() throws Exception {
		// create ephemeral node $SaturnExecutors/leader/host & $Jobs.
		namespaceShardingService.leaderElection();
		addJobListenersService.addExistJobPathListener();
		addOnlineOfflineListener();
		addLeaderElectionListener();
		addNewOrRemoveJobListener();
	}

	/**
	 *  watch 1-level-depth of the children of /$Jobs
	 */
	private void addNewOrRemoveJobListener() throws Exception {
		String path = SaturnExecutorsNode.$JOBSNODE_PATH;
		int depth = 1;
		shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
		shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new AddOrRemoveJobListener(addJobListenersService));
	}


	/**
	 *  watch 2-level-depth of the children of /$SaturnExecutors/executors
	 */
	private void addOnlineOfflineListener() throws Exception {
		String path = SaturnExecutorsNode.EXECUTORSNODE_PATH;
		int depth = 2;
		shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
		shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new ExecutorOnlineOfflineTriggerShardingListener(namespaceShardingService, executorCleanService));
	}

	/**
	 * watch 1-level-depth of the children of /$SaturnExecutors/leader  
	 */
	private void addLeaderElectionListener() throws Exception {
		String path = SaturnExecutorsNode.LEADERNODE_PATH;
		int depth = 1;
		shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
		shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new LeadershipElectionListener(namespaceShardingService));
	}


	/**
	 * close listeners, delete leadership, close curatorFramework
	 */
	public void stop() {
		shardingTreeCacheService.shutdown();
		namespaceShardingService.shutdown();
		curatorFramework.close();
	}
	
}
