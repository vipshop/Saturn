package com.vip.saturn.job.console.mybatis.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;

/**
 * 
 * @author hebelala
 *
 */
@Repository
public interface ZkClusterInfoRepository {

	List<ZkClusterInfo> selectAll();

	ZkClusterInfo selectByClusterKey(String clusterKey);

}
