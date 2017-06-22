package com.vip.saturn.job.console.mybatis.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vip.saturn.job.console.mybatis.entity.RegistryCenterConfig;
import com.vip.saturn.job.console.mybatis.repository.RegistryCenterConfigRepository;
import com.vip.saturn.job.console.mybatis.service.RegistryCenterConfigService;

/**
 * 
 * @author hebelala
 *
 */
@Service
public class RegistryCenterConfigServiceImpl implements RegistryCenterConfigService {

	@Autowired
	private RegistryCenterConfigRepository registryCenterConfigRepository;

	@Transactional(readOnly = true)
	@Override
	public String getConnectString(String namespace) {
		RegistryCenterConfig registryCenterConfig = registryCenterConfigRepository.selectByNamespace(namespace);
		return registryCenterConfig != null ? registryCenterConfig.getConnectString() : null;
	}

	@Transactional(readOnly = true)
	@Override
	public List<RegistryCenterConfig> selectAll() {
		return registryCenterConfigRepository.selectAll();
	}

	@Transactional
	@Override
	public int insert(String namespace, String connectString, String createdBy) {
		RegistryCenterConfig registryCenterConfig = new RegistryCenterConfig();
		registryCenterConfig.setCreateTime(new Date());
		registryCenterConfig.setCreatedBy(createdBy);
		registryCenterConfig.setNamespace(namespace);
		registryCenterConfig.setConnectString(connectString);
		return registryCenterConfigRepository.insert(registryCenterConfig);
	}

	@Transactional
	@Override
	public int update(String namespace, String connectString, String lastUpdatedBy) {
		RegistryCenterConfig registryCenterConfig = registryCenterConfigRepository.selectByNamespace(namespace);
		registryCenterConfig.setConnectString(connectString);
		registryCenterConfig.setLastUpdatedBy(lastUpdatedBy);
		return registryCenterConfigRepository.updateById(registryCenterConfig);
	}

	@Transactional
	@Override
	public int remove(String namespace, String lastUpdatedBy) {
		RegistryCenterConfig registryCenterConfig = registryCenterConfigRepository.selectByNamespace(namespace);
		registryCenterConfig.setIsDeleted(1);
		return registryCenterConfigRepository.updateById(registryCenterConfig);
	}

}
