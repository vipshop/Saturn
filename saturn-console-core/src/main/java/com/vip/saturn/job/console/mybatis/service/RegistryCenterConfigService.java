package com.vip.saturn.job.console.mybatis.service;

import java.util.List;

import com.vip.saturn.job.console.mybatis.entity.RegistryCenterConfig;

/**
 * 
 * @author hebelala
 *
 */
public interface RegistryCenterConfigService {

	String getConnectString(String namespace);

	List<RegistryCenterConfig> selectAll();

	int insert(String namespace, String connectString, String createdBy);

	int update(String namespace, String connectString, String lastUpdatedBy);
	
	int remove(String namespace, String lastUpdatedBy);

}
