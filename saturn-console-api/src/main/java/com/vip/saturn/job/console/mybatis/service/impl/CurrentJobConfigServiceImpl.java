package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.repository.CurrentJobConfigRepository;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.mybatis.service.HistoryJobConfigService;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

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
	public int create(JobConfig4DB currentJobConfig) throws Exception {
		return currentJobConfigRepo.insert(currentJobConfig);
	}

	@Transactional
	@Override
	public int createSelective(JobConfig4DB currentJobConfig) throws Exception {
		return currentJobConfigRepo.insertSelective(currentJobConfig);
	}

	@Transactional
	@Override
	public int deleteByPrimaryKey(Long id) throws Exception {
		return currentJobConfigRepo.deleteByPrimaryKey(id);
	}

	@Transactional(readOnly = true)
	@Override
	public JobConfig4DB findByPrimaryKey(Long id) throws Exception {
		JobConfig4DB currentJobConfig = currentJobConfigRepo.selectByPrimaryKey(id);
		return currentJobConfig;
	}

	@Transactional(readOnly = true)
	@Override
	public int selectCount(JobConfig4DB currentJobConfig) throws Exception {
		return currentJobConfigRepo.selectCount(currentJobConfig);
	}

	@Transactional
	@Override
	public int updateByPrimaryKey(JobConfig4DB currentJobConfig) throws Exception {
		return currentJobConfigRepo.updateByPrimaryKey(currentJobConfig);
	}

	@Transactional
	@Override
	public int updateByPrimaryKeySelective(JobConfig4DB currentJobConfig) throws Exception {
		return currentJobConfigRepo.updateByPrimaryKeySelective(currentJobConfig);
	}

	@Override
	public void batchUpdatePreferList(List<JobConfig4DB> jobConfigs) throws SaturnJobConsoleException {
		SqlSession batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
		try {
			for (JobConfig4DB currentJobConfig : jobConfigs) {
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
	public List<JobConfig4DB> selectPage(JobConfig4DB currentJobConfig, Pageable pageable) throws Exception {
		return currentJobConfigRepo.selectPage(currentJobConfig, pageable);
	}

	@Override
	public JobConfig4DB findConfigByNamespaceAndJobName(String namespace, String jobName) {
		return currentJobConfigRepo.findConfigByNamespaceAndJobName(namespace, jobName);
	}

	@Transactional
	@Override
	public void updateNewAndSaveOld2History(final JobConfig4DB newJobConfig, final JobConfig4DB oldJobConfig,
			final String userName) throws Exception {
		// 拷贝出历史配置
		JobConfig4DB history = mapper.map(oldJobConfig, JobConfig4DB.class);
		// 将新配置覆盖更新至历史配置，新生成当前配置
		JobConfig4DB current = mapper.map(oldJobConfig, JobConfig4DB.class);
		mapper.map(newJobConfig, current);

		// 持久化当前配置
		if (userName != null) {
			current.setLastUpdateBy(userName);
		}
		current.setLastUpdateTime(new Date());
		updateByPrimaryKey(current);

		// 持久化历史配置
		history.setId(null);
		historyJobConfigService.create(history);
	}

	@Transactional(readOnly = true)
	@Override
	public List<JobConfig4DB> findConfigsByNamespace(String namespace) {
		return currentJobConfigRepo.findConfigsByNamespace(namespace);
	}

	@Transactional
	@Override
	public int deleteAll(int limitNum) {
		return currentJobConfigRepo.deleteAll(limitNum);
	}

}
