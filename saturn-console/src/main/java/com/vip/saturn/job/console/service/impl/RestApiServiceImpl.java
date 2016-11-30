package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.RestApiService;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author xiaopeng.he
 */
@Service
public class RestApiServiceImpl implements RestApiService {

    private final static Logger logger = LoggerFactory.getLogger(RestApiServiceImpl.class);

    @Resource
    private RegistryCenterService registryCenterService;

    @Resource
    private CuratorRepository curatorRepository;

    @Override
    public List<RestApiJobInfo> getRestApiJobInfos(String namespace) throws SaturnJobConsoleException {
        List<RestApiJobInfo> restApiJobInfos = new ArrayList<>();
        try {
            RegistryCenterConfiguration registryCenterConfiguration = registryCenterService.findConfigByNamespace(namespace);
            if (registryCenterConfiguration == null) {
                throw new SaturnJobConsoleException("The namespace is not exists");
            }
            RegistryCenterClient registryCenterClient = registryCenterService.connectByNamespace(namespace);
            if (registryCenterClient != null && registryCenterClient.isConnected()) {
                CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
                CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorClient);
                String $JobsNodePath = JobNodePath.get$JobsNodePath();
                List<String> jobs = curatorFrameworkOp.getChildren($JobsNodePath);
                if (jobs != null) {
                    for (String job : jobs) {
                        RestApiJobInfo restApiJobInfo = new RestApiJobInfo();
                        restApiJobInfo.setJobName(job);
                        // 设置作业配置信息
                        setJobConfig(curatorFrameworkOp, restApiJobInfo, job);
                        // 设置运行状态
                        setRunningStatus(curatorFrameworkOp, restApiJobInfo, job);
                        // 设置统计信息
                        RestApiJobStatistics restApiJobStatistics = new RestApiJobStatistics();
                        setStatics(curatorFrameworkOp, restApiJobStatistics, job);
                        restApiJobInfo.setStatistics(restApiJobStatistics);

                        restApiJobInfos.add(restApiJobInfo);
                    }
                }
            } else {
                throw new SaturnJobConsoleException("Connect zookeeper failed");
            }
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new SaturnJobConsoleException(e);
        }
        return restApiJobInfos;
    }

    private void setRunningStatus(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, RestApiJobInfo restApiJobInfo, String jobName) {
        String executionNodePath = JobNodePath.getExecutionNodePath(jobName);
        List<String> items = curatorFrameworkOp.getChildren(executionNodePath);
        boolean isRunning = false;
        if (items != null) {
            for (String item : items) {
                String runningNodePath = JobNodePath.getExecutionNodePath(jobName, item, "running");
                if (curatorFrameworkOp.checkExists(runningNodePath)) {
                    isRunning = true;
                    break;
                }
            }
        }
        String enabledNodePath = JobNodePath.getConfigNodePath(jobName, "enabled");
        if (Boolean.valueOf(curatorFrameworkOp.getData(enabledNodePath))) {
            if (isRunning) {
                restApiJobInfo.setRunningStatus(JobStatus.RUNNING.name());
            } else {
                restApiJobInfo.setRunningStatus(JobStatus.READY.name());
            }
        } else {
            if (isRunning) {
                restApiJobInfo.setRunningStatus(JobStatus.STOPPING.name());
            } else {
                restApiJobInfo.setRunningStatus(JobStatus.STOPPED.name());
            }
        }
    }

    private void setJobConfig(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, RestApiJobInfo restApiJobInfo, String jobName) {
        String configNodePath = JobNodePath.getConfigNodePath(jobName);
        if (curatorFrameworkOp.checkExists(configNodePath)) {
            restApiJobInfo.setDescription(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "description")));
            restApiJobInfo.setEnabled(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled"))));

            RestApiJobConfig restApiJobConfig = new RestApiJobConfig();
            restApiJobConfig.setJobClass(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobClass")));
            restApiJobConfig.setJobType(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType")));
            restApiJobConfig.setShardingTotalCount(Integer.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"))));
            restApiJobConfig.setShardingItemParameters(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters")));
            restApiJobConfig.setJobParameter(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobParameter")));
            restApiJobConfig.setCron(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron")));
            restApiJobConfig.setPausePeriodDate(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate")));
            restApiJobConfig.setPausePeriodTime(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime")));
            restApiJobConfig.setTimeoutSeconds(Integer.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"))));
            restApiJobConfig.setChannelName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "channelName")));
            restApiJobConfig.setQueueName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "queueName")));
            restApiJobConfig.setLoadLevel(Integer.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"))));

            restApiJobConfig.setPreferList(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList")));
            restApiJobConfig.setUseDispreferList(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useDispreferList"))));
            restApiJobConfig.setLocalMode(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "localMode"))));
            restApiJobConfig.setUseSerial(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useSerial"))));
            restApiJobInfo.setJobConfig(restApiJobConfig);
        }
    }

    private void setStatics(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, RestApiJobStatistics restApiJobStatistics, String jobName) {
        setProcessCount(curatorFrameworkOp, restApiJobStatistics, jobName);
        setTimes(curatorFrameworkOp, restApiJobStatistics, jobName);
    }

    private void setProcessCount(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, RestApiJobStatistics restApiJobStatistics, String jobName) {
        String processCountPath = JobNodePath.getProcessCountPath(jobName);
        String processCountStr = curatorFrameworkOp.getData(processCountPath);
        if (processCountStr != null) {
            try {
                restApiJobStatistics.setProcessCount(Long.valueOf(processCountStr));
            } catch (NumberFormatException e) {
                logger.error(e.getMessage(), e);
            }
        }
        String errorCountPath = JobNodePath.getErrorCountPath(jobName);
        String errorCountPathStr = curatorFrameworkOp.getData(errorCountPath);
        if (errorCountPathStr != null) {
            try {
                restApiJobStatistics.setProcessErrorCount(Long.valueOf(errorCountPathStr));
            } catch (NumberFormatException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void setTimes(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, RestApiJobStatistics restApiJobStatistics, String jobName) {
        String executionNodePath = JobNodePath.getExecutionNodePath(jobName);
        List<String> items = curatorFrameworkOp.getChildren(executionNodePath);
        if (items != null) {
            List<String> lastBeginTimeList = new ArrayList<>();
            List<String> lastCompleteTimeList = new ArrayList<>();
            List<String> nextFireTimeList = new ArrayList<>();
            int runningItemSize = 0;
            for (String item : items) {
                if (getRunningIP(item, jobName, curatorFrameworkOp) == null) {
                    continue;
                }
                ++runningItemSize;
                String lastBeginTime = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastBeginTime"));
                if (null != lastBeginTime) {
                    lastBeginTimeList.add(lastBeginTime);
                }
                String lastCompleteTime = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastCompleteTime"));
                if (null != lastCompleteTime) {
                    boolean isItemCompleted = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "completed"));
                    boolean isItemRunning = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "running"));
                    if (isItemCompleted && !isItemRunning) { // 如果作业分片已执行完毕，则添加该完成时间到集合中进行排序
                        lastCompleteTimeList.add(lastCompleteTime);
                    }
                }
                String nextFireTime = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "nextFireTime"));
                if (null != nextFireTime) {
                    nextFireTimeList.add(nextFireTime);
                }
            }
            if (!CollectionUtils.isEmpty(lastBeginTimeList)) {
                Collections.sort(lastBeginTimeList);
                try {
                    restApiJobStatistics.setLastBeginTime(Long.parseLong(lastBeginTimeList.get(0))); // 所有分片中最近最早的开始时间
                } catch (NumberFormatException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (!CollectionUtils.isEmpty(lastCompleteTimeList) && lastCompleteTimeList.size() == runningItemSize) { // 所有分配都完成才显示最近最晚的完成时间
                Collections.sort(lastCompleteTimeList);
                try {
                    restApiJobStatistics.setLastCompleteTime(Long.parseLong(lastCompleteTimeList.get(lastCompleteTimeList.size() - 1))); // 所有分片中最近最晚的完成时间
                } catch (NumberFormatException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (!CollectionUtils.isEmpty(nextFireTimeList)) {
                Collections.sort(nextFireTimeList);
                try {
                    restApiJobStatistics.setNextFireTime(Long.parseLong(nextFireTimeList.get(0))); // 所有分片中下次最早的开始时间
                } catch (NumberFormatException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private String getRunningIP(String item, String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
        String runningIp = null;
        String serverNodePath = JobNodePath.getServerNodePath(jobName);
        if (!curatorFrameworkOp.checkExists(serverNodePath)) {
            return runningIp;
        }
        List<String> servers = curatorFrameworkOp.getChildren(serverNodePath);
        for (String server : servers) {
            String sharding = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, server, "sharding"));
            String toFind = "";
            if (Strings.isNullOrEmpty(sharding)) {
                continue;
            }
            for (String itemKey : Splitter.on(',').split(sharding)) {
                if (item.equals(itemKey)) {
                    toFind = itemKey;
                    break;
                }
            }
            if (!Strings.isNullOrEmpty(toFind)) {
                runningIp = server;
                break;
            }
        }
        return runningIp;
    }

}
