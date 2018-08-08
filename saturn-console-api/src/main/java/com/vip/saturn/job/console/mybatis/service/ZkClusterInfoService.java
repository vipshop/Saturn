package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;

import java.util.List;

/**
 * @author hebelala
 */
public interface ZkClusterInfoService {

	List<ZkClusterInfo> getAllZkClusterInfo();

	ZkClusterInfo getByClusterKey(String clusterKey);

	int createZkCluster(String clusterKey, String alias, String connectString, String createdBy);

	int updateZkCluster(ZkClusterInfo zkClusterInfo);

}
