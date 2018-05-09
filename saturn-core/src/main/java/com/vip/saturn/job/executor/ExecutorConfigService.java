package com.vip.saturn.job.executor;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.exception.SaturnExecutorException;
import org.apache.commons.lang3.StringUtils;
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

	private static final String EXECUTOR_CONFIG_PATH = "/$SaturnSelf/saturn-executor/config";

	private String executorName;

	private CuratorFramework curatorFramework;

	private Class executorConfigClass;

	private NodeCache nodeCache;

	private volatile Object executorConfig;

	public ExecutorConfigService(String executorName, CuratorFramework curatorFramework, Class executorConfigClass) {
		this.executorName = executorName;
		this.curatorFramework = curatorFramework;
		this.executorConfigClass = executorConfigClass;
	}

	public void start() throws Exception {
		validateAndInitExecutorConfig();
		nodeCache = new NodeCache(curatorFramework.usingNamespace(null), EXECUTOR_CONFIG_PATH);
		nodeCache.getListenable().addListener(new NodeCacheListener() {
			@Override
			public void nodeChanged() throws Exception {
				// Watch create, update event
				try {
					final ChildData currentData = nodeCache.getCurrentData();
					if (currentData == null) {
						return;
					}

					String configStr = null;
					byte[] data = currentData.getData();
					if (data != null && data.length > 0) {
						configStr = new String(data, "UTF-8");
					}

					log.info("The path {} created or updated event is received by {}, the data is {}",
							EXECUTOR_CONFIG_PATH, executorName, configStr);
					if (StringUtils.isBlank(configStr)) {
						executorConfig = executorConfigClass.newInstance();
					} else {
						executorConfig = JSON.parseObject(configStr, executorConfigClass);
					}
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}

		});
		nodeCache.start(false);
	}

	private void validateAndInitExecutorConfig() throws Exception {
		if (curatorFramework == null) {
			throw new SaturnExecutorException("curatorFramework cannot be null");
		}

		if (executorConfigClass == null) {
			throw new SaturnExecutorException("executorConfigClass cannot be null");
		}
		Object temp = executorConfigClass.newInstance();
		if (!(temp instanceof ExecutorConfig)) {
			throw new SaturnExecutorException(String.format("executorConfigClass should be %s or its child",
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
