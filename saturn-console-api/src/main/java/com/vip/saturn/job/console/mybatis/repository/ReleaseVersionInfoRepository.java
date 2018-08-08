package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.ReleaseVersionInfo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author timmy.hu
 */
@Repository
public interface ReleaseVersionInfoRepository {

	ReleaseVersionInfo selectByNamespace(String namespace);

	int insert(ReleaseVersionInfo releaseVersionInfo);

	List<ReleaseVersionInfo> selectAll();

	ReleaseVersionInfo selectByVersionNumber(String versionNumber);

	int selectInUsingNamespaceCount(String versionNumber);

	int deleteByVersionNumber(String versionNumber);

	/**
	 * 根据版本号更新
	 * @param releaseVersionInfo
	 * @return
	 */
	int updateByVersionNumber(ReleaseVersionInfo releaseVersionInfo);
}