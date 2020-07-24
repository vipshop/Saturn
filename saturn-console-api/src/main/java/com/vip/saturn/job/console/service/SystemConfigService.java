/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
