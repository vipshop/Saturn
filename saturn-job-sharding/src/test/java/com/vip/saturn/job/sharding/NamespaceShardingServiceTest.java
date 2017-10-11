package com.vip.saturn.job.sharding;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.curator.framework.CuratorFramework;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.utils.NestedZkUtils;

/**
 * Created by xiaopeng.he on 2016/7/6.
 */
public class NamespaceShardingServiceTest {

	private NestedZkUtils nestedZkUtils;

	@Before
	public void setUp() throws Exception {
		nestedZkUtils = new NestedZkUtils();
		nestedZkUtils.startServer();
	}

	@After
	public void tearDown() throws IOException {
		if (nestedZkUtils != null) {
			nestedZkUtils.stopServer();
		}
	}

	@Test
	public void leadershipElectionTest() throws Exception {
		String namespace = "MyNamespace";

		CuratorFramework curatorFramework = nestedZkUtils.createClient(namespace);

		// 启动第一个
		String ip1 = "127.0.0.1";
		CuratorFramework curatorFramework1 = nestedZkUtils.createClient(namespace);
		NamespaceShardingManager namespaceShardingManager1 = new NamespaceShardingManager(curatorFramework1, namespace,
				ip1, null,null);
		namespaceShardingManager1.start();

		Thread.sleep(1000);

		// 验证leadership
		assertThat(new String(curatorFramework.getData().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH), "UTF-8"))
				.isEqualTo(ip1);

		// 启动第二个
		String ip2 = "127.0.0.2";
		CuratorFramework curatorFramework2 = nestedZkUtils.createClient(namespace);
		NamespaceShardingManager namespaceShardingManager2 = new NamespaceShardingManager(curatorFramework2, namespace,
				ip2, null,null);
		namespaceShardingManager2.start();

		Thread.sleep(1000);

		// 验证leadership
		assertThat(new String(curatorFramework.getData().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH), "UTF-8"))
				.isEqualTo(ip1);

		// 启动第三个
		String ip3 = "127.0.0.3";
		CuratorFramework curatorFramework3 = nestedZkUtils.createClient(namespace);
		NamespaceShardingManager namespaceShardingManager3 = new NamespaceShardingManager(curatorFramework3, namespace,
				ip3, null,null);
		namespaceShardingManager3.start();

		Thread.sleep(1000);

		// 验证leadership
		assertThat(new String(curatorFramework.getData().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH), "UTF-8"))
				.isEqualTo(ip1);

		// 停止第一个
		namespaceShardingManager1.stopWithCurator();

		Thread.sleep(1000);

		// 验证leadership
		assertThat(new String(curatorFramework.getData().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH), "UTF-8"))
				.isIn(ip2, ip3);

		// 停止第二个
		namespaceShardingManager2.stopWithCurator();

		Thread.sleep(1000);

		// 验证leadership
		assertThat(new String(curatorFramework.getData().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH), "UTF-8"))
				.isEqualTo(ip3);
	}

}
