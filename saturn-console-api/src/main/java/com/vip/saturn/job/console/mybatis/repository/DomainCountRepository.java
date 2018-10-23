package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.DomainCount;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * @author Ray Leung
 */
@Repository
public interface DomainCountRepository {

	/**
	 * 创建或更新全域统计信息
	 * @param zkCluster
	 * @param recordDate
	 * @param successCount
	 * @param failCount
	 * @return
	 */
	int createOrUpdateDomainCount(@Param("zkCluster") String zkCluster, @Param("recordDate") Date recordDate,
			@Param("successCount") Integer successCount, @Param("failCount") Integer failCount);

	/**
	 * 通过zkCluster和recordDate查询全域信息
	 * @param zkCluster
	 * @param recordDate
	 * @return
	 */
	DomainCount selectByZkClusterAndRecordDate(@Param("zkCluster") String zkCluster,
			@Param("recordDate") Date recordDate);

	/**
	 * 按时间范围返回全域信息
	 * @param zkCluster
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	List<DomainCount> selectByZkClusterAndFromStartDateToEndDate(@Param("zkCluster") String zkCluster,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
