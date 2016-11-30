/**
 * Copyright 2016 vip.com.
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

package com.vip.saturn.job.internal.monitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.utils.SensitiveInfoUtils;

/**
 * 作业监控服务.
 * @author dylan.xue
 */
public class MonitorService {
	static Logger log = LoggerFactory.getLogger(MonitorService.class);

    private static final String DUMP_ZK_THREAD_NAME = "dump-zk-thread";

	public static final String DUMP_COMMAND = "dump";
    
    private final CoordinatorRegistryCenter coordinatorRegistryCenter;
    
    private ServerSocket serverSocket;
    
    private volatile boolean closed;
    
    private int port;
    
    public MonitorService(final CoordinatorRegistryCenter coordinatorRegistryCenter, int port) {
        this.coordinatorRegistryCenter = coordinatorRegistryCenter;
        this.port = port;
    }
    
    /**
     * 初始化作业监听服务.
     */
    public void listen() {
        try {
            log.info("msg=Saturn job: monitor service is running, the port is '{}'", port);
            openSocketForMonitor(port);
        } catch (final IOException ex) {
            log.warn(ex.getMessage(),ex);
        }
    }
    
    private void openSocketForMonitor(final int port) throws IOException {
        serverSocket = new ServerSocket(port);
        new Thread(new Runnable() {
			@Override
			public void run() {
				while (!closed) {
                    try {
                        process(serverSocket.accept());
                    } catch (final IOException ex) {
                        log.warn(ex.getMessage(),ex);
                    }
                }
			}
		}, DUMP_ZK_THREAD_NAME).start();
    }
    
    private void process(final Socket socket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            ) {
            String cmdLine = reader.readLine();
            if (null != cmdLine && cmdLine.startsWith(DUMP_COMMAND)) {
                String[] cmds = cmdLine.split(" ");
                if (cmds.length == 2) {
                	List<String> result = new ArrayList<>();
                	String jobName = cmds[1];
                    TreeCache treeCache = (TreeCache) coordinatorRegistryCenter.getRawCache(JobNodePath.getJobNameFullPath(jobName));
                    dumpDirectly(JobNodePath.getJobNameFullPath(jobName), treeCache, result);
                    outputMessage(writer, Joiner.on("\n").join(SensitiveInfoUtils.filterSenstiveIps(result)) + "\n");
                }
            }
        } catch (final IOException ex) {
            log.warn(ex.getMessage(), ex);
        }
    }
    
    private void dumpDirectly(final String path, final TreeCache treeCache, final List<String> result) {
        for (String each : coordinatorRegistryCenter.getChildrenKeys(path)) {
            String zkPath = path + "/" + each;
            String zkValue = coordinatorRegistryCenter.get(zkPath);
            ChildData treeCacheData = treeCache.getCurrentData(zkPath);
            String treeCachePath =  null == treeCacheData ? "" : treeCacheData.getPath();
            String treeCacheValue = null == treeCacheData ? "" : new String(treeCacheData.getData());
            if (zkValue.equals(treeCacheValue) && zkPath.equals(treeCachePath)) {
                result.add(Joiner.on(" | ").join(zkPath, zkValue));
            } else {
                result.add(Joiner.on(" | ").join(zkPath, zkValue, treeCachePath, treeCacheValue));
            }
            dumpDirectly(zkPath, treeCache, result);
        }
    }
    
    private void outputMessage(final BufferedWriter outputWriter, final String msg) throws IOException {
        outputWriter.append(msg);
        outputWriter.flush();
    }
    
    /**
     * 关闭作业监听服务.
     */
    public void close() {
        closed = true;
        if (null != serverSocket && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (final IOException ex) {
                log.warn(ex.getMessage(),ex);
            }
        }
    }
}
