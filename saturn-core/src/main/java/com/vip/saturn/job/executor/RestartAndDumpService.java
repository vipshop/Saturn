package com.vip.saturn.job.executor;

import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author hebelala
 */
public class RestartAndDumpService {

    private static final Logger log = LoggerFactory.getLogger(RestartAndDumpService.class);

    private String executorName;
    private CoordinatorRegistryCenter coordinatorRegistryCenter;
    private CuratorFramework curatorFramework;
    private File prgDir;
    private NodeCache restartNC;
    private ExecutorService restartES;
    private NodeCache dumpNC;
    private ExecutorService dumpES;

    public RestartAndDumpService(String executorName, CoordinatorRegistryCenter coordinatorRegistryCenter) {
        this.executorName = executorName;
        this.coordinatorRegistryCenter = coordinatorRegistryCenter;
        this.curatorFramework = (CuratorFramework) coordinatorRegistryCenter.getRawClient();
    }

    public void start() throws Exception {
        if (!SystemEnvProperties.VIP_SATURN_ENABLE_EXEC_SCRIPT) {
            log.info("The RestartAndDumpService is disabled");
            return;
        }
        validateFile(SystemEnvProperties.NAME_VIP_SATURN_PRG, SystemEnvProperties.VIP_SATURN_PRG);
        validateConfigured(SystemEnvProperties.NAME_VIP_SATURN_LOG_OUTFILE, SystemEnvProperties.VIP_SATURN_LOG_OUTFILE);
        prgDir = new File(SystemEnvProperties.NAME_VIP_SATURN_PRG).getParentFile();

        initRestart();
        initDump();
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
        log.info("The {} is configured as {}", name, value);
    }

    private void initRestart() throws Exception {
        restartES = Executors.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-restart-watcher-thread", false));
        // Remove the restart node, before add watcher that watches the create and update event
        String nodePath = SaturnExecutorsNode.EXECUTORS_ROOT + "/" + executorName + "/restart";
        coordinatorRegistryCenter.remove(nodePath);
        restartNC = new NodeCache(curatorFramework, nodePath);
        restartNC.getListenable().addListener(new NodeCacheListener() {

            @Override
            public void nodeChanged() throws Exception {
                // Watch create, update event
                if (restartNC.getCurrentData() != null) {
                    log.info("The executor {} restart event is received", executorName);
                    restartES.execute(new Runnable() {
                        @Override
                        public void run() {
                            executeRestartOrDumpCmd("restart");
                        }
                    });
                }
            }

        });
        // Start, with not buildInitial.
        // The initial data is null, so the create event will be triggered firstly.
        restartNC.start(false);
    }

    private void initDump() throws Exception {
        dumpES = Executors.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-dump-watcher-thread", false));
        final String nodePath = SaturnExecutorsNode.EXECUTORS_ROOT + "/" + executorName + "/dump";
        coordinatorRegistryCenter.remove(nodePath);
        dumpNC = new NodeCache(curatorFramework, nodePath);
        dumpNC.getListenable().addListener(new NodeCacheListener() {

            @Override
            public void nodeChanged() throws Exception {
                // Watch create, update event
                if (dumpNC.getCurrentData() != null) {
                    log.info("The executor {} dump event is received", executorName);
                    dumpES.execute(new Runnable() {
                        @Override
                        public void run() {
                            executeRestartOrDumpCmd("dump");
                            coordinatorRegistryCenter.remove(nodePath);
                        }
                    });
                }
            }

        });
        dumpNC.start(false);
    }

    // The apache's Executor maybe destroy process on some conditions,
    // and don't provide the api for redirect process's streams to file.
    // It's not expected, so I use the original way.
    private void executeRestartOrDumpCmd(String cmd) {
        try {
            log.info("Begin to execute {} script", cmd);
            String command = "chmod +x " + SystemEnvProperties.VIP_SATURN_PRG + ";" + SystemEnvProperties.VIP_SATURN_PRG
                    + " " + cmd;
            Process process = new ProcessBuilder()
                    .command("/bin/bash", "-c", command)
                    .directory(prgDir)
                    .redirectOutput(
                            ProcessBuilder.Redirect.appendTo(new File(SystemEnvProperties.VIP_SATURN_LOG_OUTFILE)))
                    .redirectError(
                            ProcessBuilder.Redirect.appendTo(new File(SystemEnvProperties.VIP_SATURN_LOG_OUTFILE)))
                    .start();
            int exit = process.waitFor();
            log.info("Executed {} script, the exit value {} is returned", cmd, exit);
        } catch (InterruptedException e) {
            log.warn("{} thread is interrupted", cmd);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Execute {} script error", cmd, e);
        }
    }

    public void stop() {
        closeNodeCacheQuietly(restartNC);
        if (restartES != null) {
            restartES.shutdownNow();
        }
        closeNodeCacheQuietly(dumpNC);
        if (dumpES != null) {
            dumpES.shutdownNow();
        }
    }


    private void closeNodeCacheQuietly(NodeCache nodeCache) {
        try {
            if (nodeCache != null) {
                nodeCache.close();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
