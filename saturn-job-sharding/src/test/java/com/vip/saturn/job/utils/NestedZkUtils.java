/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
