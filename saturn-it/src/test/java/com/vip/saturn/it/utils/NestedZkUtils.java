package com.vip.saturn.it.utils;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.KillSession;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if(testingServer != null) {
            testingServer.stop();
        }
    }

    public void startStoppedServer() throws Exception {
    	testingServer.start();
    }
    
    public void reStartServer() throws Exception {
        if(testingServer != null) {
            testingServer.restart();
        }
    }

    public boolean isStarted() {
        return testingServer != null;
    }

    public String getZkString() {
        return "localhost:" + PORT;
    }

    public void killSession(ZooKeeper client) throws Exception {
        KillSession.kill(client, getZkString());
    }

    public CuratorFramework createClient(String namespace) throws InterruptedException {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        CuratorFramework curatorFramework = builder.connectString("localhost:" + PORT)
                .sessionTimeoutMs(600 * 1000) // long long, could to debug
                .retryPolicy(new RetryNTimes(3, 1000))
                .namespace(namespace)
                .build();
        curatorFramework.start();
        curatorFramework.blockUntilConnected();
        return curatorFramework;
    }

    public static void main(String[] args) throws Exception {
        NestedZkUtils nestedZkUtils = new NestedZkUtils();
        nestedZkUtils.startServer();
        CuratorFramework client = nestedZkUtils.createClient("abc");
        Thread.sleep(1 * 1000);
        KillSession.kill(client.getZookeeperClient().getZooKeeper(), nestedZkUtils.getZkString());
        Thread.sleep(10 * 1000);
    }
}
