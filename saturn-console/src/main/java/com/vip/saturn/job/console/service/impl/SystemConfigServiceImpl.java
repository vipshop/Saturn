package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.mybatis.service.SystemConfig4SqlService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xiaopeng.he
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

	private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);

	@Autowired
	private SystemConfig4SqlService systemConfig4SqlService;

	private final ConcurrentHashMap<String, String> systemConfigCache = new ConcurrentHashMap<>();

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
	public Integer getIntegerValue(String property, int defaultValue) {
		String strValue = getValue(property);
		if (strValue == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(strValue.trim());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return defaultValue;
		}
	}

	@Override
	public boolean getBooleanValue(String property, boolean defaultValue) {
		String strValue = getValue(property);
		if (strValue == null) {
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
	public List<SystemConfig> getSystemConfigsDirectly(List<String> properties) throws SaturnJobConsoleException {
		return properties != null && !properties.isEmpty()
				? systemConfig4SqlService.selectByPropertiesAndLastly(properties)
				: systemConfig4SqlService.selectByLastly();
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
		if (systemConfigs != null && systemConfigs.size() == 1) {
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

	private void updateCacheIfNeed(SystemConfig systemConfig, int result) {
		if (result > 0) {
			systemConfigCache.put(systemConfig.getProperty(), systemConfig.getValue());
		}
	}
}
