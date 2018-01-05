package com.vip.saturn.job.console.service.impl;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.console.domain.AbnormalJob;
import com.vip.saturn.job.console.domain.Timeout4AlarmJob;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.mybatis.service.SaturnStatisticsService;
import com.vip.saturn.job.console.service.ZkClusterAlarmStatisticsService;
import com.vip.saturn.job.console.utils.StatisticsTableKeyConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author hebelala
 */
@Service
public class ZkClusterAlarmStatisticsServiceImpl implements ZkClusterAlarmStatisticsService {

	@Resource
	private SaturnStatisticsService saturnStatisticsService;

	@Override
	public String getAbnormalJobs(String zkList) {
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zkList);
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	@Override
	public String getUnableFailoverJobs(String zkList) {
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB, zkList);
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	@Override
	public String getTimeout4AlarmJobs(String zkList) {
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, zkList);
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	@Override
	public String getAbnormalContainers(String zkList) {
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.ABNORMAL_CONTAINER, zkList);
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	@Override
	public boolean setAbnormalJobMonitorStatusToRead(String zkList, String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return false;
		}
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zkList);
		if (saturnStatistics != null) {
			String result = saturnStatistics.getResult();
			List<AbnormalJob> jobs = JSON.parseArray(result, AbnormalJob.class);
			if (jobs != null) {
				boolean find = false;
				for (AbnormalJob job : jobs) {
					if (uuid.equals(job.getUuid())) {
						job.setRead(true);
						find = true;
						break;
					}
				}
				if (find) {
					saturnStatistics.setResult(JSON.toJSONString(jobs));
					saturnStatisticsService.updateByPrimaryKeySelective(saturnStatistics);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean setTimeout4AlarmJobMonitorStatusToRead(String zkList, String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return false;
		}
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, zkList);
		if (saturnStatistics != null) {
			String result = saturnStatistics.getResult();
			List<Timeout4AlarmJob> jobs = JSON.parseArray(result, Timeout4AlarmJob.class);
			if (jobs != null) {
				boolean find = false;
				for (Timeout4AlarmJob job : jobs) {
					if (uuid.equals(job.getUuid())) {
						job.setRead(true);
						find = true;
						break;
					}
				}
				if (find) {
					saturnStatistics.setResult(JSON.toJSONString(jobs));
					saturnStatisticsService.updateByPrimaryKeySelective(saturnStatistics);
					return true;
				}
			}
		}
		return false;
	}
}
