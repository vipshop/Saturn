package com.vip.saturn.job.console.service.impl;

import com.google.common.collect.Lists;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.mybatis.service.SystemConfig4SqlService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xiaopeng.he
 */
public class SystemConfigServiceImpl implements SystemConfigService {

	private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);
	private final ConcurrentHashMap<String, String> systemConfigCache = new ConcurrentHashMap<>();
	@Autowired
	private SystemConfig4SqlService systemConfig4SqlService;
	private Timer timer;

	private AtomicBoolean hasGotSystemConfigData = new AtomicBoolean(false);

	@PostConstruct
	public void init() {
		loadAll();
		timer = new Timer("get-systemConfig-to-memory-timer-" + System.currentTimeMillis(), true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					loadAll();
				} catch (Throwable t) {
					log.error("get system config from db error", t);
				}
			}
		}, SaturnConstants.GET_SYS_CONFIG_DATA_REFRESH_TIME, SaturnConstants.GET_SYS_CONFIG_DATA_REFRESH_TIME);
	}

	private void loadAll() {
		try {
			log.info("begin to get system config from db");
			List<SystemConfig> systemConfigs = systemConfig4SqlService.selectByLastly();
			synchronized (systemConfigCache) {
				systemConfigCache.clear();
				if (systemConfigs != null && !systemConfigs.isEmpty()) {
					for (SystemConfig systemConfig : systemConfigs) {
						systemConfigCache.put(systemConfig.getProperty(), systemConfig.getValue());
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			log.info("end get system config from db");
			hasGotSystemConfigData.compareAndSet(false, true);
		}
	}

	@PreDestroy
	public void destroy() {
		if (timer != null) {
			timer.cancel();
		}
	}

	@Override
	public boolean hasGotSystemConfigData() throws SaturnJobConsoleException {
		return hasGotSystemConfigData.get();
	}

	@Override
	public String getValue(String property) {
		synchronized (systemConfigCache) {
			return systemConfigCache.get(property);
		}
	}

	@Override
	public String getValueDirectly(String property) {
		List<String> properties = new ArrayList<String>();
		properties.add(property);
		List<SystemConfig> systemConfigs = systemConfig4SqlService.selectByPropertiesAndLastly(properties);
		return systemConfigs == null || systemConfigs.isEmpty() ? null : systemConfigs.get(0).getValue();
	}

	@Override
	public List<String> getValuesByPrefix(String prefix) {
		synchronized (systemConfigCache) {
			List<String> result = Lists.newArrayList();

			for (Map.Entry<String, String> entry : systemConfigCache.entrySet()) {
				if (entry.getKey().startsWith(prefix)) {
					result.add(entry.getValue());
				}
			}

			return result;
		}
	}

	@Override
	public Integer getIntegerValue(String property, int defaultValue) {
		String strValue = getValue(property);
		if (StringUtils.isBlank(strValue)) {
			return defaultValue;
		}
		try {
			return Integer.valueOf(strValue.trim());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return defaultValue;
		}
	}

	@Override
	public boolean getBooleanValue(String property, boolean defaultValue) {
		String strValue = getValue(property);
		if (StringUtils.isBlank(strValue)) {
			return defaultValue;
		}
		try {
			return Boolean.parseBoolean(strValue.trim());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return defaultValue;
		}
	}

	@Override
	public List<SystemConfig> getAllSystemConfigs() {
		return systemConfig4SqlService.selectAllConfig();
	}

	@Override
	public List<SystemConfig> getSystemConfigsDirectly(List<String> properties) throws SaturnJobConsoleException {
		return properties != null && !properties.isEmpty() ?
				systemConfig4SqlService.selectByPropertiesAndLastly(properties) :
				systemConfig4SqlService.selectByLastly();
	}

	@Override
	public List<SystemConfig> getSystemConfigsByPrefix(String prefix) throws SaturnJobConsoleException {
		if (StringUtils.isBlank(prefix)) {
			return Lists.newArrayList();
		}

		return systemConfig4SqlService.selectByPropertyPrefix(prefix);
	}


	@Override
	public String getPropertiesCached() throws SaturnJobConsoleException {
		StringBuilder sb = new StringBuilder(100);
		synchronized (systemConfigCache) {
			Iterator<Map.Entry<String, String>> iterator = systemConfigCache.entrySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next().getKey();
				sb.append(key).append(',');
			}
			int lastIndex = sb.length() - 1;
			if (lastIndex > -1 && sb.charAt(lastIndex) == ',') {
				sb.deleteCharAt(lastIndex);
			}
		}
		return sb.toString();
	}

	@Override
	public Integer insertOrUpdate(SystemConfig systemConfig) throws SaturnJobConsoleException {
		List<String> properties = new ArrayList<>();
		properties.add(systemConfig.getProperty());
		List<SystemConfig> systemConfigs = systemConfig4SqlService.selectByPropertiesAndLastly(properties);
		if (systemConfigs != null && systemConfigs.size() > 0) {
			SystemConfig systemConfig1 = systemConfigs.get(0);
			if (systemConfig1 != null) {
				systemConfig1.setProperty(systemConfig.getProperty());
				systemConfig1.setValue(systemConfig.getValue());
				int result = systemConfig4SqlService.updateById(systemConfig1);
				updateCacheIfNeed(systemConfig, result);
				return result;
			}
		}
		int result = systemConfig4SqlService.insert(systemConfig);
		updateCacheIfNeed(systemConfig, result);
		return result;
	}

	@Override
	public Integer createConfig(SystemConfig systemConfig) throws SaturnJobConsoleException {
		List<String> properties = new ArrayList<>();
		properties.add(systemConfig.getProperty());
		List<SystemConfig> systemConfigs = systemConfig4SqlService.selectByProperty(systemConfig.getProperty());

		boolean found = false;
		if (systemConfigs != null) {
			for (int i = 0; i < systemConfigs.size(); i++) {
				SystemConfig config = systemConfigs.get(i);
				if (StringUtils.equals(config.getProperty(), systemConfig.getProperty())) {
					found = true;
					break;
				}
			}
		}

		if (found) {
			throw new SaturnJobConsoleException(
					String.format("systemConfig %s already existed", systemConfig.getProperty()));
		}

		int result = systemConfig4SqlService.insert(systemConfig);
		updateCacheIfNeed(systemConfig, result);
		return result;
	}

	@Override
	public Integer updateConfig(SystemConfig systemConfig) throws SaturnJobConsoleException {
		List<String> properties = new ArrayList<>();
		properties.add(systemConfig.getProperty());
		List<SystemConfig> systemConfigs = systemConfig4SqlService.selectByPropertiesAndLastly(properties);

		if (systemConfigs == null || systemConfigs.isEmpty()) {
			throw new SaturnJobConsoleException(
					String.format("systemConfig %s not existed, update fail", systemConfig.getProperty()));
		}

		SystemConfig config = systemConfigs.get(0);
		config.setProperty(systemConfig.getProperty());
		config.setValue(systemConfig.getValue());
		int result = systemConfig4SqlService.updateById(config);
		updateCacheIfNeed(systemConfig, result);
		return result;
	}

	@Override
	public void reload() {
		loadAll();
	}

	private void updateCacheIfNeed(SystemConfig systemConfig, int result) {
		if (result > 0) {
			systemConfigCache.put(systemConfig.getProperty(), systemConfig.getValue());
		}
	}
}
