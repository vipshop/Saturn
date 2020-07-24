/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
		NamespaceVersionMapping namespaceVersionMapping = namespaceVersionMappingRepository
				.selectByNamespace(namespace);
		if (namespaceVersionMapping != null) {
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

	@Override
	public int deleteMapping(String namespace, String versionNumber) {
		return namespaceVersionMappingRepository.deleteByNamespaceAndVersionNumber(namespace, versionNumber);
	}
}
