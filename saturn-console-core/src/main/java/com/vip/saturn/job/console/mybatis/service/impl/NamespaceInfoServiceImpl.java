/**
 * 
 */
package com.vip.saturn.job.console.mybatis.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
	public void replaceAll(List<NamespaceInfo> namespaceInfos) {
		deleteAll();
		if (CollectionUtils.isEmpty(namespaceInfos)) {
			return;
		}
		batchInsert(namespaceInfos);
	}

	@Transactional
	@Override
	public void batchInsert(List<NamespaceInfo> namespaceInfos) {
		if (CollectionUtils.isEmpty(namespaceInfos)) {
			return;
		}
		List<NamespaceInfo> toInsertList;
		int insertTime = namespaceInfos.size() / BATCH_NUM + 1;
		int curIndex = 0;
		for (int i = 0; i < insertTime; i++) {
			toInsertList = new ArrayList<NamespaceInfo>();
			for (int j = 0; j < BATCH_NUM; j++) {
				if (curIndex == namespaceInfos.size() - 1) {
					break;
				}
				toInsertList.add(namespaceInfos.get(curIndex));
				curIndex++;
			}
			namespaceInfoRepository.batchInsert(namespaceInfos);
			if (curIndex == namespaceInfos.size() - 1) {
				break;
			}
		}
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
