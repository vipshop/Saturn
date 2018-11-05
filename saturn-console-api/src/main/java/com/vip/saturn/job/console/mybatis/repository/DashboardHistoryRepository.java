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
