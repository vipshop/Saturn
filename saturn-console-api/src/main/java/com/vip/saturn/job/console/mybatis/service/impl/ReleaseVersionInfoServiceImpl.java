/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.ReleaseVersionInfo;
import com.vip.saturn.job.console.mybatis.repository.ReleaseVersionInfoRepository;
import com.vip.saturn.job.console.mybatis.service.ReleaseVersionInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author timmy.hu
 */
@Service
@Transactional
public class ReleaseVersionInfoServiceImpl implements ReleaseVersionInfoService {

	@Autowired
	private ReleaseVersionInfoRepository releaseVersionInfoRepository;

	/*
	 * (non-Javadoc)
	 *
	 * @see com.vip.saturn.job.console.mybatis.service.ReleaseVersionInfoService# selectByNamespace(java.lang.String)
	 */
	@Override
	public ReleaseVersionInfo selectByNamespace(String namespace) {
		return releaseVersionInfoRepository.selectByNamespace(namespace);
	}

	@Override
	public int insert(ReleaseVersionInfo releaseVersionInfo) throws SaturnJobConsoleException {
		ReleaseVersionInfo oldVersionInfo = selectByVersionNumber(releaseVersionInfo.getVersionNumber());
		if (oldVersionInfo != null) {
			throw new SaturnJobConsoleException(
					"the version number " + releaseVersionInfo.getVersionNumber() + " has existed.");
		}
		releaseVersionInfo.setCreateTime(new Date());
		releaseVersionInfo.setLastUpdateTime(new Date());
		return releaseVersionInfoRepository.insert(releaseVersionInfo);
	}

	@Override
	public List<ReleaseVersionInfo> getVersions() {
		return releaseVersionInfoRepository.selectAll();
	}

	@Override
	public ReleaseVersionInfo selectByVersionNumber(String versionNumber) {
		return releaseVersionInfoRepository.selectByVersionNumber(versionNumber);
	}

	@Override
	public boolean isInUsing(String versionNumber) {
		int count = releaseVersionInfoRepository.selectInUsingNamespaceCount(versionNumber);
		return count > 0;
	}

	@Override
	public int deleteByVersionNumber(String versionNumber) throws SaturnJobConsoleException {
		ReleaseVersionInfo versionInfo = selectByVersionNumber(versionNumber);
		if (versionInfo == null) {
			throw new SaturnJobConsoleException("删除失败：不存在版本号为" + versionNumber + "的发布版本");
		}
		if (isInUsing(versionNumber)) {
			throw new SaturnJobConsoleException("删除失败：该版本号" + versionNumber + "已经在域的待升级版本设置中！");
		}
		return releaseVersionInfoRepository.deleteByVersionNumber(versionNumber);
	}

	@Override
	public int update(ReleaseVersionInfo releaseVersionInfo) {
		return releaseVersionInfoRepository.updateByVersionNumber(releaseVersionInfo);
	}

}
