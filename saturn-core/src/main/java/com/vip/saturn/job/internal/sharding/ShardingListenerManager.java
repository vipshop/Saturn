package com.vip.saturn.job.internal.sharding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private boolean isShutdown;
	
	private CuratorWatcher necessaryWatcher;
	
	private ShardingService shardingService;
	
	private ExecutorService executorService;

	public ShardingListenerManager(final JobScheduler jobScheduler) {
        super(jobScheduler);
        necessaryWatcher = new NecessaryWatcher();
        shardingService = jobScheduler.getShardingService();
        executorService = Executors.newSingleThreadExecutor(new SaturnThreadFactory("saturn-sharding-necessary-watch-pool-" + jobName, false));
    }
	
	@Override
	public void start() {
        shardingService.registryNecessaryWatcher(necessaryWatcher);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		isShutdown = true;
		executorService.shutdownNow();
	}
	
	class NecessaryWatcher implements CuratorWatcher {

		@Override
		public void process(WatchedEvent event) throws Exception {
			switch (event.getType()) {
			case NodeCreated:
			case NodeDataChanged:
				doBusiness(event);
			default:
				shardingService.registryNecessaryWatcher(this);
			}
		}

		private void doBusiness(final WatchedEvent event) {
			try {
				// cannot block re-registryNecessaryWatcher, so use thread pool to do business
				if(!executorService.isShutdown()) {
					executorService.submit(new Runnable() {
						@Override
						public void run() {
							if (isShutdown) {
								return;
							}		
							if (jobScheduler == null || jobScheduler.getJob() == null) {
								return;
							}
							log.info("[{}] msg={} trigger on-resharding event, type:{}, path:{}", jobName, jobName, event.getType(), event.getPath());
							jobScheduler.getJob().onResharding();
						}
					});
				}
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}

	}

}
