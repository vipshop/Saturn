package com.vip.saturn.job.executor;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public class ExecutorConfigService {

	private static final Logger log = LoggerFactory.getLogger(ExecutorConfigService.class);

	private static final String executorConfigPath = "/$SaturnSelf/saturn-executor/config";

	private String executorName;
	private CoordinatorRegistryCenter coordinatorRegistryCenter;
	private Class executorConfigClass;
	private NodeCache nodeCache;
	private volatile Object executorConfig;

	public ExecutorConfigService(String executorName, CoordinatorRegistryCenter coordinatorRegistryCenter,
			Class executorConfigClass) {
		this.executorName = executorName;
		this.coordinatorRegistryCenter = coordinatorRegistryCenter;
		this.executorConfigClass = executorConfigClass;
	}

	public void start() throws Exception {
		validateAndInitExecutorConfig();
		CuratorFramework curatorFramework = (CuratorFramework) this.coordinatorRegistryCenter.usingNamespace(null)
				.getRawClient();
		nodeCache = new NodeCache(curatorFramework, executorConfigPath);
		nodeCache.getListenable().addListener(new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				// Watch create, update event
				try {
					final ChildData currentData = nodeCache.getCurrentData();
					if (currentData != null) {
						String configStr = null;
						byte[] data = currentData.getData();
						if (data != null) {
							configStr = new String(data, "UTF-8");
						}
						log.info("The path {} created or updated event is received by {}, the data is {}",
								executorConfigPath, executorName, configStr);
						if (configStr == null) {
							executorConfig = executorConfigClass.newInstance();
						} else {
							executorConfig = JSON.parseObject(configStr, executorConfigClass);
						}
					}
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}

		});
		nodeCache.start(false);
	}

	private void validateAndInitExecutorConfig() throws Exception {
		if (executorConfigClass == null) {
			throw new Exception("executorConfigClass cannot be null");
		}
		Object temp = executorConfigClass.newInstance();
		if (!(temp instanceof ExecutorConfig)) {
			throw new Exception(String.format("executorConfigClass should be %s or its child",
					ExecutorConfig.class.getCanonicalName()));
		}
		executorConfig = temp;
	}

	public void stop() {
		try {
			if (nodeCache != null) {
				nodeCache.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public ExecutorConfig getExecutorConfig() {
		return (ExecutorConfig) executorConfig;
	}
}
