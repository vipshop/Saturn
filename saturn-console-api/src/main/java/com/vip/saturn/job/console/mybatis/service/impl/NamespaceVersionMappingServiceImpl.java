package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.mybatis.entity.NamespaceVersionMapping;
import com.vip.saturn.job.console.mybatis.repository.NamespaceVersionMappingRepository;
import com.vip.saturn.job.console.mybatis.service.NamespaceVersionMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author timmy.hu
 */
@Service
public class NamespaceVersionMappingServiceImpl implements NamespaceVersionMappingService {

    @Autowired
    private NamespaceVersionMappingRepository namespaceVersionMappingRepository;

    @Override
    public int insertOrUpdate(String namespace, String versionNumber, boolean isForced, String who) {
        NamespaceVersionMapping namespaceVersionMapping = namespaceVersionMappingRepository.selectByNamespace(namespace);
        if(namespaceVersionMapping != null) {
            namespaceVersionMapping.setVersionNumber(versionNumber);
            namespaceVersionMapping.setIsForced(isForced);
            namespaceVersionMapping.setLastUpdatedBy(who);
            namespaceVersionMapping.setIsDeleted(false);
            return namespaceVersionMappingRepository.update(namespaceVersionMapping);
        } else {
            namespaceVersionMapping = new NamespaceVersionMapping();
            namespaceVersionMapping.setNamespace(namespace);
            namespaceVersionMapping.setVersionNumber(versionNumber);
            namespaceVersionMapping.setIsForced(isForced);
            namespaceVersionMapping.setCreatedBy(who);
            Date now = new Date();
            namespaceVersionMapping.setCreateTime(now);
            namespaceVersionMapping.setLastUpdatedBy(who);
            namespaceVersionMapping.setLastUpdateTime(now);
            namespaceVersionMapping.setIsDeleted(false);
            return namespaceVersionMappingRepository.insert(namespaceVersionMapping);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<NamespaceVersionMapping> selectAllWithNotDeleted() {
        return namespaceVersionMappingRepository.selectAllWithNotDeleted();
    }

	@Override
	public NamespaceVersionMapping selectByNamespace(String namespace) {
		return namespaceVersionMappingRepository.selectByNamespace(namespace);
	}
}
