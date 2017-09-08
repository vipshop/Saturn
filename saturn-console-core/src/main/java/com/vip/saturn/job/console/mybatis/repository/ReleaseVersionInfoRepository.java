package com.vip.saturn.job.console.mybatis.repository;

import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.ReleaseVersionInfo;

/**
 * 
 * @author timmy.hu
 */
@Repository
public interface ReleaseVersionInfoRepository {

	ReleaseVersionInfo selectByNamespace(String namespace);

	int insert(ReleaseVersionInfo releaseVersionInfo);

}
