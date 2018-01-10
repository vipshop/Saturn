package com.vip.saturn.job.console.service.cache;

import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.ConsoleThreadFactory;
import com.vip.saturn.job.console.utils.SaturnSelfNodePath;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;

public class RegistryRefreshHandler {

	private String zkClusterKey;

	private CuratorFramework curatorFramework;

	private NodeCache nodeCache;

	private ExecutorService executorService;

	private RegistryCenterService registryCenterService;

	public RegistryRefreshHandler(CuratorFramework curatorFramework, String zkClusterKey) {
		this.curatorFramework = curatorFramework;
		this.zkClusterKey = zkClusterKey;
	}

	public void start() throws Exception {
		createNodeCache();
	}

	private void createNodeCache() throws Exception {
		executorService = Executors.newSingleThreadExecutor(
				new ConsoleThreadFactory("nodeCache-for-console-regcenter-refresh", false));
		nodeCache = new NodeCache(curatorFramework, SaturnSelfNodePath.SATURN_CONSOLE_DASHBOARD_LEADER_HOST);
		nodeCache.start();
		nodeCache.getListenable().addListener(new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				registryCenterService.refreshRegCenter();
			}
		}, executorService);
	}
}
