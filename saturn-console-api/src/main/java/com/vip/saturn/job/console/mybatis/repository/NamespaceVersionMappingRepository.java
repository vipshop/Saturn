package com.vip.saturn.job.console.mybatis.repository;

import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.NamespaceVersionMapping;

import java.util.List;

/**
 * 
 * @author hebelala
 *
 */
@Repository
public interface NamespaceVersionMappingRepository {

	int insert(NamespaceVersionMapping namespaceVersionMapping);

	int update(NamespaceVersionMapping namespaceVersionMapping);

	NamespaceVersionMapping selectByNamespace(String namespace);

	List<NamespaceVersionMapping> selectAllWithNotDeleted();

}
