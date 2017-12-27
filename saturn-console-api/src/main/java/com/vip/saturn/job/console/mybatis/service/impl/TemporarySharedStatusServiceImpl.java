package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.mybatis.entity.TemporarySharedStatus;
import com.vip.saturn.job.console.mybatis.repository.ShareStatusRepository;
import com.vip.saturn.job.console.mybatis.service.TemporarySharedStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author hebelala
 */
@Service
public class TemporarySharedStatusServiceImpl implements TemporarySharedStatusService {

	@Autowired
	private ShareStatusRepository shareStatusRepository;

	@Transactional
	@Override
	public int delete(String statusKey) {
		TemporarySharedStatus temporarySharedStatus = new TemporarySharedStatus();
		temporarySharedStatus.setStatusKey(statusKey);
		return shareStatusRepository.delete(temporarySharedStatus);
	}

	@Transactional
	@Override
	public int create(String statusKey, String statusValue) {
		TemporarySharedStatus temporarySharedStatus = new TemporarySharedStatus();
		temporarySharedStatus.setStatusKey(statusKey);
		temporarySharedStatus.setStatusValue(statusValue);
		return shareStatusRepository.create(temporarySharedStatus);
	}

	@Transactional
	@Override
	public int update(String statusKey, String statusValue) {
		TemporarySharedStatus temporarySharedStatus = new TemporarySharedStatus();
		temporarySharedStatus.setStatusKey(statusKey);
		temporarySharedStatus.setStatusValue(statusValue);
		return shareStatusRepository.update(temporarySharedStatus);
	}

	@Transactional(readOnly = true)
	@Override
	public TemporarySharedStatus get(String statusKey) {
		TemporarySharedStatus temporarySharedStatus = new TemporarySharedStatus();
		temporarySharedStatus.setStatusKey(statusKey);
		return shareStatusRepository.get(temporarySharedStatus);
	}

}
