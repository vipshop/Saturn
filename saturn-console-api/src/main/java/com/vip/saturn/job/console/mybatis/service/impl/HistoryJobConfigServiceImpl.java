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

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.repository.HistoryJobConfigRepository;
import com.vip.saturn.job.console.mybatis.service.HistoryJobConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistoryJobConfigServiceImpl implements HistoryJobConfigService {

	@Autowired
	private HistoryJobConfigRepository historyJobConfigRepo;

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int create(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException {
		return historyJobConfigRepo.insert(historyJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int createSelective(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException {
		return historyJobConfigRepo.insertSelective(historyJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int deleteByPrimaryKey(Long id) throws SaturnJobConsoleException {
		return historyJobConfigRepo.deleteByPrimaryKey(id);
	}

	@Transactional(readOnly = true)
	@Override
	public JobConfig4DB findByPrimaryKey(Long id) throws SaturnJobConsoleException {
		JobConfig4DB historyJobConfig = historyJobConfigRepo.selectByPrimaryKey(id);
		historyJobConfig.setDefaultValues();
		return historyJobConfig;
	}

	@Transactional(readOnly = true)
	@Override
	public int selectCount(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException {
		return historyJobConfigRepo.selectCount(historyJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int updateByPrimaryKey(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException {
		return historyJobConfigRepo.updateByPrimaryKey(historyJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int updateByPrimaryKeySelective(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException {
		return historyJobConfigRepo.updateByPrimaryKeySelective(historyJobConfig);
	}

	@Transactional
	@Override
	public List<JobConfig4DB> selectPage(JobConfig4DB historyjobconfig, Pageable pageable)
			throws SaturnJobConsoleException {
		List<JobConfig4DB> historyJobConfigs = historyJobConfigRepo.selectPage(historyjobconfig, pageable);
		if (historyJobConfigs != null) {
			int i = 1;
			for (JobConfig4DB historyJobConfig : historyJobConfigs) {
				historyJobConfig.setRownum(i++);
				historyJobConfig.setDefaultValues();
			}
		}
		return historyJobConfigs;
	}

}
