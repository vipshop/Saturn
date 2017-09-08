package com.vip.saturn.job.console.mybatis.repository;

import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.NamespaceVersionMapping;

/**
 * 
 * @author hebelala
 *
 */
@Repository
public interface NamespaceVersionMappingRepository {

	Integer insert(NamespaceVersionMapping namespaceVersionMapping);

}
