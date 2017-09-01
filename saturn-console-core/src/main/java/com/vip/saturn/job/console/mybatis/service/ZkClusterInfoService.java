package com.vip.saturn.job.console.mybatis.service;

import java.util.List;

import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;

/**
 * 
 * @author hebelala
 *
 */
public interface ZkClusterInfoService {

	List<ZkClusterInfo> getAllZkClusterInfo();

	ZkClusterInfo getByClusterKey(String clusterKey);

}
