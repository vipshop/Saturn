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

import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.repository.ZkClusterInfoRepository;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author hebelala
 */
@Service
public class ZkClusterInfoServiceImpl implements ZkClusterInfoService {

	@Autowired
	private ZkClusterInfoRepository zkClusterInfoRepository;

	@Transactional(readOnly = true)
	@Override
	public List<ZkClusterInfo> getAllZkClusterInfo() {
		return zkClusterInfoRepository.selectAll();
	}

	@Transactional(readOnly = true)
	@Override
	public ZkClusterInfo getByClusterKey(String clusterKey) {
		return zkClusterInfoRepository.selectByClusterKey(clusterKey);
	}

	@Transactional
	@Override
	public int createZkCluster(String clusterKey, String alias, String connectString, String description, String createdBy) {
		ZkClusterInfo zkClusterInfo = new ZkClusterInfo();
		Date now = new Date();
		zkClusterInfo.setCreateTime(now);
		zkClusterInfo.setCreatedBy(createdBy);
		zkClusterInfo.setLastUpdateTime(now);
		zkClusterInfo.setLastUpdatedBy(createdBy);
		zkClusterInfo.setZkClusterKey(clusterKey);
		zkClusterInfo.setAlias(alias);
		zkClusterInfo.setConnectString(connectString);
		zkClusterInfo.setDescription(description);
		return zkClusterInfoRepository.insert(zkClusterInfo);
	}

	@Transactional
	@Override
	public int updateZkCluster(ZkClusterInfo zkClusterInfo) {
		return zkClusterInfoRepository.update(zkClusterInfo);
	}

	@Transactional
	@Override
	public int deleteZkCluster(String zkClusterKey) {
		return zkClusterInfoRepository.deleteByClusterKey(zkClusterKey);
	}
}
