/**
 * 
 */
package com.vip.saturn.job.console.mybatis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vip.saturn.job.console.mybatis.entity.ReleaseVersionInfo;
import com.vip.saturn.job.console.mybatis.repository.ReleaseVersionInfoRepository;
import com.vip.saturn.job.console.mybatis.service.ReleaseVersionInfoService;

/**
 * @author timmy.hu
 *
 */
@Service
public class ReleaseVersionInfoServiceImpl implements ReleaseVersionInfoService {

	@Autowired
	private ReleaseVersionInfoRepository releaseVersionInfoRepository;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.vip.saturn.job.console.mybatis.service.ReleaseVersionInfoService#
	 * selectByNamespace(java.lang.String)
	 */
	@Override
	public ReleaseVersionInfo selectByNamespace(String namespace) {
		return releaseVersionInfoRepository.selectByNamespace(namespace);
	}

	@Override
	public int insert(ReleaseVersionInfo releaseVersionInfo) {
		return releaseVersionInfoRepository.insert(releaseVersionInfo);
	}

}
