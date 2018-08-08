package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.NamespaceVersionMapping;

import java.util.List;

/**
 * @author timmy.hu
 */
public interface NamespaceVersionMappingService {

	int insertOrUpdate(String namespace, String versionNumber, boolean isForced, String who);

	List<NamespaceVersionMapping> selectAllWithNotDeleted();

	NamespaceVersionMapping selectByNamespace(String namespace);


	/**
	 * 删除绑定
	 * @param namespace
	 * @param versionNumber
	 * @return
	 */
	int deleteMapping(String namespace, String versionNumber);

}
