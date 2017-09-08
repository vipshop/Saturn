package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.ReleaseVersionInfo;

/**
 * 
 * @author timmy.hu
 *
 */
public interface ReleaseVersionInfoService {

	ReleaseVersionInfo selectByNamespace(String namespace);
	
	int insert(ReleaseVersionInfo releaseVersionInfo);
}
