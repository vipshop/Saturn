
package com.vip.saturn.job.console.mybatis.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vip.saturn.job.console.domain.JobSettings;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.entity.HistoryJobConfig;
import com.vip.saturn.job.console.mybatis.repository.CurrentJobConfigRepository;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.mybatis.service.HistoryJobConfigService;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;

@Service
public class CurrentJobConfigServiceImpl implements CurrentJobConfigService {
	@Resource
	private HistoryJobConfigService historyJobConfigService;

	@Autowired
	private CurrentJobConfigRepository currentJobConfigRepo;
	@Autowired
	private SqlSessionFactory sqlSessionFactory;

	private MapperFacade mapper;

	@Autowired
	public void setMapperFactory(MapperFactory mapperFactory) {
		this.mapper = mapperFactory.getMapperFacade();
	}

	@Transactional(readOnly = false)
	@Override
	public int create(CurrentJobConfig currentJobConfig) throws Exception {
		return currentJobConfigRepo.insert(currentJobConfig);
	}

	@Transactional
	@Override
	public int createSelective(CurrentJobConfig currentJobConfig) throws Exception {
		return currentJobConfigRepo.insertSelective(currentJobConfig);
	}

	@Transactional
	@Override
	public int deleteByPrimaryKey(Long id) throws Exception {
		return currentJobConfigRepo.deleteByPrimaryKey(id);
	}

	@Transactional(readOnly = true)
	@Override
	public CurrentJobConfig findByPrimaryKey(Long id) throws Exception {
		CurrentJobConfig currentJobConfig = currentJobConfigRepo.selectByPrimaryKey(id);
		return currentJobConfig;
	}

	@Transactional(readOnly = true)
	@Override
	public int selectCount(CurrentJobConfig currentJobConfig) throws Exception {
		return currentJobConfigRepo.selectCount(currentJobConfig);
	}

	@Transactional
	@Override
	public int updateByPrimaryKey(CurrentJobConfig currentJobConfig) throws Exception {
		return currentJobConfigRepo.updateByPrimaryKey(currentJobConfig);
	}

	@Transactional
	@Override
	public int updateByPrimaryKeySelective(CurrentJobConfig currentJobConfig) throws Exception {
		return currentJobConfigRepo.updateByPrimaryKeySelective(currentJobConfig);
	}

	@Override
	public void batchUpdatePreferList(List<CurrentJobConfig> jobConfigs) throws SaturnJobConsoleException {
		SqlSession batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
		try {
			for (CurrentJobConfig currentJobConfig : jobConfigs) {
				batchSqlSession.getMapper(CurrentJobConfigRepository.class).updatePreferList(currentJobConfig);
			}
			batchSqlSession.commit();
		} catch (Exception e) {
			batchSqlSession.rollback();
			throw new SaturnJobConsoleException("error when batchUpdatePreferList", e);
		} finally {
			IOUtils.closeQuietly(batchSqlSession);
		}
	}

	@Override
	public List<CurrentJobConfig> selectPage(CurrentJobConfig currentJobConfig, Pageable pageable) throws Exception {
		return currentJobConfigRepo.selectPage(currentJobConfig, pageable);
	}

	@Override
	public CurrentJobConfig findConfigByNamespaceAndJobName(String namespace, String jobName) {
		return currentJobConfigRepo.findConfigByNamespaceAndJobName(namespace, jobName);
	}

	@Transactional
	@Override
	public void updateConfigAndSave2History(final CurrentJobConfig jobconfig, final JobSettings jobSettings,
			final String userName) throws Exception {
		HistoryJobConfig history = mapper.map(jobconfig, HistoryJobConfig.class);
		mapper.map(jobSettings, jobconfig);
		jobconfig.setLastUpdateBy(userName);
		jobconfig.setLastUpdateTime(new Date());
		updateByPrimaryKey(jobconfig);
		history.setId(null);
		historyJobConfigService.create(history);
	}

	@Transactional
	@Override
	public void updateConfigAndSave2History(final CurrentJobConfig newJobconfig, final CurrentJobConfig oldJobconfig,
			final String userName) throws Exception {
		HistoryJobConfig history = mapper.map(oldJobconfig, HistoryJobConfig.class);
		if (userName != null) {
			newJobconfig.setLastUpdateBy(userName);
		}
		newJobconfig.setLastUpdateTime(new Date());
		updateByPrimaryKey(newJobconfig);
		history.setId(null);
		historyJobConfigService.create(history);
	}

	@Transactional(readOnly = true)
	@Override
	public List<CurrentJobConfig> findConfigsByNamespace(String namespace) {
		return currentJobConfigRepo.findConfigsByNamespace(namespace);
	}

	@Transactional
	@Override
	public int deleteAll(int limitNum) {
		return currentJobConfigRepo.deleteAll(limitNum);
	}

}
