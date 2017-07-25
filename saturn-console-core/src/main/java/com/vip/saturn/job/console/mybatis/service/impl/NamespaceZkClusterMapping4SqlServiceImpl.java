package com.vip.saturn.job.console.mybatis.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vip.saturn.job.console.mybatis.entity.NamespaceZkClusterMapping;
import com.vip.saturn.job.console.mybatis.repository.NamespaceZkClusterMappingRepository;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMapping4SqlService;

/**
 * 
 * @author hebelala
 *
 */
@Service
public class NamespaceZkClusterMapping4SqlServiceImpl implements NamespaceZkClusterMapping4SqlService {

	@Autowired
	private NamespaceZkClusterMappingRepository namespaceZkClusterMappingRepository;

	private List<NamespaceZkClusterMapping> existingAll(List<NamespaceZkClusterMapping> namespaceZkClusterMappings) {
		List<NamespaceZkClusterMapping> existingAll = new ArrayList<>();
		if (namespaceZkClusterMappings != null) {
			Iterator<NamespaceZkClusterMapping> iterator = namespaceZkClusterMappings.iterator();
			while (iterator.hasNext()) {
				NamespaceZkClusterMapping next = iterator.next();
				if (next.getIsDeleted() == 0) {
					existingAll.add(next);
				}
			}
		}
		return existingAll;
	}

	@Transactional(readOnly = true)
	@Override
	public List<NamespaceZkClusterMapping> getAllMappings() {
		return existingAll(namespaceZkClusterMappingRepository.selectAll());
	}

	@Transactional(readOnly = true)
	@Override
	public List<NamespaceZkClusterMapping> getAllMappingsOfCluster(String zkClusterKey) {
		return existingAll(namespaceZkClusterMappingRepository.selectByZkClusterKey(zkClusterKey));
	}

	@Transactional(readOnly = true)
	@Override
	public List<String> getAllNamespacesOfCluster(String zkClusterKey) {
		List<String> result = new ArrayList<String>();
		List<NamespaceZkClusterMapping> namespaceZkClusterMappings = existingAll(
				namespaceZkClusterMappingRepository.selectByZkClusterKey(zkClusterKey));
		for (NamespaceZkClusterMapping mapping : namespaceZkClusterMappings) {
			result.add(mapping.getNamespace());
		}
		return result;
	}

	@Transactional(readOnly = true)
	@Override
	public String getZkClusterKey(String namespace) {
		NamespaceZkClusterMapping namespaceZkClusterMapping = namespaceZkClusterMappingRepository
				.selectByNamespace(namespace);
		if (namespaceZkClusterMapping == null) {
			return null;
		}
		if (namespaceZkClusterMapping.getIsDeleted() == 1) {
			return null;
		}
		return namespaceZkClusterMapping.getZkClusterKey();
	}

	@Transactional
	@Override
	public Integer insert(String namespace, String name, String zkClusterKey, String createdBy) {
		NamespaceZkClusterMapping namespaceZkClusterMapping = namespaceZkClusterMappingRepository
				.selectByNamespace(namespace);
		boolean insert;
		if (namespaceZkClusterMapping == null) {
			namespaceZkClusterMapping = new NamespaceZkClusterMapping();
			insert = true;
		} else {
			insert = false;
		}
		Date now = new Date();
		namespaceZkClusterMapping.setIsDeleted(0);
		namespaceZkClusterMapping.setCreateTime(now);
		namespaceZkClusterMapping.setCreatedBy(createdBy);
		namespaceZkClusterMapping.setLastUpdateTime(now);
		namespaceZkClusterMapping.setLastUpdatedBy(createdBy);
		namespaceZkClusterMapping.setNamespace(namespace);
		namespaceZkClusterMapping.setName(name);
		namespaceZkClusterMapping.setZkClusterKey(zkClusterKey);
		if (insert) {
			return namespaceZkClusterMappingRepository.insert(namespaceZkClusterMapping);
		} else {
			return namespaceZkClusterMappingRepository.updateAllById(namespaceZkClusterMapping);
		}
	}

	@Transactional
	@Override
	public Integer remove(String namespace, String lastUpdatedBy) {
		NamespaceZkClusterMapping namespaceZkClusterMapping = namespaceZkClusterMappingRepository
				.selectByNamespace(namespace);
		namespaceZkClusterMapping.setIsDeleted(1);
		namespaceZkClusterMapping.setLastUpdatedBy(lastUpdatedBy);
		return namespaceZkClusterMappingRepository.updateById(namespaceZkClusterMapping);
	}

	@Transactional
	@Override
	public Integer update(String namespace, String name, String zkClusterKey, String lastUpdatedBy) {
		NamespaceZkClusterMapping namespaceZkClusterMapping = namespaceZkClusterMappingRepository
				.selectByNamespace(namespace);
		if (name != null) {
			namespaceZkClusterMapping.setName(name);
		}
		if (zkClusterKey != null) {
			namespaceZkClusterMapping.setZkClusterKey(zkClusterKey);
		}
		namespaceZkClusterMapping.setLastUpdatedBy(lastUpdatedBy);
		return namespaceZkClusterMappingRepository.updateById(namespaceZkClusterMapping);
	}
}
