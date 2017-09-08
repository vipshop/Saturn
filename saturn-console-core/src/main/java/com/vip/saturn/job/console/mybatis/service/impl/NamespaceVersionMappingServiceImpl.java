/**
 * 
 */
package com.vip.saturn.job.console.mybatis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vip.saturn.job.console.mybatis.entity.NamespaceVersionMapping;
import com.vip.saturn.job.console.mybatis.repository.NamespaceVersionMappingRepository;
import com.vip.saturn.job.console.mybatis.service.NamespaceVersionMappingService;

/**
 * @author timmy.hu
 *
 */
@Service
public class NamespaceVersionMappingServiceImpl implements NamespaceVersionMappingService {

	@Autowired
	private NamespaceVersionMappingRepository namespaceVersionMappingRepository;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.vip.saturn.job.console.mybatis.service.NamespaceVersionMappingService
	 * #insert(com.vip.saturn.job.console.mybatis.entity.
	 * NamespaceVersionMapping)
	 */
	@Override
	public int insert(NamespaceVersionMapping namespaceVersionMapping) {
		return namespaceVersionMappingRepository.insert(namespaceVersionMapping);
	}

}
