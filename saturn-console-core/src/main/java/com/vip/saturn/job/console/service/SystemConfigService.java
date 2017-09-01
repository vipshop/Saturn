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

	Integer getIntegerValue(String property, int defaultValue);

	boolean getBooleanValue(String property, boolean defaultValue);

	List<SystemConfig> getSystemConfigsDirectly(List<String> properties) throws SaturnJobConsoleException;

	String getPropertiesCached() throws SaturnJobConsoleException;

	Integer insertOrUpdate(SystemConfig systemConfig) throws SaturnJobConsoleException;

}
