package com.vip.saturn.job.executor;

import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author hebelala
 */
public class RestartExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartExecutorService.class);

    private String executorName;
    private CoordinatorRegistryCenter coordinatorRegistryCenter;
    private CuratorFramework curatorFramework;
    private String nodePath;
    private NodeCache nodeCache;
    private volatile Thread restartThread;
    private volatile File prgDir;

    public RestartExecutorService(String executorName, CoordinatorRegistryCenter coordinatorRegistryCenter) {
        this.executorName = executorName;
        this.coordinatorRegistryCenter = coordinatorRegistryCenter;
        this.curatorFramework = (CuratorFramework) coordinatorRegistryCenter.getRawClient();
        this.nodePath = SaturnExecutorsNode.EXECUTORS_ROOT + "/" + executorName + "/restart";
    }

    /**
     * Delete the restart node, and add the watcher for restart node created or updated
     */
    public void start() throws Exception {
        if (!SystemEnvProperties.VIP_SATURN_ENABLE_RESTART_EXECUTOR) {
            LOGGER.info("The RestartExecutorService is disabled");
            return;
        }
        validateFile(SystemEnvProperties.NAME_VIP_SATURN_PRG, SystemEnvProperties.VIP_SATURN_PRG);
        validateConfigured(SystemEnvProperties.NAME_VIP_SATURN_LOG_OUTFILE, SystemEnvProperties.VIP_SATURN_LOG_OUTFILE);
        prgDir = new File(SystemEnvProperties.NAME_VIP_SATURN_PRG).getParentFile();

        // Remove the restart node, before add watcher that watches the create and update event
        coordinatorRegistryCenter.remove(nodePath);
        nodeCache = new NodeCache(curatorFramework, nodePath);
        nodeCache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                // Watch create, update event
                if (nodeCache.getCurrentData() != null) {
                    LOGGER.info("The {} restart event is received", executorName);
                    restartOneTime();
                }
            }
        });
        // Start, with not buildInitial.
        // The initial data is null, so the create event will be triggered firstly.
        nodeCache.start(false);
    }

    private void validateFile(String name, String value) throws SaturnJobException {
        validateConfigured(name, value);
        File file = new File(value);
        if (!file.exists()) {
            throw new SaturnJobException(value + " is not existing");
        }
        if (!file.isFile()) {
            throw new SaturnJobException(value + " is not file");
        }
    }

    private void validateConfigured(String name, String value) throws SaturnJobException {
        if (StringUtils.isBlank(value)) {
            throw new SaturnJobException(name + " is not configured");
        }
        LOGGER.info("The {} is configured as {}", name, value);
    }

    private synchronized void restartOneTime() {
        if (restartThread != null) {
            LOGGER.warn("The {} restart thread is already running, cannot execute again", executorName);
            return;
        }

        restartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // The apache's Executor maybe destroy process on some conditions,
                    // and don't provide the api for redirect process's streams to file.
                    // It's not expected, so I use the original way.
                    LOGGER.info("Begin to execute restart script");
                    String command = "chmod +x " + SystemEnvProperties.VIP_SATURN_PRG + ";" + SystemEnvProperties.VIP_SATURN_PRG + " restart";
                    Process process = new ProcessBuilder()
                            .command("/bin/bash", "-c", command)
                            .directory(prgDir)
                            .redirectOutput(ProcessBuilder.Redirect.appendTo(new File(SystemEnvProperties.VIP_SATURN_LOG_OUTFILE)))
                            .redirectError(ProcessBuilder.Redirect.appendTo(new File(SystemEnvProperties.VIP_SATURN_LOG_OUTFILE)))
                            .start();
                    int exit = process.waitFor();
                    LOGGER.info("Executed restart script, the exit value {} is returned", exit);
                } catch (InterruptedException e) {
                    LOGGER.info("Restart thread is interrupted");
                } catch (Exception e) {
                    LOGGER.error("Execute restart script error", e);
                }
            }
        });
        restartThread.setDaemon(true);
        restartThread.start();
    }

    public void stop() {
        if (restartThread != null) {
            restartThread.interrupt();
            restartThread = null;
        }
        try {
            if (nodeCache != null) {
                nodeCache.close();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


}
