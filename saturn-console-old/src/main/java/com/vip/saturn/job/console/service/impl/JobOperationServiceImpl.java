/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.JobBriefInfo;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobMode;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.*;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.CronExpression;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.JsonUtils;
import com.vip.saturn.job.console.utils.SaturnConstants;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;

import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.*;

@Service
public class JobOperationServiceImpl implements JobOperationService {

	private static final Logger log = LoggerFactory.getLogger(JobOperationServiceImpl.class);

	private static final int DEFAULT_INTERVAL_TIME_OF_ENABLED_REPORT = 5;

	private static final int CALCULATE_NEXT_FIRE_TIME_MAX_TIMES = 5;

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private CurrentJobConfigService currentJobConfigService;

	@Resource
	private SystemConfigService systemConfigService;

	private MapType customContextType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class,
			String.class);

	private MapperFacade mapper;

	@Autowired
	public void setMapperFactory(MapperFactory mapperFactory) {
		this.mapper = mapperFactory.getMapperFacade();
	}

	@Override
	public void runAtOnceByJobnameAndExecutorName(String jobName, String executorName) {
		runAtOnceByJobnameAndExecutorName(jobName, executorName, curatorRepository.inSessionClient());
	}

	@Override
	public void runAtOnceByJobnameAndExecutorName(String jobName, String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String path = JobNodePath.getRunOneTimePath(jobName, executorName);
		createNode(curatorFrameworkOp, path);
	}

	private void createNode(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String path) {
		if (curatorFrameworkOp.checkExists(path)) {
			curatorFrameworkOp.delete(path);
		}
		curatorFrameworkOp.create(path);
	}

	@Override
	public void stopAtOnceByJobnameAndExecutorName(String jobName, String executorName) {
		stopAtOnceByJobnameAndExecutorName(jobName, executorName, curatorRepository.inSessionClient());
	}

	@Override
	public void stopAtOnceByJobnameAndExecutorName(String jobName, String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String path = JobNodePath.getStopOneTimePath(jobName, executorName);
		createNode(curatorFrameworkOp, path);
	}

	@Transactional
	@Override
	public void setJobEnabledState(String jobName, boolean state) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String namespace = curatorFrameworkOp.getCuratorFramework().getNamespace();
		CurrentJobConfig oldCurrentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace,
				jobName);
		if (oldCurrentJobConfig != null) {
			oldCurrentJobConfig.setEnabled(state);
			oldCurrentJobConfig.setLastUpdateTime(new Date());
			try {
				currentJobConfigService.updateByPrimaryKey(oldCurrentJobConfig);
			} catch (Exception e) {
				log.error("exception is thrown during change job state in db", e);
				throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
			}
		} else {
			log.warn("job:{} not existed in db", jobName);
		}
		curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, "enabled"), state);
	}

	@Transactional
	@Override
	public void updateJobCron(String jobName, String cron, Map<String, String> customContext)
			throws SaturnJobConsoleException {
		String cron0 = cron;
		if (cron0 != null && !cron0.trim().isEmpty()) {
			try {
				cron0 = cron0.trim();
				CronExpression.validateExpression(cron0);
			} catch (ParseException e) {
				throw new SaturnJobConsoleException("The cron expression is valid: " + cron);
			}
		} else {
			cron0 = "";
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))) {
			String newCustomContextStr = null;
			String newCron = null;
			String oldCustomContextStr = curatorFrameworkOp
					.getData(JobNodePath.getConfigNodePath(jobName, "customContext"));
			Map<String, String> oldCustomContextMap = toCustomContext(oldCustomContextStr);
			if (customContext != null && !customContext.isEmpty()) {
				oldCustomContextMap.putAll(customContext);
				newCustomContextStr = toCustomContext(oldCustomContextMap);
				if (newCustomContextStr.getBytes().length > 1024 * 1024) {
					throw new SaturnJobConsoleException("The all customContext is out of zk limit memory(1M)");
				}
			}
			String oldCron = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron"));
			if (cron0 != null && oldCron != null && !cron0.equals(oldCron.trim())) {
				newCron = cron0;
			}
			if (newCustomContextStr != null || newCron != null) {
				saveCronToDb(jobName, curatorFrameworkOp, newCustomContextStr, newCron);
			}
			if (newCustomContextStr != null) {
				curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, "customContext"), newCustomContextStr);
			}
			if (newCron != null) {
				curatorFrameworkOp.update(JobNodePath.getConfigNodePath(jobName, "cron"), newCron);
			}
		} else {
			throw new SaturnJobConsoleException("The job is not found: " + jobName);
		}
	}

	private void saveCronToDb(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			String newCustomContextStr, String newCron)
			throws SaturnJobConsoleException, SaturnJobConsoleHttpException {
		String namespace = curatorFrameworkOp.getCuratorFramework().getNamespace();
		CurrentJobConfig oldCurrentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace,
				jobName);
		if (oldCurrentJobConfig == null) {
			String errorMsg = "在DB找不到该作业的配置, namespace：" + namespace + " jobname:" + jobName;
			log.error(errorMsg);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMsg);
		}
		CurrentJobConfig newCurrentJobConfig = mapper.map(oldCurrentJobConfig, CurrentJobConfig.class);
		if (newCustomContextStr != null) {
			newCurrentJobConfig.setCustomContext(newCustomContextStr);
		}
		if (newCron != null) {
			newCurrentJobConfig.setCron(newCron);
		}

		try {
			currentJobConfigService.updateConfigAndSave2History(newCurrentJobConfig, oldCurrentJobConfig, null);
		} catch (Exception e) {
			log.error("exception is thrown during change job state in db", e);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	/**
	 * 将str转为map
	 *
	 * @param customContextStr str字符串
	 * @return 自定义上下文map
	 */
	private Map<String, String> toCustomContext(String customContextStr) {
		Map<String, String> customContext = null;
		if (customContextStr != null) {
			customContext = JsonUtils.fromJSON(customContextStr, customContextType);
		}
		if (customContext == null) {
			customContext = new HashMap<>();
		}
		return customContext;
	}

	/**
	 * 将map转为str字符串
	 *
	 * @param customContextMap 自定义上下文map
	 * @return 自定义上下文str
	 */
	private String toCustomContext(Map<String, String> customContextMap) {
		String result = JsonUtils.toJSON(customContextMap);
		if (result == null) {
			result = "";
		}
		return result.trim();
	}

	@Override
	public void validateJobConfig(JobConfig jobConfig) throws SaturnJobConsoleException {
		// 作业名必填
		if (jobConfig.getJobName() == null || jobConfig.getJobName().trim().isEmpty()) {
			throw new SaturnJobConsoleException("作业名必填");
		}
		// 作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_
		if (!jobConfig.getJobName().matches("[0-9a-zA-Z_]*")) {
			throw new SaturnJobConsoleException("作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_");
		}
		// 依赖的作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,
		if (jobConfig.getDependencies() != null && !jobConfig.getDependencies().matches("[0-9a-zA-Z_,]*")) {
			throw new SaturnJobConsoleException("依赖的作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,");
		}
		// 作业类型必填
		if (jobConfig.getJobType() == null || jobConfig.getJobType().trim().isEmpty()) {
			throw new SaturnJobConsoleException("作业类型必填");
		}
		// 验证作业类型
		if (JobBriefInfo.JobType.getJobType(jobConfig.getJobType()).equals(JobBriefInfo.JobType.UNKOWN_JOB)) {
			throw new SaturnJobConsoleException("作业类型未知");
		}
		// 验证VSHELL类型版本兼容性
		if (JobBriefInfo.JobType.getJobType(jobConfig.getJobType()).equals(JobBriefInfo.JobType.VSHELL)
				&& jobDimensionService.isNewSaturn("1.1.2") != 2) {
			throw new SaturnJobConsoleException("Shell消息作业导入要求所有executor版本都是1.1.2及以上");
		}
		// 如果是JAVA/MSG作业
		if (jobConfig.getJobType().equals(JobBriefInfo.JobType.JAVA_JOB.name())
				|| jobConfig.getJobType().equals(JobBriefInfo.JobType.MSG_JOB.name())) {
			// 作业实现类必填
			if (jobConfig.getJobClass() == null || jobConfig.getJobClass().trim().isEmpty()) {
				throw new SaturnJobConsoleException("对于JAVA/MSG作业，作业实现类必填");
			}
		}
		// 如果是JAVA/SHELL作业
		if (jobConfig.getJobType().equals(JobBriefInfo.JobType.JAVA_JOB.name())
				|| jobConfig.getJobType().equals(JobBriefInfo.JobType.SHELL_JOB.name())) {
			// cron表达式必填
			if (jobConfig.getCron() == null || jobConfig.getCron().trim().isEmpty()) {
				throw new SaturnJobConsoleException("对于JAVA/SHELL作业，cron表达式必填");
			}
			// cron表达式语法验证
			try {
				CronExpression.validateExpression(jobConfig.getCron());
			} catch (ParseException e) {
				throw new SaturnJobConsoleException("cron表达式语法有误，" + e.toString());
			}
		} else {
			jobConfig.setCron("");
			;// 其他类型的不需要持久化保存cron表达式
		}
		if (jobConfig.getLocalMode() != null && jobConfig.getLocalMode()) {
			if (jobConfig.getShardingItemParameters() == null) {
				throw new SaturnJobConsoleException("对于本地模式作业，分片参数必填。");
			} else {
				String[] split = jobConfig.getShardingItemParameters().split(",");
				boolean includeXing = false;
				for (String tmp : split) {
					String[] split2 = tmp.split("=");
					if ("*".equalsIgnoreCase(split2[0].trim())) {
						includeXing = true;
						break;
					}
				}
				if (!includeXing) {
					throw new SaturnJobConsoleException("对于本地模式作业，分片参数必须包含如*=xx。");
				}
			}
		} else {
			// 分片参数不能小于分片总数
			if (jobConfig.getShardingTotalCount() == null || jobConfig.getShardingTotalCount() < 1) {
				throw new SaturnJobConsoleException("分片数不能为空，并且不能小于1");
			}
			if (jobConfig.getShardingTotalCount() > 0) {
				if (jobConfig.getShardingItemParameters() == null
						|| jobConfig.getShardingItemParameters().trim().isEmpty()
						|| jobConfig.getShardingItemParameters().split(",").length < jobConfig
								.getShardingTotalCount()) {
					throw new SaturnJobConsoleException("分片参数不能小于分片总数");
				}
			}
		}
		// 不能添加系统作业
		if (jobConfig.getJobMode() != null && jobConfig.getJobMode().startsWith(JobMode.SYSTEM_PREFIX)) {
			throw new SaturnJobConsoleException("作业模式有误，不能添加系统作业");
		}
	}

	@Transactional
	@Override
	public void persistJob(JobConfig jobConfig, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		if (curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobConfig.getJobName()))) {
			curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobConfig.getJobName()));
		}
		correctConfigValueIfNeeded(jobConfig, curatorFrameworkOp);
		saveJobConfigToDb(jobConfig, curatorFrameworkOp);
		saveJobConfigToZkWhenPersist(jobConfig, curatorFrameworkOp, false);
	}

	@Transactional
	@Override
	public void copyAndPersistJob(JobConfig jobConfig, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws Exception {
		if (curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobConfig.getJobName()))) {
			curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobConfig.getJobName()));
		}
		correctConfigValueIfNeeded(jobConfig, curatorFrameworkOp);
		saveJobConfigToDb(jobConfig, curatorFrameworkOp);
		saveJobConfigToZkWhenCopy(jobConfig, curatorFrameworkOp);
	}

	@Override
	public void persistJobFromDB(JobConfig jobConfig, CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		jobConfig.setDefaultValues();
		saveJobConfigToZkWhenPersist(jobConfig, curatorFrameworkOp, true);
	}

	/**
	 * 对作业配置的一些属性进行矫正
	 * 
	 * @param jobConfig
	 * @param curatorFrameworkOp
	 */
	private void correctConfigValueIfNeeded(JobConfig jobConfig,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		jobConfig.setDefaultValues();
		jobConfig.setEnabled(false);
		jobConfig.setFailover(jobConfig.getLocalMode() == false);
		if (JobBriefInfo.JobType.MSG_JOB.name().equals(jobConfig.getJobType())) {// MSG作业没有cron表达式
			jobConfig.setCron("");
		} else if (JobBriefInfo.JobType.SHELL_JOB.name().equals(jobConfig.getJobType())) {
			int isNewSaturn = jobDimensionService.isNewSaturn(null, curatorFrameworkOp);
			if (isNewSaturn == 0) {
				// 旧版executor加shell作业时添加com.vip.saturn.job.executor.script.SaturnScriptJob类
				jobConfig.setJobClass("com.vip.saturn.job.executor.script.SaturnScriptJob");
			} else if (isNewSaturn == 1) {
				// 新版executor加shell作业时JobClass设为空
				jobConfig.setJobClass("");
			}
		}
		jobConfig.setEnabledReport(getEnabledReport(jobConfig.getJobType(), jobConfig.getCron(), jobConfig.getTimeZone()));
	}

	/**
	 * 对于定时作业，根据cron和INTERVAL_TIME_OF_ENABLED_REPORT来计算是否需要上报状态 see #286
	 */
	private boolean getEnabledReport(String jobType, String cron, String timeZone) {
		if (!jobType.equals(JobBriefInfo.JobType.JAVA_JOB.name()) && !jobType
				.equals(JobBriefInfo.JobType.SHELL_JOB.name())) {
			return false;
		}

		try {
			Integer intervalTimeConfigured = systemConfigService
					.getIntegerValue(SystemConfigProperties.INTERVAL_TIME_OF_ENABLED_REPORT,
							DEFAULT_INTERVAL_TIME_OF_ENABLED_REPORT);
			if (intervalTimeConfigured == null) {
				log.debug("System config INTERVAL_TIME_OF_ENABLED_REPORT is null");
				intervalTimeConfigured = DEFAULT_INTERVAL_TIME_OF_ENABLED_REPORT;
			}
			CronExpression cronExpression = new CronExpression(cron);
			cronExpression.setTimeZone(TimeZone.getTimeZone(timeZone));
			// 基于当前时间的下次调度时间
			Date lastNextTime = cronExpression.getNextValidTimeAfter(new Date());
			if (lastNextTime != null) {
				for (int i = 0; i < CALCULATE_NEXT_FIRE_TIME_MAX_TIMES; i++) {
					Date nextTime = cronExpression.getNextValidTimeAfter(lastNextTime);
					// no next fire time
					if (nextTime == null) {
						return true;
					}
					long interval = nextTime.getTime() - lastNextTime.getTime();
					if (interval < intervalTimeConfigured * 1000L) {
						return false;
					}
					lastNextTime = nextTime;
				}
			}
		} catch (ParseException e) {
			log.warn(e.getMessage(), e);
		}

		return true;
	}

	private void saveJobConfigToDb(JobConfig jobConfig, CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		String jobName = jobConfig.getJobName();
		String namespace = curatorFrameworkOp.getCuratorFramework().getNamespace();
		CurrentJobConfig oldJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (oldJobConfig != null) {
			log.warn(
					"when create a new job, a jobConfig with the same name from db exists, will delete it first. namespace:{} and jobName:{}",
					namespace, jobName);
			try {
				currentJobConfigService.deleteByPrimaryKey(oldJobConfig.getId());
			} catch (Exception e) {
				log.error("exception is thrown during delete job config in db", e);
				throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
						"创建作业时，数据库存在已经存在该作业的相关配置！并且清理该配置的时候失败", e);
			}
		}
		CurrentJobConfig currentJobConfig = new CurrentJobConfig();
		mapper.map(jobConfig, currentJobConfig);
		currentJobConfig.setCreateTime(new Date());
		currentJobConfig.setLastUpdateTime(new Date());
		currentJobConfig.setNamespace(namespace);
		try {
			currentJobConfigService.create(currentJobConfig);
		} catch (Exception e) {
			log.error("exception is thrown during creating job config in db", e);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	private void saveJobConfigToZkWhenPersist(JobConfig jobConfig,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, boolean fromDB) {
		String jobName = jobConfig.getJobName();
		if (!fromDB) {
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "enabled"), "false");
		} else {
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "enabled"),
					jobConfig.getEnabled());
		}
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "description"),
				jobConfig.getDescription());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "customContext"),
				jobConfig.getCustomContext());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobType"),
				jobConfig.getJobType());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobMode"),
				jobConfig.getJobMode());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters"),
				jobConfig.getShardingItemParameters());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobParameter"),
				jobConfig.getJobParameter());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "queueName"),
				jobConfig.getQueueName());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "channelName"),
				jobConfig.getChannelName());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "failover"),
				jobConfig.getFailover());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "monitorExecution"), "true");
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "timeout4AlarmSeconds"),
				jobConfig.getTimeout4AlarmSeconds());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"),
				jobConfig.getTimeoutSeconds());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "timeZone"),
				jobConfig.getTimeZone());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "cron"), jobConfig.getCron());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate"),
				jobConfig.getPausePeriodDate());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime"),
				jobConfig.getPausePeriodTime());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"),
				jobConfig.getProcessCountIntervalSeconds());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"),
				jobConfig.getShardingTotalCount());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "showNormalLog"),
				jobConfig.getShowNormalLog());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "loadLevel"),
				jobConfig.getLoadLevel());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobDegree"),
				jobConfig.getJobDegree());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "enabledReport"),
				jobConfig.getEnabledReport());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "preferList"),
				jobConfig.getPreferList());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "useDispreferList"),
				jobConfig.getUseDispreferList());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "localMode"),
				jobConfig.getLocalMode());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "useSerial"),
				jobConfig.getUseSerial());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "dependencies"),
				jobConfig.getDependencies());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "groups"),
				jobConfig.getGroups());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobClass"),
				jobConfig.getJobClass());
	}

	private void saveJobConfigToZkWhenCopy(JobConfig jobConfig, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws IllegalAccessException {
		String originJobName = jobConfig.getOriginJobName();
		List<String> jobConfigNodes = curatorFrameworkOp.getChildren(JobNodePath.getConfigNodePath(originJobName));
		if (CollectionUtils.isEmpty(jobConfigNodes)) {
			return;
		}
		Class<?> cls = jobConfig.getClass();
		String jobClassPath = "";
		String jobClassValue = "";
		for (String jobConfigNode : jobConfigNodes) {
			String jobConfigPath = JobNodePath.getConfigNodePath(originJobName, jobConfigNode);
			String jobConfigValue = curatorFrameworkOp.getData(jobConfigPath);
			try {
				Field field = cls.getDeclaredField(jobConfigNode);
				field.setAccessible(true);
				Object fieldValue = field.get(jobConfig);
				if (fieldValue != null) {
					jobConfigValue = fieldValue.toString();
				}
				if ("jobClass".equals(jobConfigNode)) {// 持久化jobClass会触发添加作业，待其他节点全部持久化完毕以后再持久化jobClass
					jobClassPath = JobNodePath.getConfigNodePath(jobConfig.getJobName(), jobConfigNode);
					jobClassValue = jobConfigValue;
				}
			} catch (NoSuchFieldException e) {// 即使JobConfig类中不存在该属性也复制（一般是旧版作业的一些节点，可以在旧版Executor上运行）
				continue;
			} finally {
				if (!"jobClass".equals(jobConfigNode)) {// 持久化jobClass会触发添加作业，待其他节点全部持久化完毕以后再持久化jobClass
					String fillJobNodePath = JobNodePath.getConfigNodePath(jobConfig.getJobName(), jobConfigNode);
					curatorFrameworkOp.fillJobNodeIfNotExist(fillJobNodePath, jobConfigValue);
				}
			}
		}
		if (!Strings.isNullOrEmpty(jobClassPath)) {
			curatorFrameworkOp.fillJobNodeIfNotExist(jobClassPath, jobClassValue);
		}
	}

	@Transactional
	@Override
	public void deleteJob(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException {
		try {
			String enabledNodePath = JobNodePath.getConfigNodePath(jobName, "enabled");
			long creationTime = curatorFrameworkOp.getCtime(enabledNodePath);
			long timeDiff = System.currentTimeMillis() - creationTime;
			if (timeDiff < SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT) {
				String errMessage = "Job cannot be deleted until "
						+ (SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT / 1000) + " seconds after job creation";
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), errMessage);
			}

			// 1.作业的executor全online的情况，添加toDelete节点，触发监听器动态删除节点
			String toDeleteNodePath = JobNodePath.getConfigNodePath(jobName, "toDelete");
			if (curatorFrameworkOp.checkExists(toDeleteNodePath)) {
				curatorFrameworkOp.deleteRecursive(toDeleteNodePath);
			}
			curatorFrameworkOp.create(toDeleteNodePath);

			for (int i = 0; i < 20; i++) {
				// 2.作业的executor全offline的情况，或有几个online，几个offline的情况
				String jobServerPath = JobNodePath.getServerNodePath(jobName);
				if (!curatorFrameworkOp.checkExists(jobServerPath)) {
					// (1)如果不存在$Job/JobName/servers节点，说明该作业没有任何executor接管，可直接删除作业节点
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
					return;
				}
				// (2)如果该作业servers下没有任何executor，可直接删除作业节点
				List<String> executors = curatorFrameworkOp.getChildren(jobServerPath);
				if (CollectionUtils.isEmpty(executors)) {
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
					return;
				}
				// (3)只要该作业没有一个能运行的该作业的executor在线，那么直接删除作业节点
				boolean hasOnlineExecutor = false;
				for (String executor : executors) {
					if (curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executor, "ip"))
							&& curatorFrameworkOp.checkExists(JobNodePath.getServerStatus(jobName, executor))) {
						hasOnlineExecutor = true;
					} else {
						curatorFrameworkOp.deleteRecursive(JobNodePath.getServerNodePath(jobName, executor));
					}
				}
				if (!hasOnlineExecutor) {
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
				}

				Thread.sleep(200);
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Throwable t) {
			log.error("exception is thrown during delete job", t);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), t);
		}
	}
}
