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
