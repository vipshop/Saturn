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

package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HistoryJobConfigService {

	int create(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	int createSelective(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	int deleteByPrimaryKey(Long id) throws SaturnJobConsoleException;

	JobConfig4DB findByPrimaryKey(Long id) throws SaturnJobConsoleException;

	int selectCount(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	int updateByPrimaryKey(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	int updateByPrimaryKeySelective(JobConfig4DB historyJobConfig) throws SaturnJobConsoleException;

	List<JobConfig4DB> selectPage(JobConfig4DB historyJobConfig, Pageable pageable) throws SaturnJobConsoleException;

}