package com.vip.saturn.job.internal.sharding;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.basic.JobType;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.LogUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 分片监听管理器.
 *
 * @author linzhaoming
 *
 */
public class ShardingListenerManager extends AbstractListenerManager {
	private static final Logger log = LoggerFactory.getLogger(ShardingListenerManager.class);

	private volatile boolean isShutdown;

	private CuratorWatcher necessaryWatcher;

	private ShardingService shardingService;

	private ExecutorService executorService;

	private ConnectionStateListener connectionStateListener;

	public ShardingListenerManager(final JobScheduler jobScheduler) {
		super(jobScheduler);
		shardingService = jobScheduler.getShardingService();
		// because cron/passive job do nothing in onResharding method, no need this watcher.
		JobType jobType = jobScheduler.getConfigService().getJobType();
		if (!jobType.isCron() && !jobType.isPassive()) {
			necessaryWatcher = new NecessaryWatcher();
		}
	}

	@Override
	public void start() {
		if (necessaryWatcher != null) {
			executorService = Executors.newSingleThreadExecutor(
					new SaturnThreadFactory(executorName + "-" + jobName + "-registerNecessaryWatcher", false));
			shardingService.registerNecessaryWatcher(necessaryWatcher);
			connectionStateListener = new ConnectionStateListener() {
				@Override
				public void stateChanged(CuratorFramework client, ConnectionState newState) {
					if ((newState == ConnectionState.CONNECTED) || (newState == ConnectionState.RECONNECTED)) {
						// maybe node data have changed, so doBusiness whatever, it's okay for MSG job
						LogUtils.info(log, jobName,
								"state change to {}, trigger doBusiness and register necessary watcher.", newState);
						doBusiness();
						registerNecessaryWatcher();
					}
				}
			};
			addConnectionStateListener(connectionStateListener);
		}
	}

	@Override
	public void shutdown() {
		super.shutdown();
		if (executorService != null) {
			executorService.shutdownNow();
		}
		if (connectionStateListener != null) {
			removeConnectionStateListener(connectionStateListener);
		}
		isShutdown = true;
	}

	private void registerNecessaryWatcher() {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				if (!isShutdown) {
					shardingService.registerNecessaryWatcher();
				}
			}
		});
	}

	private void doBusiness() {
		try {
			// cannot block reconnected thread or re-registerNecessaryWatcher, so use thread pool to do business,
			// and the thread pool is the same with job-tree-cache's
			zkCacheManager.getExecutorService().execute(new Runnable() {
				@Override
				public void run() {
					try {
						if (isShutdown) {
							return;
						}
						if (jobScheduler == null || jobScheduler.getJob() == null) {
							return;
						}
						LogUtils.info(log, jobName, "{} trigger on-resharding", jobName);
						jobScheduler.getJob().onResharding();
					} catch (Throwable t) {
						LogUtils.error(log, jobName, "Exception throws during resharding", t);
					}
				}
			});
		} catch (Throwable t) {
			LogUtils.error(log, jobName, "Exception throws during execute thread", t);
		}
	}

	class NecessaryWatcher implements CuratorWatcher {

		@Override
		public void process(WatchedEvent event) throws Exception {
			if (isShutdown) {
				return;
			}
			switch (event.getType()) {
				case NodeCreated:
				case NodeDataChanged: // NOSONAR
					LogUtils.info(log, jobName, "event type:{}, path:{}", event.getType(), event.getPath());
					doBusiness();
				default:
					// use the thread pool to executor registerNecessaryWatcher by async,
					// fix the problem:
					// when zk is reconnected, this watcher thread is earlier than the notice of RECONNECTED EVENT,
					// registerNecessaryWatcher will wait until reconnected or timeout,
					// the drawback is that this watcher thread will block the notice of RECONNECTED EVENT.
					registerNecessaryWatcher();
			}
		}

	}

}
