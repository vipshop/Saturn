package com.vip.saturn.job.executor;

import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import com.vip.saturn.job.threads.SaturnThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.CloseableExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.vip.saturn.job.executor.SaturnExecutorService.WAIT_JOBCLASS_ADDED_COUNT;

/**
 * @author hebelala
 */
public class InitNewJobService {

	private static final Logger log = LoggerFactory.getLogger(InitNewJobService.class);

	private SaturnExecutorService saturnExecutorService;
	private String executorName;
	private CoordinatorRegistryCenter regCenter;

	private TreeCache treeCache;
	private ExecutorService executorService;

	private List<String> jobNames = new ArrayList<>();

	public InitNewJobService(SaturnExecutorService saturnExecutorService) {
		this.saturnExecutorService = saturnExecutorService;
		this.executorName = saturnExecutorService.getExecutorName();
		this.regCenter = saturnExecutorService.getCoordinatorRegistryCenter();
	}

	public void start() throws Exception {
		treeCache = TreeCache.newBuilder((CuratorFramework) regCenter.getRawClient(), JobNodePath.ROOT).setExecutor(
				new CloseableExecutorService(Executors
						.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-$Jobs-watcher", false)),
						true)).setMaxDepth(1).build();
		executorService = Executors
				.newSingleThreadExecutor(new SaturnThreadFactory(executorName + "-initNewJob-thread", false));
		treeCache.getListenable().addListener(new InitNewJobListener(), executorService);
		treeCache.start();
	}

	public void shutdown() {
		try {
			if (treeCache != null) {
				treeCache.close();
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
		try {
			if (executorService != null && !executorService.isTerminated()) {
				executorService.shutdownNow();
				int count = 0;
				while (!executorService.awaitTermination(50, TimeUnit.MILLISECONDS)) {
					if (++count == 4) {
						log.info("InitNewJob executorService try to shutdown now");
						count = 0;
					}
					executorService.shutdownNow();
				}
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	public boolean removeJobName(String jobName) {
		return jobNames.remove(jobName);
	}

	class InitNewJobListener implements TreeCacheListener {

		@Override
		public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
			if (event == null) {
				return;
			}

			ChildData data = event.getData();
			if (data == null) {
				return;
			}

			String path = data.getPath();
			if (path == null || path.equals(JobNodePath.ROOT)) {
				return;
			}

			TreeCacheEvent.Type type = event.getType();
			if (type == null || !type.equals(TreeCacheEvent.Type.NODE_ADDED)) {
				return;
			}

			String jobName = StringUtils.substringAfterLast(path, "/");
			String jobClassPath = JobNodePath.getNodeFullPath(jobName, ConfigurationNode.JOB_CLASS);
			// wait 5 seconds at most until jobClass created.
			for (int i = 0; i < WAIT_JOBCLASS_ADDED_COUNT; i++) {
				if (!regCenter.isExisted(jobClassPath)) {
					Thread.sleep(200L);
					continue;
				}

				log.info("new job: {} 's jobClass created event received", jobName);

				if (!jobNames.contains(jobName)) {
					if (initJobScheduler(jobName)) {
						jobNames.add(jobName);
						log.info("the job {} initialize successfully", jobName);
					} else {
						log.warn("the job {} initialize fail", jobName);
					}
				} else {
					log.warn("the job {} is unnecessary to initialize, because it's already existing", jobName);
				}
				break;
			}
		}

		private boolean initJobScheduler(String jobName) {
			try {
				log.info("[{}] msg=add new job {} - {}", jobName, executorName, jobName);
				JobConfiguration jobConfig = new JobConfiguration(regCenter, jobName);
				if (jobConfig.getSaturnJobClass() == null) {
					log.warn("[{}] msg={} - {} the saturnJobClass is null, jobType is {}", jobConfig, executorName,
							jobName, jobConfig.getJobType());
					return false;
				}
				if (jobConfig.isDeleting()) {
					log.warn("[{}] msg={} - {} the job is on deleting", jobName, executorName, jobName);
					String serverNodePath = JobNodePath.getServerNodePath(jobName, executorName);
					regCenter.remove(serverNodePath);
					return false;
				}
				JobScheduler scheduler = new JobScheduler(regCenter, jobConfig);
				scheduler.setSaturnExecutorService(saturnExecutorService);
				return scheduler.init();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return false;
			}
		}

	}

}
