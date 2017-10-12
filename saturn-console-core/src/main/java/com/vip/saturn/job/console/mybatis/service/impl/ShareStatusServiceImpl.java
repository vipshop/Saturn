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
	public int delete(String function) {
		ShareStatus shareStatus = new ShareStatus();
		shareStatus.setFunction(function);
		return shareStatusRepository.delete(shareStatus);
	}

	@Transactional
	@Override
	public int create(String function, String data) {
		ShareStatus shareStatus = new ShareStatus();
		shareStatus.setFunction(function);
		shareStatus.setData(data);
		return shareStatusRepository.create(shareStatus);
	}

	@Transactional
	@Override
	public int update(String function, String data) {
		ShareStatus shareStatus = new ShareStatus();
		shareStatus.setFunction(function);
		shareStatus.setData(data);
		return shareStatusRepository.update(shareStatus);
	}

	@Transactional(readOnly = true)
	@Override
	public ShareStatus get(String function) {
		ShareStatus shareStatus = new ShareStatus();
		shareStatus.setFunction(function);
		return shareStatusRepository.get(shareStatus);
	}

}
