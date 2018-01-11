package com.vip.saturn.job.console.mybatis.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.vip.saturn.job.console.mybatis.entity.NamespaceInfo;
import com.vip.saturn.job.console.mybatis.repository.NamespaceInfoRepository;
import com.vip.saturn.job.console.mybatis.service.NamespaceInfoService;

/**
 * @author timmy.hu
 *
 */
@Service
public class NamespaceInfoServiceImpl implements NamespaceInfoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceInfoServiceImpl.class);

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
			} catch (InterruptedException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

}
