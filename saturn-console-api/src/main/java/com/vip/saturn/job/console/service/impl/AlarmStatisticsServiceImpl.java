package com.vip.saturn.job.console.service.impl;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.mybatis.service.SaturnStatisticsService;
import com.vip.saturn.job.console.service.AlarmStatisticsService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.helper.DashboardServiceHelper;
import com.vip.saturn.job.console.utils.StatisticsTableKeyConstant;
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
	private SaturnStatisticsService saturnStatisticsService;

	@Override
	public String getAbnormalJobs() throws SaturnJobConsoleException {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = getAbnormalJobs(zkCluster.getZkClusterKey());
			List<AbnormalJob> tempList = JSON.parseArray(result, AbnormalJob.class);
			if (tempList != null) {
				abnormalJobList.addAll(tempList);
			}
		}
		return JSON.toJSONString(DashboardServiceHelper.sortUnnormaoJobByTimeDesc(abnormalJobList));
	}

	@Override
	public String getUnableFailoverJobs() throws SaturnJobConsoleException {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = getUnableFailoverJobs(zkCluster.getZkClusterKey());
			List<AbnormalJob> tempList = JSON.parseArray(result, AbnormalJob.class);
			if (tempList != null) {
				abnormalJobList.addAll(tempList);
			}

		}
		return JSON.toJSONString(abnormalJobList);
	}

	@Override
	public String getTimeout4AlarmJobs() throws SaturnJobConsoleException {
		List<Timeout4AlarmJob> timeout4AlarmJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = getAbnormalJobs(zkCluster.getZkClusterKey());
			List<Timeout4AlarmJob> tempList = JSON.parseArray(result, Timeout4AlarmJob.class);
			if (tempList != null) {
				timeout4AlarmJobList.addAll(tempList);
			}
		}
		return JSON.toJSONString(timeout4AlarmJobList);
	}

	@Override
	public String getAbnormalContainers() throws SaturnJobConsoleException {
		List<AbnormalContainer> abnormalContainerList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = getAbnormalContainers(zkCluster.getZkClusterKey());
			List<AbnormalContainer> tempList = JSON.parseArray(result, AbnormalContainer.class);
			if (tempList != null) {
				abnormalContainerList.addAll(tempList);
			}
		}
		return JSON.toJSONString(abnormalContainerList);
	}

	@Override
	public void setAbnormalJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException {
		if (StringUtils.isBlank(uuid)) {
			throw new SaturnJobConsoleException("uuid不能为空");
		}
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zkCluster.getZkAddr());
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
						return;
					}
				}
			}
		}
		throw new SaturnJobConsoleException(String.format("该uuid(%s)不存在", uuid));
	}

	@Override
	public void setTimeout4AlarmJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException {
		if (StringUtils.isBlank(uuid)) {
			throw new SaturnJobConsoleException("uuid不能为空");
		}
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = saturnStatisticsService
					.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB,
							zkCluster.getZkAddr());
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
						return;
					}
				}
			}
		}
		throw new SaturnJobConsoleException(String.format("该uuid(%s)不存在", uuid));
	}

	private ZkCluster validateAndGetZKCluster(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
		if (zkCluster == null) {
			throw new SaturnJobConsoleException(String.format("该集群key(%s)不存在", zkClusterKey));
		}
		return zkCluster;
	}

	@Override
	public String getAbnormalJobs(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zkCluster.getZkAddr());
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	@Override
	public String getUnableFailoverJobs(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB, zkCluster.getZkAddr());
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	@Override
	public String getTimeout4AlarmJobs(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, zkCluster.getZkAddr());
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	@Override
	public String getAbnormalContainers(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.ABNORMAL_CONTAINER, zkCluster.getZkAddr());
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	private RegistryCenterConfiguration validateAndGetConf(String namespace) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = registryCenterService.findConfigByNamespace(namespace);
		if (conf == null) {
			throw new SaturnJobConsoleException(String.format("该域(%s)不存在", namespace));
		}
		return conf;
	}

	@Override
	public String getAbnormalJobsByNamespace(String namespace) throws SaturnJobConsoleException {
		List<AbnormalJob> jobsByNamespace = new ArrayList<>();
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getAbnormalContainers(conf.getZkClusterKey());
		List<AbnormalJob> jobs = JSON.parseArray(result, AbnormalJob.class);
		if (jobs != null) {
			for (AbnormalJob job : jobs) {
				if (namespace.equals(job.getDomainName())) {
					jobsByNamespace.add(job);
				}
			}
		}
		return JSON.toJSONString(jobsByNamespace);
	}

	@Override
	public String getUnableFailoverJobsByNamespace(String namespace) throws SaturnJobConsoleException {
		List<AbnormalJob> jobsByNamespace = new ArrayList<>();
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getUnableFailoverJobs(conf.getZkClusterKey());
		List<AbnormalJob> jobs = JSON.parseArray(result, AbnormalJob.class);
		if (jobs != null) {
			for (AbnormalJob job : jobs) {
				if (namespace.equals(job.getDomainName())) {
					jobsByNamespace.add(job);
				}
			}
		}
		return JSON.toJSONString(jobsByNamespace);
	}

	@Override
	public String getTimeout4AlarmJobsByNamespace(String namespace) throws SaturnJobConsoleException {
		List<Timeout4AlarmJob> jobsByNamespace = new ArrayList<>();
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getTimeout4AlarmJobs(conf.getZkClusterKey());
		List<Timeout4AlarmJob> jobs = JSON.parseArray(result, Timeout4AlarmJob.class);
		if (jobs != null) {
			for (Timeout4AlarmJob job : jobs) {
				if (namespace.equals(job.getDomainName())) {
					jobsByNamespace.add(job);
				}
			}
		}
		return JSON.toJSONString(jobsByNamespace);
	}

	@Override
	public String getAbnormalContainersByNamespace(String namespace) throws SaturnJobConsoleException {
		List<AbnormalContainer> jobsByNamespace = new ArrayList<>();
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getAbnormalContainers(conf.getZkClusterKey());
		List<AbnormalContainer> jobs = JSON.parseArray(result, AbnormalContainer.class);
		if (jobs != null) {
			for (AbnormalContainer job : jobs) {
				if (namespace.equals(job.getDomainName())) {
					jobsByNamespace.add(job);
				}
			}
		}
		return JSON.toJSONString(jobsByNamespace);
	}

	@Override
	public boolean isAbnormalJob(String namespace, String jobName) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getAbnormalContainers(conf.getZkClusterKey());
		List<AbnormalJob> jobs = JSON.parseArray(result, AbnormalJob.class);
		if (jobs != null) {
			for (AbnormalJob job : jobs) {
				if (namespace.equals(job.getDomainName()) && jobName.equals(job.getJobName())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isUnableFailoverJob(String namespace, String jobName) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getUnableFailoverJobs(conf.getZkClusterKey());
		List<AbnormalJob> jobs = JSON.parseArray(result, AbnormalJob.class);
		if (jobs != null) {
			for (AbnormalJob job : jobs) {
				if (namespace.equals(job.getDomainName()) && jobName.equals(job.getJobName())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isTimeout4AlarmJob(String namespace, String jobName) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getTimeout4AlarmJobs(conf.getZkClusterKey());
		List<Timeout4AlarmJob> jobs = JSON.parseArray(result, Timeout4AlarmJob.class);
		if (jobs != null) {
			for (Timeout4AlarmJob job : jobs) {
				if (namespace.equals(job.getDomainName()) && jobName.equals(job.getJobName())) {
					return true;
				}
			}
		}
		return false;
	}
}
