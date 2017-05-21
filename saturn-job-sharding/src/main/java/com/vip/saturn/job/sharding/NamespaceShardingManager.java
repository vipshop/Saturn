package com.vip.saturn.job.sharding;

import com.vip.saturn.job.integrate.service.ReportAlarmService;
import com.vip.saturn.job.sharding.listener.*;
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

	private AtomicBoolean isStoppedFlag = new AtomicBoolean(true);

	private ShardingConnectionLostListener shardingConnectionLostListener;
	
	public NamespaceShardingManager(CuratorFramework curatorFramework, String namespace, String hostValue, ReportAlarmService reportAlarmService) {
		this.curatorFramework = curatorFramework;
		this.namespace = namespace;
		this.shardingTreeCacheService = new ShardingTreeCacheService(namespace, curatorFramework);
		this.namespaceShardingService = new NamespaceShardingService(curatorFramework, hostValue, reportAlarmService);
		this.executorCleanService = new ExecutorCleanService(curatorFramework);
		this.addJobListenersService = new AddJobListenersService(namespace, curatorFramework, namespaceShardingService, shardingTreeCacheService);
	}

	public boolean isStopped() {
		return isStoppedFlag.get();
	}

	public String getNamespace() {
		return namespace;
	}

    public CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }

    private void start0() throws Exception {
		shardingTreeCacheService.start();
		// create ephemeral node $SaturnExecutors/leader/host & $Jobs.
		namespaceShardingService.leaderElection();
		addJobListenersService.addExistJobPathListener();
		addOnlineOfflineListener();
		addExecutorShardingListener();
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
		synchronized (isStoppedFlag) {
			if (isStoppedFlag.compareAndSet(true, false)) {
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
		createNodePathIfNotExists(path);
		shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
		shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new AddOrRemoveJobListener(addJobListenersService));
	}


	/**
	 *  watch 2-level-depth of the children of /$SaturnExecutors/executors
	 */
	private void addOnlineOfflineListener() throws Exception {
		String path = SaturnExecutorsNode.EXECUTORSNODE_PATH;
		int depth = 2;
		createNodePathIfNotExists(path);
		shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
		shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new ExecutorOnlineOfflineTriggerShardingListener(namespaceShardingService, executorCleanService));
	}
	
	/**
	 *  watch 1-level-depth of the children of /$SaturnExecutors/sharding
	 */
	private void addExecutorShardingListener() throws Exception {
		String path = SaturnExecutorsNode.SHARDINGNODE_PATH;
		int depth = 1;
		createNodePathIfNotExists(path);
		shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
		shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new SaturnExecutorsShardingTriggerShardingListener(namespaceShardingService));
	}

	/**
	 * watch 1-level-depth of the children of /$SaturnExecutors/leader  
	 */
	private void addLeaderElectionListener() throws Exception {
		String path = SaturnExecutorsNode.LEADERNODE_PATH;
		int depth = 1;
		createNodePathIfNotExists(path);
		shardingTreeCacheService.addTreeCacheIfAbsent(path, depth);
		shardingTreeCacheService.addTreeCacheListenerIfAbsent(path, depth, new LeadershipElectionListener(namespaceShardingService));
	}

	private void createNodePathIfNotExists(String path) {
		try {
			if (curatorFramework.checkExists().forPath(path) == null) {
				curatorFramework.create().creatingParentsIfNeeded().forPath(path);
			}
		} catch (Exception e) { //NOSONAR
		}
	}

	/**
	 * close listeners, delete leadership
	 */
	public void stop() {
		synchronized (isStoppedFlag) {
			if (isStoppedFlag.compareAndSet(false, true)) {
				stop0();
				curatorFramework.getConnectionStateListenable().removeListener(shardingConnectionLostListener);
				shardingConnectionLostListener.shutdown();
			}
		}
	}

	public NamespaceShardingService getNamespaceShardingService() {
		return namespaceShardingService;
	}

}
