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

import com.vip.saturn.job.console.mybatis.entity.DashboardHistory;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * @author Ray Leung
 */
@Repository
public interface DashboardHistoryRepository {

	/**
	 * 创建或更新dashboard统计信息
	 * @param zkCluster
	 * @param type
	 * @param topic
	 * @param content
	 * @param recordDate
	 * @return
	 */
	int createOrUpdateHistory(@Param("zkCluster") String zkCluster, @Param("type") String type,
			@Param("topic") String topic, @Param("content") String content, @Param("recordDate") Date recordDate);

	/**
	 * 范围查询Dashboard历史数据
	 * @param zkCluster
	 * @param type
	 * @param topic
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<DashboardHistory> selectByZkClustersAndTypeAndTopicAndFromStartDateToEndDate(@Param("zkClusters") List<String> zkCluster,
			@Param("type") String type, @Param("topic") String topic, @Param("startDate") Date startDate,
			@Param("endDate") Date endDate);

	/**
	 * 批量更新dashboard历史数据
	 * @param dashboardHistories
	 * @return
	 */
	int batchCreateOrUpdateHistory(@Param("list") List<DashboardHistory> dashboardHistories);
}
