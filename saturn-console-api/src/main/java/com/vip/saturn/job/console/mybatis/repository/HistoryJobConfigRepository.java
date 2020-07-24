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

package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryJobConfigRepository {

	int deleteByPrimaryKey(Long id);

	int insert(JobConfig4DB historyJobConfig);

	int insertSelective(JobConfig4DB historyJobConfig);

	JobConfig4DB selectByPrimaryKey(Long id);

	int updateByPrimaryKeySelective(JobConfig4DB historyJobConfig);

	int updateByPrimaryKey(JobConfig4DB historyJobConfig);

	int selectCount(JobConfig4DB historyJobConfig);

	List<JobConfig4DB> selectPage(@Param("historyJobConfig") JobConfig4DB historyJobConfig,
			@Param("pageable") Pageable pageable);
}