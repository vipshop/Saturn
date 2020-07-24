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

import com.vip.saturn.job.console.mybatis.entity.NamespaceInfo;
import com.vip.saturn.job.console.mybatis.repository.NamespaceInfoRepository;
import com.vip.saturn.job.console.mybatis.service.NamespaceInfoService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author timmy.hu
 */
@Service
public class NamespaceInfoServiceImpl implements NamespaceInfoService {

	private static final Logger log = LoggerFactory.getLogger(NamespaceInfoServiceImpl.class);

	private static final int BATCH_NUM = 2000;

	@Autowired
	private NamespaceInfoRepository namespaceInfoRepository;

	@Override
	public NamespaceInfo selectByNamespace(String namespace) {
		return namespaceInfoRepository.selectByNamespace(namespace);
	}

	@Override
	public List<NamespaceInfo> selectAll() {
		return namespaceInfoRepository.selectAll();
	}

	@Override
	public List<NamespaceInfo> selectAll(List<String> nsList) {
		return namespaceInfoRepository.selectAllByNamespaces(nsList);
	}

	@Transactional
	@Override
	public void create(NamespaceInfo namespaceInfo) {
		namespaceInfoRepository.insert(namespaceInfo);
	}

	@Override
	public void update(NamespaceInfo namespaceInfo) {
		namespaceInfoRepository.update(namespaceInfo);
	}

	@Transactional
	@Override
	public void replaceAll(List<NamespaceInfo> namespaceInfos) {
		deleteAll();
		if (CollectionUtils.isEmpty(namespaceInfos)) {
			return;
		}
		batchCreate(namespaceInfos);
	}

	@Transactional
	@Override
	public void batchCreate(List<NamespaceInfo> namespaceInfos) {
		if (CollectionUtils.isEmpty(namespaceInfos)) {
			return;
		}
		List<NamespaceInfo> toInsertList;
		int divNum = namespaceInfos.size() / BATCH_NUM;
		for (int i = 0; i < divNum; i++) {
			toInsertList = namespaceInfos.subList(i * BATCH_NUM, (i + 1) * BATCH_NUM);
			namespaceInfoRepository.batchInsert(toInsertList);
		}
		if (namespaceInfos.size() > divNum * BATCH_NUM) {
			toInsertList = namespaceInfos.subList(divNum * BATCH_NUM, namespaceInfos.size());
			namespaceInfoRepository.batchInsert(toInsertList);
		}
	}

	@Transactional
	@Override
	public int deleteByNamespace(String namespace) {
		return namespaceInfoRepository.deleteByNamespace(namespace);
	}

	@Transactional
	@Override
	public void deleteAll() {
		int deleteNum = 0;
		while (true) {
			deleteNum = namespaceInfoRepository.batchDelete(BATCH_NUM);
			if (deleteNum < BATCH_NUM) {
				return;
			}
			try {
				TimeUnit.MILLISECONDS.sleep(50);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

}
