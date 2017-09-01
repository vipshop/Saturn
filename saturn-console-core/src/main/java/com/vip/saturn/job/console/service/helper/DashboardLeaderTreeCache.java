package com.vip.saturn.job.console.service.helper;

import com.vip.saturn.job.console.utils.ConsoleThreadFactory;
import com.vip.saturn.job.console.utils.LocalHostService;
import com.vip.saturn.job.console.utils.SaturnSelfNodePath;
import com.vip.saturn.job.sharding.listener.AbstractConnectionListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author hebelala
 */
public class DashboardLeaderTreeCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(DashboardLeaderTreeCache.class);

	private final String hostValue = LocalHostService.cachedIpAddress + "-" + UUID.randomUUID().toString();

	private String zkAlias;
	private CuratorFramework curatorFramework;
	private NodeCache nodeCache;
	private ExecutorService executorService;
	private DashboardLeaderConnectionListener dashboardLeaderConnectionListener;

	public DashboardLeaderTreeCache(String zkAlias, CuratorFramework curatorFramework) {
		this.zkAlias = zkAlias;
		this.curatorFramework = curatorFramework;
	}

	public void start() throws Exception {
		dashboardLeaderConnectionListener = new DashboardLeaderConnectionListener(
				"connectionListener-for-dashboardLeaderTreeCache-" + zkAlias);
		curatorFramework.getConnectionStateListenable().addListener(dashboardLeaderConnectionListener);
		createNodeCache();
		electLeaderIfNecessary();
	}

	private void createNodeCache() throws Exception {
		executorService = Executors.newSingleThreadExecutor(
				new ConsoleThreadFactory("nodeCache-for-dashboardLeaderHost-" + zkAlias, false));
		nodeCache = new NodeCache(curatorFramework, SaturnSelfNodePath.SATURN_CONSOLE_DASHBOARD_LEADER_HOST);
		nodeCache.start();
		nodeCache.getListenable().addListener(new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				electLeaderIfNecessary();
			}
		}, executorService);
	}

	private void electLeaderIfNecessary() throws Exception {
		if (!hasLeader()) {
			electLeader();
		}
	}

	private boolean hasLeader() throws Exception {
		if (!curatorFramework.getZookeeperClient().isConnected()) {
			return false;
		}
		Stat stat = curatorFramework.checkExists().forPath(SaturnSelfNodePath.SATURN_CONSOLE_DASHBOARD_LEADER_HOST);
		return stat != null;
	}

	private void electLeader() throws Exception {
		LeaderLatch leaderLatch = null;
		try {
			if (!curatorFramework.getZookeeperClient().isConnected()) {
				return;
			}
			leaderLatch = new LeaderLatch(curatorFramework, SaturnSelfNodePath.SATURN_CONSOLE_DASHBOARD_LEADER_LATCH);
			leaderLatch.start();
			int timeoutSeconds = 60;
			if (leaderLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
				if (!hasLeader()) {
					curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(
							SaturnSelfNodePath.SATURN_CONSOLE_DASHBOARD_LEADER_HOST, hostValue.getBytes("UTF-8"));
				}
			} else {
				LOGGER.error("Try to elect dashboard leader timeout({}s), zkCluster zkAlias is {}", timeoutSeconds,
						zkAlias);
			}
		} finally {
			if (leaderLatch != null) {
				try {
					leaderLatch.close();
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
	}

	private void releaseLeader() {
		try {
			if (!curatorFramework.getZookeeperClient().isConnected()) {
				return;
			}
			if (curatorFramework.checkExists()
					.forPath(SaturnSelfNodePath.SATURN_CONSOLE_DASHBOARD_LEADER_HOST) != null) {
				byte[] bytes = curatorFramework.getData()
						.forPath(SaturnSelfNodePath.SATURN_CONSOLE_DASHBOARD_LEADER_HOST);
				if (bytes != null) {
					String data = new String(bytes, "UTF-8");
					if (data.equals(hostValue)) {
						curatorFramework.delete().guaranteed().deletingChildrenIfNeeded()
								.forPath(SaturnSelfNodePath.SATURN_CONSOLE_DASHBOARD_LEADER_HOST);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private void removeAndShutdownConnectionListener() {
		try {
			if (dashboardLeaderConnectionListener != null) {
				curatorFramework.getConnectionStateListenable().removeListener(dashboardLeaderConnectionListener);
				dashboardLeaderConnectionListener.shutdownNowUntilTerminated();
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private void closeNodeCache() {
		try {
			if (nodeCache != null) {
				nodeCache.close();
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		if (executorService != null) {
			executorService.shutdownNow();
		}
	}

	public void shutdown() {
		removeAndShutdownConnectionListener();
		closeNodeCache();
		releaseLeader();
	}

	public void shutdownWithCurator() {
		shutdown();
		curatorFramework.close();
	}

	public boolean isLeader() {
		try {
			electLeaderIfNecessary();
			if (!curatorFramework.getZookeeperClient().isConnected()) {
				return false;
			}
			byte[] bytes = curatorFramework.getData().forPath(SaturnSelfNodePath.SATURN_CONSOLE_DASHBOARD_LEADER_HOST);
			if (bytes != null) {
				String data = new String(bytes, "UTF-8");
				return data.equals(hostValue);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return false;
	}

	class DashboardLeaderConnectionListener extends AbstractConnectionListener {

		public DashboardLeaderConnectionListener(String threadName) {
			super(threadName);
		}

		@Override
		public void stop() {
			closeNodeCache();
		}

		@Override
		public void restart() {
			try {
				createNodeCache();
				electLeaderIfNecessary();
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
}
