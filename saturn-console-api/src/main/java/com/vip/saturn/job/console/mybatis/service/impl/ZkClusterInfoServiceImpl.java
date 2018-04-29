package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.repository.ZkClusterInfoRepository;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author hebelala
 */
@Service
public class ZkClusterInfoServiceImpl implements ZkClusterInfoService {

	@Autowired
	private ZkClusterInfoRepository zkClusterInfoRepository;

	@Transactional(readOnly = true)
	@Override
	public List<ZkClusterInfo> getAllZkClusterInfo() {
		return zkClusterInfoRepository.selectAll();
	}

	@Transactional(readOnly = true)
	@Override
	public ZkClusterInfo getByClusterKey(String clusterKey) {
		return zkClusterInfoRepository.selectByClusterKey(clusterKey);
	}

	@Transactional
	@Override
	public int createZkCluster(String clusterKey, String alias, String connectString, String createdBy) {
		ZkClusterInfo zkClusterInfo = new ZkClusterInfo();
		Date now = new Date();
		zkClusterInfo.setCreateTime(now);
		zkClusterInfo.setCreatedBy(createdBy);
		zkClusterInfo.setLastUpdateTime(now);
		zkClusterInfo.setLastUpdatedBy(createdBy);
		zkClusterInfo.setZkClusterKey(clusterKey);
		zkClusterInfo.setAlias(alias);
		zkClusterInfo.setConnectString(connectString);
		return zkClusterInfoRepository.insert(zkClusterInfo);
	}

	@Transactional
	@Override
	public int updateZkCluster(ZkClusterInfo zkClusterInfo) {
		return zkClusterInfoRepository.update(zkClusterInfo);
	}
}
