package com.vip.saturn.job.console.mybatis.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.repository.ZkClusterInfoRepository;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;

/**
 * 
 * @author hebelala
 *
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

}
