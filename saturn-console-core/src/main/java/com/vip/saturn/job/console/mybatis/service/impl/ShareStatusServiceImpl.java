package com.vip.saturn.job.console.mybatis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vip.saturn.job.console.mybatis.entity.ShareStatus;
import com.vip.saturn.job.console.mybatis.repository.ShareStatusRepository;
import com.vip.saturn.job.console.mybatis.service.ShareStatusService;

/**
 * 
 * @author hebelala
 *
 */
@Service
public class ShareStatusServiceImpl implements ShareStatusService {

	@Autowired
	private ShareStatusRepository shareStatusRepository;

	@Transactional
	@Override
	public int delete(String moduleName) {
		ShareStatus shareStatus = new ShareStatus();
		shareStatus.setModuleName(moduleName);
		return shareStatusRepository.delete(shareStatus);
	}

	@Transactional
	@Override
	public int create(String moduleName, String data) {
		ShareStatus shareStatus = new ShareStatus();
		shareStatus.setModuleName(moduleName);
		shareStatus.setData(data);
		return shareStatusRepository.create(shareStatus);
	}

	@Transactional
	@Override
	public int update(String moduleName, String data) {
		ShareStatus shareStatus = new ShareStatus();
		shareStatus.setModuleName(moduleName);
		shareStatus.setData(data);
		return shareStatusRepository.update(shareStatus);
	}

	@Transactional(readOnly = true)
	@Override
	public ShareStatus get(String moduleName) {
		ShareStatus shareStatus = new ShareStatus();
		shareStatus.setModuleName(moduleName);
		return shareStatusRepository.get(shareStatus);
	}

}
