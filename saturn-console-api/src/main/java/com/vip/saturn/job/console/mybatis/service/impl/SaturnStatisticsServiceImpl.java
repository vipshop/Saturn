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

import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.mybatis.repository.SaturnStatisticsRepository;
import com.vip.saturn.job.console.mybatis.service.SaturnStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
