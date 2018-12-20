package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.repository.ZkClusterInfoRepository;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static com.vip.saturn.job.console.service.impl.RegistryCenterServiceImpl.DEFAULT_CONSOLE_CLUSTER_ID;

/**
 * @author hebelala
 */
@Service
public class ZkClusterInfoServiceImpl implements ZkClusterInfoService {

	@Autowired
	private ZkClusterInfoRepository zkClusterInfoRepository;

	@Autowired
	private SystemConfigService systemConfigService;

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
	public int createZkCluster(String clusterKey, String alias, String connectString, String description,
			String createdBy) {
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

	@Override
	public void updateConsoleZKClusterMapping(String zkClusterKey) throws SaturnJobConsoleException {

		String consoleClusterId = getConsoleClusterId(zkClusterKey);

		String consoleZkClusterMapping = systemConfigService
				.getValue(SystemConfigProperties.CONSOLE_ZK_CLUSTER_MAPPING);

		String[] items = splitItems(consoleZkClusterMapping, consoleClusterId);
		String updatedConsoleZkClusterMapping = updateMapping(items, zkClusterKey, consoleClusterId);

		SystemConfig systemConfig = new SystemConfig();
		systemConfig.setProperty(SystemConfigProperties.CONSOLE_ZK_CLUSTER_MAPPING);
		systemConfig.setValue(updatedConsoleZkClusterMapping);
		systemConfigService.updateConfig(systemConfig);
	}

	private String updateMapping(String[] items, String zkClusterKey, String consoleClusterId) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < items.length; i++) {
			String[] keyValue = items[i].split(":");
			String clusterId = keyValue[0];
			String origin = items[i];
			if (StringUtils.equals(clusterId, consoleClusterId)) {
				if (keyValue.length == 1) {
					origin = origin + zkClusterKey;
				} else {
					origin = origin + "," + zkClusterKey;
				}
			}
			sb.append(origin);
			sb.append(";");
		}
		return sb.toString();
	}

	private String getConsoleClusterId(String zkClusterKey) {
		String consoleClusterId = null;
		if (StringUtils.isBlank(SaturnEnvProperties.VIP_SATURN_CONSOLE_CLUSTER_ID)) {
			consoleClusterId = DEFAULT_CONSOLE_CLUSTER_ID;
		} else {
			consoleClusterId = SaturnEnvProperties.VIP_SATURN_CONSOLE_CLUSTER_ID;
		}
		return consoleClusterId;
	}

	private String[] splitItems(String consoleZkClusterMapping, String consoleClusterId) {
		String mapping[] = null;
		if (StringUtils.isNotBlank(consoleZkClusterMapping)) {
			mapping = consoleZkClusterMapping.split(";");
		} else {
			mapping = new String[1];
			mapping[0] = consoleClusterId + ":";
		}
		return mapping;
	}

}
