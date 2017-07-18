package com.vip.saturn.job.internal.sharding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.AbstractSaturnJob;
import com.vip.saturn.job.basic.CrondJob;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.listener.AbstractListenerManager;
import com.vip.saturn.job.threads.SaturnThreadFactory;

/**
 * 分片监听管理器.
 *
 * @author linzhaoming
 *
 */
public class ShardingListenerManager extends AbstractListenerManager {
	static Logger log = LoggerFactory.getLogger(ShardingListenerManager.class);

	private volatile boolean isShutdown;
	
	private CuratorWatcher necessaryWatcher;
	
	private ShardingService shardingService;
	
	private ExecutorService executorService;

	public ShardingListenerManager(final JobScheduler jobScheduler) {
        super(jobScheduler);
		shardingService = jobScheduler.getShardingService();
		if(!isCrondJob(jobScheduler.getCurrentConf().getSaturnJobClass())) { // because crondJob do nothing in onResharding method, no need this watcher
			necessaryWatcher = new NecessaryWatcher();
		}
    }

	private boolean isCrondJob(Class<?> saturnJobClass) {
		if (saturnJobClass != null) {
			Class<?> superClass = saturnJobClass.getSuperclass();
			String crondJobCanonicalName = CrondJob.class.getCanonicalName();
			String abstractSaturnJobCanonicalName = AbstractSaturnJob.class.getCanonicalName();
			while (superClass != null) {
				String superClassCanonicalName = superClass.getCanonicalName();
				if (superClassCanonicalName.equals(crondJobCanonicalName)) {
					return true;
				}
				if(superClassCanonicalName.equals(abstractSaturnJobCanonicalName)) { // AbstractSaturnJob is CrondJob's parent
					return false;
				}
				superClass = superClass.getSuperclass();
			}
		}
		return false;
	}
	
	@Override
	public void start() {
		if(necessaryWatcher != null) {
			executorService = Executors.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-" + jobName + "-registerNecessaryWatcher", false));
			shardingService.registerNecessaryWatcher(necessaryWatcher);
			addConnectionStateListener(new ConnectionStateListener() {
				@Override
				public void stateChanged(CuratorFramework client, ConnectionState newState) {
					if ((newState == ConnectionState.CONNECTED) || (newState == ConnectionState.RECONNECTED)) {
						doBusinessForReconnected(); // maybe node data have changed, so doBusiness whatever, it's okay for MSG job
						registerNecessaryWatcher();
					}
				}
			});
		}
	}

	@Override
	public void shutdown() {
		super.shutdown();
		if(executorService != null) {
			executorService.shutdownNow();
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
	
	private void doBusinessForReconnected() {
		try {
			// cannot block reconnected thread, so use thread pool to do business,
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
						log.info("[{}] msg={} trigger on-resharding, because of zk RECONNECTED", jobName, jobName);
						jobScheduler.getJob().onResharding();
					} catch (Throwable t) {
						log.error(t.getMessage(), t);
					}
				}
			});
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}
	
	class NecessaryWatcher implements CuratorWatcher {

		@Override
		public void process(WatchedEvent event) throws Exception {
			switch (event.getType()) {
			case NodeCreated:
			case NodeDataChanged:
				doBusiness(event);
			default:
				// use the thread pool to executor registerNecessaryWatcher by async,
				// fix the problem:
				// when zk is reconnected, this watcher thread is earlier than the notice of RECONNECTED EVENT,
				// registerNecessaryWatcher will wait until reconnected or timeout, 
				// the bad thing is that this watcher thread will block the notice of RECONNECTED EVENT.
				registerNecessaryWatcher();
			}
		}
		
		private void doBusiness(final WatchedEvent event) {
			try {
				// cannot block re-registerNecessaryWatcher, so use thread pool to do business,
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
							log.info("[{}] msg={} trigger on-resharding event, type:{}, path:{}", jobName, jobName, event.getType(), event.getPath());
							jobScheduler.getJob().onResharding();
						} catch (Throwable t) {
							log.error(t.getMessage(), t);
						}
					}
				});
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}

	}

}
