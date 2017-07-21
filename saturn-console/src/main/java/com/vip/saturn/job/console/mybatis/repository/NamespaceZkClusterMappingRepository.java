package com.vip.saturn.job.console.mybatis.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;

/**
 * 
 * @author hebelala
 *
 */
@Repository
public interface NamespaceZkClusterMappingRepository {

	List<NamespaceZkClusterMapping> selectAll();

	NamespaceZkClusterMapping selectByNamespace(String namespace);

}
