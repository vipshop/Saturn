package com.vip.saturn.job.console.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.vip.saturn.job.console.domain.JobBriefInfo.JobType;
import com.vip.saturn.job.console.domain.ExportJobConfigPageStatus;
import com.vip.saturn.job.console.domain.JobMode;
import com.vip.saturn.job.console.domain.JobSettings;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.entity.TemporarySharedStatus;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.mybatis.service.TemporarySharedStatusService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobConfigInitializationService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import com.vip.saturn.job.console.utils.ShareStatusModuleNames;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;

/**
 * @author timmy.hu
 */
@Service
public class JobConfigInitializationServiceImpl implements JobConfigInitializationService {

	private static final Logger log = LoggerFactory.getLogger(ExecutorServiceImpl.class);

	private final static int MAX_DELETE_NUM = 2000;

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	protected RegistryCenterService registryCenterService;

	@Resource
	private CurrentJobConfigService currentJobConfigService;

	@Resource
	private TemporarySharedStatusService temporarySharedStatusService;

	private Gson gson = new Gson();
	private MapperFacade mapper;
	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	@Autowired
	public void setMapperFactory(MapperFactory mapperFactory) {
		this.mapper = mapperFactory.getMapperFacade();
	}

	@PreDestroy
	public void destroy() {
		if (executorService != null) {
			executorService.shutdownNow();
		}
	}

	@Override
	public List<RegistryCenterConfiguration> getRegistryCenterConfigurations() throws SaturnJobConsoleException {
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		List<RegistryCenterConfiguration> rccs = new ArrayList<>();
		if (zkClusterList != null) {
			for (ZkCluster zkCluster : zkClusterList) {
				if (zkCluster != null && !zkCluster.isOffline()) {
					rccs.addAll(zkCluster.getRegCenterConfList());
				}
			}
		}
		return rccs;
	}

	@Override
	public void exportAllToDb(final String userName) throws SaturnJobConsoleException {
		final ExportJobConfigPageStatus exportJobConfigPageStatus = new ExportJobConfigPageStatus();
		temporarySharedStatusService.delete(ShareStatusModuleNames.EXPORT_JOB_CONFIG_PAGE_STATUS);
		temporarySharedStatusService.create(ShareStatusModuleNames.EXPORT_JOB_CONFIG_PAGE_STATUS,
				gson.toJson(exportJobConfigPageStatus));
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				log.info("start to export all to db");
				try {
					log.info("start to delete all from table job_config");
					deleteAll();
					log.info("delete all from table job_config successfully");
					Collection<ZkCluster> zkClusters = registryCenterService.getZkClusterList();
					if (zkClusters != null) {
						for (ZkCluster tmp : zkClusters) {
							exportToDbByZkCluster(userName, tmp, exportJobConfigPageStatus);
						}
					}
					exportJobConfigPageStatus.setSuccess(true);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					exportJobConfigPageStatus.setSuccess(false);
				} finally {
					exportJobConfigPageStatus.setExported(true);
					temporarySharedStatusService.update(ShareStatusModuleNames.EXPORT_JOB_CONFIG_PAGE_STATUS,
							gson.toJson(exportJobConfigPageStatus));
				}
			}
		});
	}

	private void deleteAll() {
		int deleteNum = 0;
		while (true) {
			log.info(" begin to delete {} jobconfigs from table job_config successfully", MAX_DELETE_NUM);
			deleteNum = currentJobConfigService.deleteAll(MAX_DELETE_NUM);
			log.info(" successfully delete {} jobconfigs from table job_config successfully", deleteNum);
			if (deleteNum < MAX_DELETE_NUM) {
				return;
			}
			try {
				TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private void exportToDbByZkCluster(String userName, ZkCluster tmp,
			ExportJobConfigPageStatus exportJobConfigPageStatus) throws SaturnJobConsoleException {
		log.info(" start to export db by single zkCluster, zkCluster Addr is :{}", tmp.getZkAddr());
		ZkCluster zkCluster = tmp;
		ArrayList<RegistryCenterConfiguration> oldRccs = zkCluster.getRegCenterConfList();
		ArrayList<RegistryCenterConfiguration> rccs = new ArrayList<RegistryCenterConfiguration>(oldRccs);
		for (RegistryCenterConfiguration rcc : rccs) {
			List<String> jobNames = getAllUnSystemJobs(rcc.getNamespace(), zkCluster.getCuratorFramework());
			for (String jobName : jobNames) {
				try {
					JobSettings jobSettings = getJobSettings(jobName, rcc, zkCluster.getCuratorFramework());
					CurrentJobConfig current = mapper.map(jobSettings, CurrentJobConfig.class);
					current.setCreateBy(userName);
					current.setCreateTime(new Date());
					current.setLastUpdateBy(userName);
					current.setLastUpdateTime(new Date());
					current.setNamespace(rcc.getNamespace());
					currentJobConfigService.create(current);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					throw new SaturnJobConsoleException(e.getMessage());
				}
			}
			exportJobConfigPageStatus.setSuccessJobNum(exportJobConfigPageStatus.getSuccessJobNum() + jobNames.size());
			exportJobConfigPageStatus.setSuccessNamespaceNum(exportJobConfigPageStatus.getSuccessNamespaceNum() + 1);
			temporarySharedStatusService.update(ShareStatusModuleNames.EXPORT_JOB_CONFIG_PAGE_STATUS,
					gson.toJson(exportJobConfigPageStatus));
		}
		log.info("export db by single zkCluster successfully, zkCluster Addr is :{}", tmp.getZkAddr());
	}

	private JobSettings getJobSettings(final String jobName, RegistryCenterConfiguration rcc,
			CuratorFramework curatorFramework) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository
				.newCuratorFrameworkOp(curatorFramework);
		JobSettings result = new JobSettings();
		result.setJobName(jobName);
		result.setJobClass(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "jobClass"))));
		result.setShardingTotalCount(
				parseInt(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
						JobNodePath.getConfigNodePath(jobName, "shardingTotalCount")))));
		String timeZone = curatorFrameworkOp.getData(
				getConfigNodePathWithNamespace(rcc.getNamespace(), JobNodePath.getConfigNodePath(jobName, "timeZone")));
		if (Strings.isNullOrEmpty(timeZone)) {
			result.setTimeZone(SaturnConstants.TIME_ZONE_ID_DEFAULT);
		} else {
			result.setTimeZone(timeZone);
		}
		result.setTimeZonesProvided(Arrays.asList(TimeZone.getAvailableIDs()));
		result.setCron(curatorFrameworkOp.getData(
				getConfigNodePathWithNamespace(rcc.getNamespace(), JobNodePath.getConfigNodePath(jobName, "cron"))));
		result.setCustomContext(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "customContext"))));
		result.setPausePeriodDate(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "pausePeriodDate"))));
		result.setPausePeriodTime(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "pausePeriodTime"))));
		result.setShardingItemParameters(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "shardingItemParameters"))));
		result.setJobParameter(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "jobParameter"))));
		result.setProcessCountIntervalSeconds(
				parseInt(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
						JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds")))));
		String timeout4AlarmSecondsStr = curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "timeout4AlarmSeconds")));
		if (Strings.isNullOrEmpty(timeout4AlarmSecondsStr)) {
			result.setTimeout4AlarmSeconds(0);
		} else {
			result.setTimeout4AlarmSeconds(parseInt(timeout4AlarmSecondsStr));
		}
		result.setTimeoutSeconds(parseInt(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "timeoutSeconds")))));
		String lv = curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "loadLevel")));
		if (Strings.isNullOrEmpty(lv)) {
			result.setLoadLevel(1);
		} else {
			result.setLoadLevel(parseInt(lv));
		}
		String jobDegree = curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "jobDegree")));
		if (Strings.isNullOrEmpty(jobDegree)) {
			result.setJobDegree(0);
		} else {
			result.setJobDegree(parseInt(jobDegree));
		}
		result.setEnabled(Boolean.valueOf(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "enabled")))));// 默认是禁用的
		result.setPreferList(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "preferList"))));
		String useDispreferList = curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "useDispreferList")));
		if (Strings.isNullOrEmpty(useDispreferList)) {
			result.setUseDispreferList(null);
		} else {
			result.setUseDispreferList(Boolean.valueOf(useDispreferList));
		}
		result.setUseSerial(
				Boolean.valueOf(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
						JobNodePath.getConfigNodePath(jobName, "useSerial")))));
		result.setLocalMode(
				Boolean.valueOf(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
						JobNodePath.getConfigNodePath(jobName, "localMode")))));
		result.setFailover(Boolean.valueOf(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "failover")))));
		result.setDependencies(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "dependencies"))));
		result.setGroups(curatorFrameworkOp.getData(
				getConfigNodePathWithNamespace(rcc.getNamespace(), JobNodePath.getConfigNodePath(jobName, "groups"))));
		result.setDescription(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "description"))));
		result.setJobMode(curatorFrameworkOp.getData(
				getConfigNodePathWithNamespace(rcc.getNamespace(), JobNodePath.getConfigNodePath(jobName, "jobMode"))));
		result.setQueueName(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "queueName"))));
		result.setChannelName(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "channelName"))));
		String jobType = curatorFrameworkOp.getData(
				getConfigNodePathWithNamespace(rcc.getNamespace(), JobNodePath.getConfigNodePath(jobName, "jobType")));
		result.setJobType(jobType);
		String enabledReport = curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
				JobNodePath.getConfigNodePath(jobName, "enabledReport")));
		Boolean enabledReportValue = Boolean.valueOf(enabledReport);
		if (Strings.isNullOrEmpty(enabledReport)) {
			if (JobType.JAVA_JOB.name().equals(jobType) || JobType.SHELL_JOB.name().equals(jobType)) {
				enabledReportValue = true;
			} else {
				enabledReportValue = false;
			}
		}
		result.setEnabledReport(enabledReportValue);
		// 兼容旧版没有msg_job。
		if (StringUtils.isBlank(result.getJobType())) {
			if (result.getJobClass() != null) {
				if (result.getJobClass().indexOf("script") > 0) {
					result.setJobType(JobType.SHELL_JOB.name());
				} else {
					result.setJobType(JobType.JAVA_JOB.name());
				}
			} else {
				result.setJobType(JobType.JAVA_JOB.name());
			}

		}
		result.setShowNormalLog(
				Boolean.valueOf(curatorFrameworkOp.getData(getConfigNodePathWithNamespace(rcc.getNamespace(),
						JobNodePath.getConfigNodePath(jobName, "showNormalLog")))));
		return result;
	}

	private String getConfigNodePathWithNamespace(String namespace, String configNodePath) {
		return "/" + namespace + configNodePath;
	}

	private List<String> getAllUnSystemJobs(String namespace, CuratorFramework curatorFramework)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository
				.newCuratorFrameworkOp(curatorFramework);
		List<String> allJobs = getAllJobs(namespace, curatorFramework);
		Iterator<String> iterator = allJobs.iterator();
		while (iterator.hasNext()) {
			String job = iterator.next();
			String jobMode = getConfigNodePathWithNamespace(namespace, JobNodePath.getConfigNodePath(job, "jobMode"));
			if (curatorFrameworkOp.checkExists(jobMode)) {
				String data = curatorFrameworkOp.getData(jobMode);
				if (data != null && data.startsWith(JobMode.SYSTEM_PREFIX)) {
					iterator.remove();
				}
			}
		}
		return allJobs;
	}

	private List<String> getAllJobs(String namespace, CuratorFramework curatorFramework)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository
				.newCuratorFrameworkOp(curatorFramework);
		List<String> allJobs = new ArrayList<>();
		String jobsNodePath = getConfigNodePathWithNamespace(namespace, JobNodePath.get$JobsNodePath());
		if (curatorFrameworkOp.checkExists(jobsNodePath)) {
			List<String> jobs = curatorFrameworkOp.getChildren(jobsNodePath);
			if (jobs != null && jobs.size() > 0) {
				for (String job : jobs) {
					if (curatorFrameworkOp.checkExists(
							getConfigNodePathWithNamespace(namespace, JobNodePath.getConfigNodePath(job)))) {// 如果config节点存在才视为正常作业，其他异常作业在其他功能操作时也忽略
						allJobs.add(job);
					}
				}
			}
		}
		Collections.sort(allJobs);
		return allJobs;
	}

	private int parseInt(String str) {
		if (StringUtils.isEmpty(str)) {
			return 0;
		}
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return 0;
		}
	}

	@Override
	public ExportJobConfigPageStatus getStatus() throws SaturnJobConsoleException {
		TemporarySharedStatus temporarySharedStatus = temporarySharedStatusService
				.get(ShareStatusModuleNames.EXPORT_JOB_CONFIG_PAGE_STATUS);
		if (temporarySharedStatus != null) {
			return gson.fromJson(temporarySharedStatus.getStatusValue(), ExportJobConfigPageStatus.class);
		}
		return null;
	}

}
