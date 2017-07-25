package com.vip.saturn.job.console.mybatis.service;

import java.util.List;

import com.vip.saturn.job.console.mybatis.entity.NamespaceInfo;

/**
 * 
 * @author timmy.hu
 *
 */
public interface NamespaceInfoService {

	NamespaceInfo selectByNamespace(String namespace);

	List<NamespaceInfo> selectAll();

	Integer batchInsert(List<NamespaceInfo> namespaceInfos);

	void deleteAll();

	void replaceAll(List<NamespaceInfo> namespaceInfos);
}
