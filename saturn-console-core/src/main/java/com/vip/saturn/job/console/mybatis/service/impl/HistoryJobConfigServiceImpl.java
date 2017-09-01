
package com.vip.saturn.job.console.mybatis.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vip.saturn.job.console.mybatis.entity.HistoryJobConfig;
import com.vip.saturn.job.console.mybatis.repository.HistoryJobConfigRepository;
import com.vip.saturn.job.console.mybatis.service.HistoryJobConfigService;

@Service
public class HistoryJobConfigServiceImpl implements HistoryJobConfigService {

	@Autowired
	private HistoryJobConfigRepository historyJobConfigRepo;

	@Transactional(readOnly = false)
	@Override
	public int create(HistoryJobConfig historyJobConfig) throws Exception {
		return historyJobConfigRepo.insert(historyJobConfig);
	}

	@Transactional
	@Override
	public int createSelective(HistoryJobConfig historyJobConfig) throws Exception {
		return historyJobConfigRepo.insertSelective(historyJobConfig);
	}

	@Transactional
	@Override
	public int deleteByPrimaryKey(Long id) throws Exception {
		return historyJobConfigRepo.deleteByPrimaryKey(id);
	}

	@Transactional(readOnly = true)
	@Override
	public HistoryJobConfig findByPrimaryKey(Long id) throws Exception {
		HistoryJobConfig historyJobConfig = historyJobConfigRepo.selectByPrimaryKey(id);
		historyJobConfig.setDefaultValues();
		return historyJobConfig;
	}

	@Transactional(readOnly = true)
	@Override
	public int selectCount(HistoryJobConfig historyJobConfig) throws Exception {
		return historyJobConfigRepo.selectCount(historyJobConfig);
	}

	@Transactional
	@Override
	public int updateByPrimaryKey(HistoryJobConfig historyJobConfig) throws Exception {
		return historyJobConfigRepo.updateByPrimaryKey(historyJobConfig);
	}

	@Transactional
	@Override
	public int updateByPrimaryKeySelective(HistoryJobConfig historyJobConfig) throws Exception {
		return historyJobConfigRepo.updateByPrimaryKeySelective(historyJobConfig);
	}

	@Override
	public List<HistoryJobConfig> selectPage(HistoryJobConfig historyjobconfig, Pageable pageable) throws Exception {
		List<HistoryJobConfig> historyJobConfigs = historyJobConfigRepo.selectPage(historyjobconfig, pageable);
		if (historyJobConfigs != null) {
			int i = 1;
			for (HistoryJobConfig historyJobConfig : historyJobConfigs) {
				historyJobConfig.setRownum(i++);
				historyJobConfig.setDefaultValues();
			}
		}
		return historyJobConfigs;
	}

}
