/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.reg.zookeeper;

import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.Assert.*;

public class ZookeeperRegistryCenterTest {

	private static final String NAMESPACE = "ut-saturn";

	private int PORT = 3181;

	private TestingServer testingServer;

	@Before
	public void before() throws Exception {
		startServer();
	}

	@After
	public void after() throws Exception {
		stopServer();
	}

	@Test
	public void testZkClientConfig() throws Exception {
		// default settings
		CuratorFramework client = initZk("127.0.0.1:" + PORT, "ut-ns");
		assertEquals(20000L, client.getZookeeperClient().getConnectionTimeoutMs());
		assertEquals(20000L, client.getZookeeperClient().getZooKeeper().getSessionTimeout());
		ExponentialBackoffRetry retryPolicy = (ExponentialBackoffRetry) client.getZookeeperClient().getRetryPolicy();
		assertEquals(3, retryPolicy.getN());

		// set VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT = true
		System.setProperty("VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING", "true");
		SystemEnvProperties.loadProperties();

		client = initZk("127.0.0.1:" + PORT, "ut-ns");
		assertEquals(40000L, client.getZookeeperClient().getConnectionTimeoutMs());
		assertEquals(40000L, client.getZookeeperClient().getZooKeeper().getSessionTimeout());
		retryPolicy = (ExponentialBackoffRetry) client.getZookeeperClient().getRetryPolicy();
		assertEquals(9, retryPolicy.getN());
	}

	private CuratorFramework initZk(String serverLists, String namespace) {
		ZookeeperConfiguration zkConfig = new ZookeeperConfiguration(serverLists, namespace, 1000, 3000);
		ZookeeperRegistryCenter regCenter = new ZookeeperRegistryCenter(zkConfig);
		regCenter.init();

		return (CuratorFramework) regCenter.getRawClient();
	}

	public void startServer() throws Exception {
		try (ServerSocket socket = new ServerSocket(0);) {
			PORT = socket.getLocalPort();
		} catch (IOException e) {
		}
		System.err.println("zkTestServer starting. Port: " + PORT);
		testingServer = new TestingServer(PORT);
	}

	public void stopServer() throws IOException {
		if (testingServer != null) {
			testingServer.stop();
		}
	}

}