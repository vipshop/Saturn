package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.ContainerNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import com.vip.saturn.job.console.vo.JobInfo;
import com.vip.saturn.job.console.vo.JobMode;
import com.vip.saturn.job.console.vo.JobStatus;
import com.vip.saturn.job.console.vo.JobType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
@Service
public class JobServiceImpl implements JobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceImpl.class);

    @Resource
    private RegistryCenterService registryCenterService;

    @Resource
    private CurrentJobConfigService currentJobConfigService;

    @Override
    public List<JobInfo> jobs(String namespace) throws SaturnJobConsoleException {
        List<JobInfo> list = new ArrayList<>();
        try {
            CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
            List<CurrentJobConfig> jobConfigList = currentJobConfigService.findConfigsByNamespace(namespace);
            if (jobConfigList != null) {
                for (CurrentJobConfig jobConfig : jobConfigList) {
                    try {
                        if (isSystemJob(jobConfig)) {
                            continue;
                        }
                        JobInfo jobInfo = new JobInfo();
                        jobInfo.setJobName(jobConfig.getJobName());
                        jobInfo.setDescription(jobConfig.getDescription());
                        jobInfo.setJobClass(jobConfig.getJobClass());
                        jobInfo.setJobType(JobType.getJobType(jobConfig.getJobType()));
                        if (JobType.UNKOWN_JOB.equals(jobInfo.getJobType())) {
                            if (jobInfo.getJobClass() != null && jobInfo.getJobClass().indexOf("SaturnScriptJob") != -1) {
                                jobInfo.setJobType(JobType.SHELL_JOB);
                            } else {
                                jobInfo.setJobType(JobType.JAVA_JOB);
                            }
                        }
                        jobInfo.setJobEnabled(jobConfig.getEnabled());
                        jobInfo.setStatus(getJobStatus(jobConfig.getJobName(), curatorFrameworkOp, jobConfig.getEnabled()));
                        jobInfo.setJobParameter(jobConfig.getJobParameter());
                        jobInfo.setShardingItemParameters(jobConfig.getShardingItemParameters());
                        jobInfo.setQueueName(jobConfig.getQueueName());
                        jobInfo.setChannelName(jobConfig.getChannelName());
                        jobInfo.setLoadLevel(String.valueOf(jobConfig.getLoadLevel()));
                        String jobDegree = jobConfig.getJobDegree() == null ? "0" : String.valueOf(jobConfig.getJobDegree());
                        jobInfo.setJobDegree(jobDegree);
                        jobInfo.setShardingTotalCount(String.valueOf(jobConfig.getShardingTotalCount()));

                        if (jobConfig.getTimeout4AlarmSeconds() == null) {
                            jobInfo.setTimeout4AlarmSeconds(0);
                        } else {
                            jobInfo.setTimeout4AlarmSeconds(jobConfig.getTimeout4AlarmSeconds());
                        }
                        jobInfo.setTimeoutSeconds(jobConfig.getTimeoutSeconds());
                        jobInfo.setPausePeriodDate(jobConfig.getPausePeriodDate());
                        jobInfo.setPausePeriodTime(jobConfig.getPausePeriodTime());
                        jobInfo.setShowNormalLog(jobConfig.getShowNormalLog());
                        jobInfo.setLocalMode(jobConfig.getLocalMode());
                        jobInfo.setUseSerial(jobConfig.getUseSerial());
                        jobInfo.setUseDispreferList(jobConfig.getUseDispreferList());
                        jobInfo.setProcessCountIntervalSeconds(jobConfig.getProcessCountIntervalSeconds());
                        jobInfo.setGroups(jobConfig.getGroups());
                        String preferList = jobConfig.getPreferList();
                        jobInfo.setPreferList(preferList);
                        if (!StringUtils.isBlank(preferList)) {
                            String containerTaskIdsNodePath = ContainerNodePath.getDcosTasksNodePath();
                            List<String> containerTaskIds = curatorFrameworkOp.getChildren(containerTaskIdsNodePath);
                            jobInfo.setMigrateEnabled(isMigrateEnabled(preferList, containerTaskIds));
                        } else {
                            jobInfo.setMigrateEnabled(false);
                        }
                        String timeZone = jobConfig.getTimeZone();
                        if (Strings.isNullOrEmpty(timeZone)) {
                            jobInfo.setTimeZone(SaturnConstants.TIME_ZONE_ID_DEFAULT);
                        } else {
                            jobInfo.setTimeZone(timeZone);
                        }
                        jobInfo.setCron(jobConfig.getCron());

                        updateJobInfoStatus(curatorFrameworkOp, jobConfig.getJobName(), jobInfo);

                        list.add(jobInfo);
                    } catch (Exception e) {
                        LOGGER.error("list job " + jobConfig.getJobName() + " error", e);
                    }
                }
            }
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Exception e) {
            throw new SaturnJobConsoleException(e);
        }

        return list;
    }

    private boolean isSystemJob(CurrentJobConfig jobConfig) {
        return StringUtils.isNotBlank(jobConfig.getJobMode()) && jobConfig.getJobMode().startsWith(JobMode.SYSTEM_PREFIX);
    }

    private JobStatus getJobStatus(final String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
                                   boolean enabled) {
        // see if all the shards is finished.
        boolean isAllShardsFinished = isAllShardsFinished(jobName, curatorFrameworkOp);
        if (enabled) {
            if (isAllShardsFinished) {
                return JobStatus.READY;
            }
            return JobStatus.RUNNING;
        } else {
            if (isAllShardsFinished) {
                return JobStatus.STOPPED;
            }
            return JobStatus.STOPPING;
        }
    }

    private boolean isAllShardsFinished(final String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
        List<String> executionItems = curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName));
        boolean isAllShardsFinished = true;
        if (executionItems != null && !executionItems.isEmpty()) {
            for (String itemStr : executionItems) {
                boolean isItemCompleted = curatorFrameworkOp
                        .checkExists(JobNodePath.getExecutionNodePath(jobName, itemStr, "completed"));
                boolean isItemRunning = curatorFrameworkOp
                        .checkExists(JobNodePath.getExecutionNodePath(jobName, itemStr, "running"));
                // if executor is kill by -9 while it is running, completed node won't exists as well as running node.
                // under this circumstance, we consider it is completed.
                if (!isItemCompleted && isItemRunning) {
                    isAllShardsFinished = false;
                    break;
                }
            }
        }
        return isAllShardsFinished;
    }

    private boolean isMigrateEnabled(String preferList, List<String> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return false;
        }
        List<String> preferTasks = new ArrayList<>();
        String[] split = preferList.split(",");
        for (int i = 0; i < split.length; i++) {
            String prefer = split[i].trim();
            if (prefer.startsWith("@")) {
                preferTasks.add(prefer.substring(1));
            }
        }
        if (!preferTasks.isEmpty()) {
            for (String task : tasks) {
                if (!preferTasks.contains(task)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateJobInfoStatus(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName,
                                     JobInfo jobInfo) {
        if (JobStatus.STOPPED.equals(jobInfo.getStatus())) {// 作业如果是STOPPED状态，不需要显示已分配的executor
            return;
        }
        String executorsPath = JobNodePath.getServerNodePath(jobName);
        List<String> executors = curatorFrameworkOp.getChildren(executorsPath);
        if (CollectionUtils.isEmpty(executors)) {
            return;
        }
        StringBuilder shardingListSb = new StringBuilder();
        for (String executor : executors) {
            String sharding = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executor, "sharding"));
            if (!Strings.isNullOrEmpty(sharding)) {
                shardingListSb.append(executor).append(",");
            }
        }
        if (shardingListSb != null && shardingListSb.length() > 0) {
            jobInfo.setShardingList(shardingListSb.substring(0, shardingListSb.length() - 1));
        }
    }

    @Override
    public List<String> groups(String namespace) throws SaturnJobConsoleException {
        List<String> groups = new ArrayList<>();
        List<CurrentJobConfig> jobConfigList = currentJobConfigService.findConfigsByNamespace(namespace);
        if (jobConfigList != null) {
            for (CurrentJobConfig jobConfig : jobConfigList) {
                if (isSystemJob(jobConfig)) {
                    continue;
                }
                String jobGroups = jobConfig.getGroups();
                if (jobGroups != null && !groups.contains(jobGroups)) {
                    groups.add(jobGroups);
                }
            }
        }
        return groups;
    }

}
