package com.vip.saturn.it.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.KillSession;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author dylan.xue
 */
public class NestedZkUtils {
	static Logger log = LoggerFactory.getLogger(NestedZkUtils.class);

	private TestingServer testingServer;

	public int PORT = 3181;

	public void startServer() throws Exception {
		try (ServerSocket socket = new ServerSocket(0);) {
			PORT = socket.getLocalPort();
		} catch (IOException e) {
		}
		System.err.println("zkTestServer starting. Port: " + PORT);
		log.error("zk starts at: {}", PORT);
		testingServer = new TestingServer(PORT);
	}

	public void stopServer() throws IOException {
		if (testingServer != null) {
			testingServer.stop();
		}
	}

	public void startStoppedServer() throws Exception {
		testingServer.start();
	}

	public void reStartServer() throws Exception {
		if (testingServer != null) {
			testingServer.restart();
		}
	}

	public boolean isStarted() {
		return testingServer != null;
	}

	public String getZkString() {
		return "127.0.0.1:" + PORT;
	}

	public void killSession(ZooKeeper client) throws Exception {
		KillSession.kill(client, getZkString());
	}

	public CuratorFramework createClient(String namespace) throws InterruptedException {
		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
		CuratorFramework curatorFramework = builder.connectString("127.0.0.1:" + PORT).sessionTimeoutMs(600 * 1000) // long
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
