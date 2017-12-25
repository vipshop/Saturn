
package com.vip.saturn.job.console.mybatis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.mybatis.repository.SaturnStatisticsRepository;
import com.vip.saturn.job.console.mybatis.service.SaturnStatisticsService;

@Service
public class SaturnStatisticsServiceImpl implements SaturnStatisticsService {

	@Autowired
	private SaturnStatisticsRepository saturnStatisticsRepo;

	/*
	 * @Transactional is not necessarry for the single atomic CRUD statement for better performance, but you still have
	 * to take care of @Transactional for multi-statements scenario. if read only,please config as
	 * "@Transactional(readOnly = true)",otherwise "@Transactional"
	 */
	@Transactional
	@Override
	public int deleteByPrimaryKey(Integer id) {
		return saturnStatisticsRepo.deleteByPrimaryKey(id);
	}

	/*
	 * @Transactional is not necessarry for the single atomic CRUD statement for better performance, but you still have
	 * to take care of @Transactional for multi-statements scenario. if read only,please config as
	 * "@Transactional(readOnly = true)",otherwise "@Transactional"
	 */
	@Transactional(readOnly = true)
	@Override
	public SaturnStatistics findByPrimaryKey(Integer id) {
		return saturnStatisticsRepo.selectByPrimaryKey(id);
	}

	/*
	 * @Transactional is not necessarry for the single atomic CRUD statement for better performance, but you still have
	 * to take care of @Transactional for multi-statements scenario. if read only,please config as
	 * "@Transactional(readOnly = true)",otherwise "@Transactional"
	 */
	@Transactional
	@Override
	public int updateByPrimaryKey(SaturnStatistics saturnStatistics) {
		return saturnStatisticsRepo.updateByPrimaryKey(saturnStatistics);
	}

	/*
	 * @Transactional is not necessarry for the single atomic CRUD statement for better performance, but you still have
	 * to take care of @Transactional for multi-statements scenario. if read only,please config as
	 * "@Transactional(readOnly = true)",otherwise "@Transactional"
	 */
	@Transactional
	@Override
	public int updateByPrimaryKeySelective(SaturnStatistics saturnStatistics) {
		return saturnStatisticsRepo.updateByPrimaryKeySelective(saturnStatistics);
	}

	/*
	 * @Transactional is not necessarry for the single atomic CRUD statement for better performance, but you still have
	 * to take care of @Transactional for multi-statements scenario. if read only,please config as
	 * "@Transactional(readOnly = true)",otherwise "@Transactional"
	 */
	@Transactional
	@Override
	public int create(SaturnStatistics saturnStatistics) {
		return saturnStatisticsRepo.insert(saturnStatistics);
	}

	/*
	 * @Transactional is not necessarry for the single atomic CRUD statement for better performance, but you still have
	 * to take care of @Transactional for multi-statements scenario. if read only,please config as
	 * "@Transactional(readOnly = true)",otherwise "@Transactional"
	 */
	@Transactional
	@Override
	public int createSelective(SaturnStatistics saturnStatistics) {
		return saturnStatisticsRepo.insertSelective(saturnStatistics);
	}

	/*
	 * @Transactional is not necessarry for the single atomic CRUD statement for better performance, but you still have
	 * to take care of @Transactional for multi-statements scenario. if read only,please config as
	 * "@Transactional(readOnly = true)",otherwise "@Transactional"
	 */
	@Transactional(readOnly = true)
	@Override
	public int selectCount(SaturnStatistics saturnStatistics) {
		return saturnStatisticsRepo.selectCount(saturnStatistics);
	}

	@Transactional(readOnly = true)
	@Override
	public SaturnStatistics findStatisticsByNameAndZkList(String name, String zklist) {
		return saturnStatisticsRepo.findStatisticsByNameAndZkList(name, zklist);
	}

}
