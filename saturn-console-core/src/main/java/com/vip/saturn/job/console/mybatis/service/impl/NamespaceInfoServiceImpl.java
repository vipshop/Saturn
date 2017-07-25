/**
 * 
 */
package com.vip.saturn.job.console.mybatis.service.impl;

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
import com.vip.saturn.job.console.service.helper.DashboardLeaderTreeCache;

/**
 * @author timmy.hu
 *
 */
@Service
public class NamespaceInfoServiceImpl implements NamespaceInfoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceInfoServiceImpl.class);

	private static final int MAX_DELETE_NUM = 2000;

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

	@Transactional
	@Override
	public void replaceAll(List<NamespaceInfo> namespaceInfos) {
		deleteAll();
		if(CollectionUtils.isEmpty(namespaceInfos)) {
			return;
		}
		namespaceInfoRepository.batchInsert(namespaceInfos);
	}

	@Override
	public Integer batchInsert(List<NamespaceInfo> namespaceInfos) {
		if (CollectionUtils.isEmpty(namespaceInfos)) {
			return 0;
		}
		return namespaceInfoRepository.batchInsert(namespaceInfos);
	}

	@Override
	public void deleteAll() {
		int deleteNum = 0;
		while (true) {
			deleteNum = namespaceInfoRepository.batchDelete(MAX_DELETE_NUM);
			if (deleteNum < MAX_DELETE_NUM) {
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
