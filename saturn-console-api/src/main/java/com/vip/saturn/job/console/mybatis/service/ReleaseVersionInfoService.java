package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.ReleaseVersionInfo;

import java.util.List;

/**
 * @author timmy.hu
 */
public interface ReleaseVersionInfoService {

	ReleaseVersionInfo selectByNamespace(String namespace);

	ReleaseVersionInfo selectByVersionNumber(String versionNumber);

	List<ReleaseVersionInfo> getVersions();

	int insert(ReleaseVersionInfo releaseVersionInfo) throws SaturnJobConsoleException;

	boolean isInUsing(String versionNumber);

	int deleteByVersionNumber(String versionNumber) throws SaturnJobConsoleException;

	/**
	 * update
	 * @param releaseVersionInfo
	 * @return
	 */
	int update(ReleaseVersionInfo releaseVersionInfo);
}
