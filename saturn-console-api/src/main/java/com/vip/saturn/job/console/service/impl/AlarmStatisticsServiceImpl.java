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

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author hebelala
 */
public class AlarmStatisticsServiceImpl implements AlarmStatisticsService {

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private SaturnStatisticsService saturnStatisticsService;

	@Override
	public String getAbnormalJobsString() throws SaturnJobConsoleException {
		List<AbnormalJob> abnormalJobList = getAbnormalJobList();
		return JSON.toJSONString(DashboardServiceHelper.sortUnnormaoJobByTimeDesc(abnormalJobList));
	}

	@Override
	public String getUnableFailoverJobsString() throws SaturnJobConsoleException {
		List<AbnormalJob> abnormalJobList = getUnableFailoverJobList();
		return JSON.toJSONString(abnormalJobList);
	}

	@Override
	public String getTimeout4AlarmJobsString() throws SaturnJobConsoleException {
		List<Timeout4AlarmJob> timeout4AlarmJobList = getTimeout4AlarmJobList();
		return JSON.toJSONString(timeout4AlarmJobList);
	}

	@Override
	public List<AlarmJobCount> getCountOfAlarmJobs() throws SaturnJobConsoleException {
		List<AlarmJobCount> alarmJobCountList = new ArrayList<>();
		alarmJobCountList.add(new AlarmJobCount(StatisticsTableKeyConstant.UNNORMAL_JOB, getAbnormalJobList().size()));
		alarmJobCountList.add(
				new AlarmJobCount(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB, getUnableFailoverJobList().size()));
		alarmJobCountList.add(
				new AlarmJobCount(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, getTimeout4AlarmJobList().size()));
		return alarmJobCountList;
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
		setAlarmJobMonitorStatusToRead(uuid, StatisticsTableKeyConstant.UNNORMAL_JOB, AbnormalJob.class);
	}

	@Override
	public void setTimeout4AlarmJobMonitorStatusToRead(String uuid) throws SaturnJobConsoleException {
		if (StringUtils.isBlank(uuid)) {
			throw new SaturnJobConsoleException("uuid不能为空");
		}
		setAlarmJobMonitorStatusToRead(uuid, StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB, Timeout4AlarmJob.class);
	}

	public List<AbnormalJob> getAbnormalJobList() throws SaturnJobConsoleException {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = getAbnormalJobsStringByZKCluster(zkCluster.getZkClusterKey());
			List<AbnormalJob> tempList = JSON.parseArray(result, AbnormalJob.class);
			if (tempList != null) {
				abnormalJobList.addAll(tempList);
			}
		}
		return abnormalJobList;
	}

	public List<AbnormalJob> getUnableFailoverJobList() throws SaturnJobConsoleException {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = getUnableFailoverJobsStringByZKCluster(zkCluster.getZkClusterKey());
			List<AbnormalJob> tempList = JSON.parseArray(result, AbnormalJob.class);
			if (tempList != null) {
				abnormalJobList.addAll(tempList);
			}

		}
		return abnormalJobList;
	}

	public List<Timeout4AlarmJob> getTimeout4AlarmJobList() throws SaturnJobConsoleException {
		List<Timeout4AlarmJob> timeout4AlarmJobList = new ArrayList<>();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			String result = getTimeout4AlarmJobsStringByZKCluster(zkCluster.getZkClusterKey());
			List<Timeout4AlarmJob> tempList = JSON.parseArray(result, Timeout4AlarmJob.class);
			if (tempList != null) {
				timeout4AlarmJobList.addAll(tempList);
			}
		}
		return timeout4AlarmJobList;
	}

	private <T extends AbstractAlarmJob> void setAlarmJobMonitorStatusToRead(String uuid, String alarmJobType,
			Class<T> t) throws SaturnJobConsoleException {
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			SaturnStatistics saturnStatistics = saturnStatisticsService.findStatisticsByNameAndZkList(alarmJobType,
					zkCluster.getZkAddr());
			if (null == saturnStatistics) {
				continue;
			}
			String result = saturnStatistics.getResult();
			List<T> jobs = JSON.parseArray(result, t);
			if (jobs != null) {
				boolean find = false;
				for (T job : jobs) {
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
		throw new SaturnJobConsoleException(String.format("该uuid(%s)不存在", uuid));
	}

	private ZkCluster validateAndGetZKCluster(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
		if (zkCluster == null) {
			throw new SaturnJobConsoleException(String.format("该集群key(%s)不存在", zkClusterKey));
		}
		return zkCluster;
	}

	public String getAbnormalJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNNORMAL_JOB, zkCluster.getZkAddr());
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	@Override
	public String getUnableFailoverJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = validateAndGetZKCluster(zkClusterKey);
		SaturnStatistics saturnStatistics = saturnStatisticsService
				.findStatisticsByNameAndZkList(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB, zkCluster.getZkAddr());
		return saturnStatistics != null ? saturnStatistics.getResult() : null;
	}

	@Override
	public String getTimeout4AlarmJobsStringByZKCluster(String zkClusterKey) throws SaturnJobConsoleException {
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
	public List<AbnormalJob> getAbnormalJobListByNamespace(String namespace) throws SaturnJobConsoleException {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getAbnormalJobsStringByZKCluster(conf.getZkClusterKey());
		abnormalJobList.addAll(getAlarmJobListFilterByNamespace(namespace, result, AbnormalJob.class));
		return abnormalJobList;
	}

	@Override
	public String getAbnormalJobsStringByNamespace(String namespace) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getAbnormalJobsStringByZKCluster(conf.getZkClusterKey());
		return JSON.toJSONString(getAlarmJobListFilterByNamespace(namespace, result, AbnormalJob.class));
	}

	public List<AbnormalJob> getUnableFailoverListByNamespace(String namespace) throws SaturnJobConsoleException {
		List<AbnormalJob> abnormalJobList = new ArrayList<>();
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getUnableFailoverJobsStringByZKCluster(conf.getZkClusterKey());
		abnormalJobList.addAll(getAlarmJobListFilterByNamespace(namespace, result, AbnormalJob.class));
		return abnormalJobList;
	}

	@Override
	public String getUnableFailoverJobsStringByNamespace(String namespace) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getUnableFailoverJobsStringByZKCluster(conf.getZkClusterKey());
		return JSON.toJSONString(getAlarmJobListFilterByNamespace(namespace, result, AbnormalJob.class));
	}

	public List<Timeout4AlarmJob> getTimeout4AlarmJobListByNamespace(String namespace)
			throws SaturnJobConsoleException {
		List<Timeout4AlarmJob> timeout4AlarmJobs = new ArrayList<>();
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getTimeout4AlarmJobsStringByZKCluster(conf.getZkClusterKey());
		timeout4AlarmJobs.addAll(getAlarmJobListFilterByNamespace(namespace, result, Timeout4AlarmJob.class));
		return timeout4AlarmJobs;
	}

	@Override
	public String getTimeout4AlarmJobsStringByNamespace(String namespace) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getTimeout4AlarmJobsStringByZKCluster(conf.getZkClusterKey());
		return JSON.toJSONString(getAlarmJobListFilterByNamespace(namespace, result, Timeout4AlarmJob.class));
	}

	@Override
	public List<AlarmJobCount> getCountOfAlarmJobsByNamespace(String namespace) throws SaturnJobConsoleException {
		List<AlarmJobCount> alarmJobCountList = new ArrayList<>();
		alarmJobCountList.add(new AlarmJobCount(StatisticsTableKeyConstant.UNNORMAL_JOB,
				getAbnormalJobListByNamespace(namespace).size()));
		alarmJobCountList.add(new AlarmJobCount(StatisticsTableKeyConstant.UNABLE_FAILOVER_JOB,
				getUnableFailoverListByNamespace(namespace).size()));
		alarmJobCountList.add(new AlarmJobCount(StatisticsTableKeyConstant.TIMEOUT_4_ALARM_JOB,
				getTimeout4AlarmJobListByNamespace(namespace).size()));
		return alarmJobCountList;
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
	public AbnormalJob isAbnormalJob(String namespace, String jobName) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getAbnormalJobsStringByZKCluster(conf.getZkClusterKey());
		return getAlarmJobFilterByNamespaceAndJobName(namespace, jobName, result, AbnormalJob.class);
	}

	@Override
	public AbnormalJob isUnableFailoverJob(String namespace, String jobName) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getUnableFailoverJobsStringByZKCluster(conf.getZkClusterKey());
		return getAlarmJobFilterByNamespaceAndJobName(namespace, jobName, result, AbnormalJob.class);
	}

	@Override
	public Timeout4AlarmJob isTimeout4AlarmJob(String namespace, String jobName) throws SaturnJobConsoleException {
		RegistryCenterConfiguration conf = validateAndGetConf(namespace);
		String result = getTimeout4AlarmJobsStringByZKCluster(conf.getZkClusterKey());
		return getAlarmJobFilterByNamespaceAndJobName(namespace, jobName, result, Timeout4AlarmJob.class);
	}

	private <T extends AbstractAlarmJob> List<T> getAlarmJobListFilterByNamespace(String namespace, String result,
			Class<T> t) {
		List<T> jobsByNamespace = new ArrayList<>();
		List<T> jobs = JSON.parseArray(result, t);
		if (jobs != null) {
			for (T job : jobs) {
				if (namespace.equals(job.getDomainName())) {
					jobsByNamespace.add(job);
				}
			}
		}
		return jobsByNamespace;
	}

	private <T extends AbstractAlarmJob> T getAlarmJobFilterByNamespaceAndJobName(String namespace, String jobName,
			String result, Class<T> t) {
		List<T> jobs = JSON.parseArray(result, t);
		if (jobs != null) {
			for (T job : jobs) {
				if (namespace.equals(job.getDomainName()) && jobName.equals(job.getJobName())) {
					return job;
				}
			}
		}
		return null;
	}
}
