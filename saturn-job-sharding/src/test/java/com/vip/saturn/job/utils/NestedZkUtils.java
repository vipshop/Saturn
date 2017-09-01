package com.vip.saturn.job.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by xiaopeng.he on 2016/7/8.
 */
public class NestedZkUtils {

	private TestingServer testingServer;

	private int port;

	public void startServer() throws Exception {
		try (ServerSocket socket = new ServerSocket(0);) {
			port = socket.getLocalPort();
		} catch (IOException e) {
		}
		testingServer = new TestingServer(port);
	}

	public void stopServer() throws IOException {
		if (testingServer != null) {
			testingServer.close();
		}
	}

	public CuratorFramework createClient(String namespace) throws InterruptedException {
		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
		CuratorFramework curatorFramework = builder.connectString("127.0.0.1:" + port).sessionTimeoutMs(600 * 1000) // long
																													// long,
																													// could
																													// to
																													// debug
				.retryPolicy(new RetryNTimes(3, 1000)).namespace(namespace).build();
		curatorFramework.start();
		curatorFramework.blockUntilConnected();
		return curatorFramework;
	}
}
