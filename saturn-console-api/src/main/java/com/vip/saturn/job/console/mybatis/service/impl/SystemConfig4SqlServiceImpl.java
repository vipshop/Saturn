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

package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.mybatis.repository.SystemConfigRepository;
import com.vip.saturn.job.console.mybatis.service.SystemConfig4SqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author xiaopeng.he
 */
@Service
public class SystemConfig4SqlServiceImpl implements SystemConfig4SqlService {

	@Autowired
	private SystemConfigRepository systemConfigRepository;

	@Override
	public List<SystemConfig> selectAllConfig() {
		return systemConfigRepository.selectAllConfig();
	}

	@Override
	public List<SystemConfig> selectByProperty(String property) {
		return systemConfigRepository.selectByProperty(property);
	}

	@Override
	public List<SystemConfig> selectByPropertiesAndLastly(List<String> properties) {
		return systemConfigRepository.selectByPropertiesAndLastly(properties);
	}

	@Override
	public List<SystemConfig> selectByLastly() {
		return systemConfigRepository.selectByLastly();
	}

	@Override
	public List<SystemConfig> selectByPropertyPrefix(String prefix) {
		return systemConfigRepository.selectByPropertyPrefix(prefix);
	}

	@Transactional
	@Override
	public Integer insert(SystemConfig systemConfig) {
		return systemConfigRepository.insert(systemConfig);
	}

	@Transactional
	@Override
	public Integer updateById(SystemConfig systemConfig) {
		return systemConfigRepository.updateById(systemConfig);
	}

}
