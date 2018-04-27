package com.vip.saturn.job.console.mybatis.service.impl;

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
	public int create(JobConfig4DB historyJobConfig) throws Exception {
		return historyJobConfigRepo.insert(historyJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int createSelective(JobConfig4DB historyJobConfig) throws Exception {
		return historyJobConfigRepo.insertSelective(historyJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int deleteByPrimaryKey(Long id) throws Exception {
		return historyJobConfigRepo.deleteByPrimaryKey(id);
	}

	@Transactional(readOnly = true)
	@Override
	public JobConfig4DB findByPrimaryKey(Long id) throws Exception {
		JobConfig4DB historyJobConfig = historyJobConfigRepo.selectByPrimaryKey(id);
		historyJobConfig.setDefaultValues();
		return historyJobConfig;
	}

	@Transactional(readOnly = true)
	@Override
	public int selectCount(JobConfig4DB historyJobConfig) throws Exception {
		return historyJobConfigRepo.selectCount(historyJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int updateByPrimaryKey(JobConfig4DB historyJobConfig) throws Exception {
		return historyJobConfigRepo.updateByPrimaryKey(historyJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int updateByPrimaryKeySelective(JobConfig4DB historyJobConfig) throws Exception {
		return historyJobConfigRepo.updateByPrimaryKeySelective(historyJobConfig);
	}

	@Transactional
	@Override
	public List<JobConfig4DB> selectPage(JobConfig4DB historyjobconfig, Pageable pageable) throws Exception {
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
