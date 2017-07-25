package com.vip.saturn.job.console.mybatis.service;

import java.util.List;

import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;

/**
 * 
 * @author hebelala
 *
 */
public interface NamespaceZkClusterMappingService {

	List<NamespaceZkClusterMapping> getAllMappings();
	
	List<NamespaceZkClusterMapping> getAllMappingsOfCluster(String zkClusterKey);
	
	List<String> getAllNamespacesOfCluster(String zkClusterKey);
	
	String getZkClusterKey(String namespace);
	
	Integer insert(String namespace, String name, String zkClusterKey, String createdBy);

	Integer remove(String namespace, String lastUpdatedBy);

	Integer update(String namespace, String name, String zkClusterKey, String lastUpdatedBy);

}
