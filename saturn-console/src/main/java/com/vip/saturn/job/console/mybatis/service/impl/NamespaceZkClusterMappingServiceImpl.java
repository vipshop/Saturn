package com.vip.saturn.job.console.mybatis.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;
import com.vip.saturn.job.console.mybatis.repository.NamespaceZkClusterMappingRepository;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMappingService;

/**
 * 
 * @author hebelala
 *
 */
@Service
public class NamespaceZkClusterMappingServiceImpl implements NamespaceZkClusterMappingService {

	@Autowired
	private NamespaceZkClusterMappingRepository namespaceZkClusterMappingRepository;

	@Transactional(readOnly = true)
	@Override
	public List<NamespaceZkClusterMapping> getAllNamespaceZkClusterMapping() {
		return namespaceZkClusterMappingRepository.selectAll();
	}

}
