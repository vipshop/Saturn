package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author hebelala
 */
@Repository
public interface NamespaceZkClusterMappingRepository {

	NamespaceZkClusterMapping selectByNamespace(String namespace);

	List<NamespaceZkClusterMapping> selectByNamespaceAndZkClusterKey(@Param("namespace") String namespace, @Param("zkClusterKey") String zkClusterKey);

	Integer deleteByNamespace(String namespace);

	List<NamespaceZkClusterMapping> selectAll();

	List<NamespaceZkClusterMapping> selectByZkClusterKey(String zkClusterKey);

	Integer insert(NamespaceZkClusterMapping namespaceZkClusterMapping);

	Integer updateById(NamespaceZkClusterMapping namespaceZkClusterMapping);

	Integer updateAllById(NamespaceZkClusterMapping namespaceZkClusterMapping);

}
