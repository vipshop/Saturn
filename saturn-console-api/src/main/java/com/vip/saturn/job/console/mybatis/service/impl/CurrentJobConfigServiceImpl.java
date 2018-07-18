package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.repository.CurrentJobConfigRepository;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.mybatis.service.HistoryJobConfigService;
import java.util.Map;
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

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int create(JobConfig4DB currentJobConfig) throws Exception {
		return currentJobConfigRepo.insert(currentJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int createSelective(JobConfig4DB currentJobConfig) throws Exception {
		return currentJobConfigRepo.insertSelective(currentJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
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

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int updateByPrimaryKey(JobConfig4DB currentJobConfig) throws Exception {
		return currentJobConfigRepo.updateByPrimaryKey(currentJobConfig);
	}

	@Transactional(rollbackFor = Exception.class)
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

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateNewAndSaveOld2History(final JobConfig4DB newJobConfig, final JobConfig4DB oldJobConfig,
			final String userName) throws Exception {
		// 持久化当前配置
		if (userName != null) {
			newJobConfig.setLastUpdateBy(userName);
		}
		newJobConfig.setLastUpdateTime(new Date());
		updateByPrimaryKey(newJobConfig);

		// 持久化历史配置
		oldJobConfig.setId(null);
		historyJobConfigService.create(oldJobConfig);
	}

	@Override
	public List<JobConfig4DB> findConfigsByNamespace(String namespace) {
		return currentJobConfigRepo.findConfigsByNamespace(namespace);
	}

	@Override
	public List<JobConfig4DB> findConfigsByNamespaceWithCondition(String namespace, Map<String, Object> condition,
			Pageable pageable) {
		return currentJobConfigRepo.findConfigsByNamespaceWithCondition(namespace, condition, pageable);
	}

	@Override
    public int countConfigsByNamespaceWithCondition(String namespace, Map<String, Object> condition) {
	    return currentJobConfigRepo.countConfigsByNamespaceWithCondition(namespace, condition);
    }

    @Override
    public int countEnabledUnSystemJobsByNamespace(String namespace) {
        return currentJobConfigRepo.countEnabledUnSystemJobsByNamespace(namespace, 1);
    }

    @Override
	public List<String> findConfigNamesByNamespace(String namespace) {
		return currentJobConfigRepo.findConfigNamesByNamespace(namespace);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int deleteAll(int limitNum) {
		return currentJobConfigRepo.deleteAll(limitNum);
	}

}
