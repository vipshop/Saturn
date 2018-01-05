package com.vip.saturn.job.console.service.impl;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.console.domain.AbnormalContainer;
import com.vip.saturn.job.console.domain.AbnormalJob;
import com.vip.saturn.job.console.domain.Timeout4AlarmJob;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.service.AlarmStatisticsService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.ZkClusterAlarmStatisticsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author hebelala
 */
@Service
public class AlarmStatisticsServiceImpl implements AlarmStatisticsService {

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private ZkClusterAlarmStatisticsService zkClusterAlarmStatisticsService;

	@Override
	public String getAbnormalJobs() {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = zkClusterAlarmStatisticsService.getAbnormalJobs(zkCluster.getZkAddr());
			List<AbnormalJob> tempList = JSON.parseArray(result, AbnormalJob.class);
			if (tempList != null) {
				abnormalJobList.addAll(tempList);
			}
		}
		return JSON.toJSONString(abnormalJobList);
	}

	@Override
	public String getUnableFailoverJobs() {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = zkClusterAlarmStatisticsService.getUnableFailoverJobs(zkCluster.getZkAddr());
			List<AbnormalJob> tempList = JSON.parseArray(result, AbnormalJob.class);
			if (tempList != null) {
				abnormalJobList.addAll(tempList);
			}

		}
		return JSON.toJSONString(abnormalJobList);
	}

	@Override
	public String getTimeout4AlarmJobs() {
		List<Timeout4AlarmJob> timeout4AlarmJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = zkClusterAlarmStatisticsService.getAbnormalJobs(zkCluster.getZkAddr());
			List<Timeout4AlarmJob> tempList = JSON.parseArray(result, Timeout4AlarmJob.class);
			if (tempList != null) {
				timeout4AlarmJobList.addAll(tempList);
			}
		}
		return JSON.toJSONString(timeout4AlarmJobList);
	}

	@Override
	public String getAbnormalContainers() {
		List<AbnormalContainer> abnormalContainerList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = zkClusterAlarmStatisticsService.getAbnormalContainers(zkCluster.getZkAddr());
			List<AbnormalContainer> tempList = JSON.parseArray(result, AbnormalContainer.class);
			if (tempList != null) {
				abnormalContainerList.addAll(tempList);
			}
		}
		return JSON.toJSONString(abnormalContainerList);
	}

	@Override
	public boolean setAbnormalJobMonitorStatusToRead(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return false;
		}
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			boolean success = zkClusterAlarmStatisticsService
					.setAbnormalJobMonitorStatusToRead(zkCluster.getZkAddr(), uuid);
			if (success) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean setTimeout4AlarmJobMonitorStatusToRead(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return false;
		}
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			boolean success = zkClusterAlarmStatisticsService
					.setTimeout4AlarmJobMonitorStatusToRead(zkCluster.getZkAddr(), uuid);
			if (success) {
				return true;
			}
		}
		return false;
	}
}
