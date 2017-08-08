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

	@Transactional
	@Override
	public List<SystemConfig> selectByPropertiesAndLastly(List<String> properties) {
		return systemConfigRepository.selectByPropertiesAndLastly(properties);
	}

	@Override
	public List<SystemConfig> selectByLastly() {
		return systemConfigRepository.selectByLastly();
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
