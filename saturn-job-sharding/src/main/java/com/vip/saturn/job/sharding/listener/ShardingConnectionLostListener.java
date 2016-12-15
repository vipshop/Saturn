package com.vip.saturn.job.sharding.listener;

import com.vip.saturn.job.sharding.NamespaceShardingManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hebelala
 */
public abstract class ShardingConnectionLostListener implements ConnectionStateListener {

    private static final Logger logger = LoggerFactory.getLogger(ShardingConnectionLostListener.class);

    private NamespaceShardingManager namespaceShardingManager;
    private ExecutorService executor;

    public ShardingConnectionLostListener(NamespaceShardingManager namespaceShardingManager) {
        this.namespaceShardingManager = namespaceShardingManager;
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, ShardingConnectionLostListener.this.namespaceShardingManager.getNamespace() + "-NamespaceShardingManager-zk-reconnect-thread");
                if (t.isDaemon()) {
                    t.setDaemon(false);
                }
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY);
                }
                return t;
            }
        });
    }

    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);

    private long getSessionId(CuratorFramework client) {
        long sessionId;
        try {
            sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
        } catch (Exception e) {// NOSONAR
            return -1;
        }
        return sessionId;
    }

    public abstract void stop();

    public abstract void restart();

    @Override
    public void stateChanged(final CuratorFramework client, final ConnectionState newState) {
        // 使用single thread executor严格保证ZK事件执行的顺序性，避免并发性问题
        if (ConnectionState.SUSPENDED == newState) {
            connected.set(false);
            final long sessionId = getSessionId(client);
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    do {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        if (namespaceShardingManager.isShutdown()) {
                            return;
                        }
                        long newSessionId = getSessionId(client);
                        if (sessionId != newSessionId) {
                            logger.info(" {}-NamespaceShardingManager is going to stop for zk lost", namespaceShardingManager.getNamespace());
                            stop();
                            stopped.set(true);
                            return;
                        }
                    } while (!namespaceShardingManager.isShutdown() && !connected.get());
                }
            });

        } else if (ConnectionState.RECONNECTED == newState) {
            connected.set(true);
            executor.submit(new Runnable() {
                @Override
                public void run() {

                    if (stopped.compareAndSet(true, false)) {
                        logger.info(" {}-NamespaceShardingManager is going to restart for zk reconnected", namespaceShardingManager.getNamespace());
                        restart();
                    }

                }

            });
        }
    }

}