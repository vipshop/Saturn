package com.vip.saturn.job.sharding;

import com.vip.saturn.job.sharding.listener.AddOrRemoveJobListener;
import com.vip.saturn.job.sharding.listener.ExecutorOnlineOfflineTriggerShardingListener;
import com.vip.saturn.job.sharding.listener.LeadershipElectionListener;
import com.vip.saturn.job.sharding.listener.ShardingConnectionLostListener;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.sharding.service.AddJobListenersService;
import com.vip.saturn.job.sharding.service.ExecutorCleanService;
import com.vip.saturn.job.sharding.service.NamespaceShardingService;
import com.vip.saturn.job.sharding.service.ShardingTreeCacheService;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 * @author hebelala
 *
 */
public class NamespaceShardingManager {
	static Logger log = LoggerFactory.getLogger(NamespaceShardingManager.class);

	private NamespaceShardingService namespaceShardingService;
	private ExecutorCleanService executorCleanService;
	private CuratorFramework curatorFramework;
	private AddJobListenersService addJobListenersService;

	private String namespace;

	private ShardingTreeCacheService shardingTreeCacheService;

	private AtomicBoolean isStopped = new AtomicBoolean(true);

	private ShardingConnectionLostListener shardingConnectionLostListener;
	
	public NamespaceShardingManager(CuratorFramework curatorFramework, String namespace, String hostValue) {
		this.curatorFramework = curatorFramework;
		this.namespace = namespace;
		this.shardingTreeCacheService = new ShardingTreeCacheService(namespace, curatorFramework);
		this.namespaceShardingService = new NamespaceShardingService(curatorFramework, hostValue);
		this.executorCleanService = new ExecutorCleanService(curatorFramework);
		this.addJobListenersService = new AddJobListenersService(namespace, curatorFramework, namespaceShardingService, shardingTreeCacheService);
	}

	public boolean isStopped() {
		return isStopped.get();
	}

	public String getNamespace() {
		return namespace;
	}

	private void start0() throws Exception {
		// create ephemeral node $SaturnExecutors/leader/host & $Jobs.
		namespaceShardingService.leaderElection();
		addJobListenersService.addExistJobPathListener();
		addOnlineOfflineListener();
		addLeaderElectionListener();
		addNewOrRemoveJobListener();
	}

	private void stop0() {
		shardingTreeCacheService.shutdown();
		namespaceShardingService.shutdown();
	}

	/**
	 * leadership election, add listeners
	 */
	public void start() throws Exception {
		synchronized (isStopped) {
			if (isStopped.compareAndSet(true, false)) {
				start0();
				addConnectionLostListener();
			}
		}
	}

	private void addConnectionLostListener() {
		shardingConnectionLostListener = new ShardingConnectionLostListener(this) {
			@Override
			public void stop() {
				stop0();
			}

			@Override
			public void restart() {
				try {
					start0();
				} catch (Exception e) {
					log.error("restart " + namespace + "-NamespaceShardingManager error", e);
				}
			}
		};
		curatorFramework.getConnectionStateListenable().addListener(shardingConnectionLostListener);
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
	 * close listeners, delete leadership
	 */
	public void stop() {
		synchronized (isStopped) {
			if (isStopped.compareAndSet(false, true)) {
				stop0();
				curatorFramework.getConnectionStateListenable().removeListener(shardingConnectionLostListener);
				shardingConnectionLostListener.shutdown();
			}
		}
	}
	
}
