package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;

import java.util.List;

/**
 * @author hebelala
 */
public interface SystemConfigService {

	boolean hasGotSystemConfigData() throws SaturnJobConsoleException;

	String getValue(String property);

	String getValueDirectly(String property);

	List<String> getValuesByPrefix(String prefix);

	Integer getIntegerValue(String property, int defaultValue);

	boolean getBooleanValue(String property, boolean defaultValue);

	List<SystemConfig> getAllSystemConfigs();

	List<SystemConfig> getSystemConfigsDirectly(List<String> properties) throws SaturnJobConsoleException;

	List<SystemConfig> getSystemConfigsByPrefix(String prefix) throws SaturnJobConsoleException;

	String getPropertiesCached() throws SaturnJobConsoleException;

	Integer insertOrUpdate(SystemConfig systemConfig) throws SaturnJobConsoleException;

	Integer createConfig(SystemConfig systemConfig) throws SaturnJobConsoleException;

	Integer updateConfig(SystemConfig systemConfig) throws SaturnJobConsoleException;

	void reload();

}
