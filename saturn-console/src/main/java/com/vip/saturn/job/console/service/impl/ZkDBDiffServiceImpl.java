package com.vip.saturn.job.console.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vip.saturn.job.console.domain.JobDiffInfo;
import com.vip.saturn.job.console.domain.JobSettings;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMapping4SqlService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.ZkDBDiffService;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@Service
public class ZkDBDiffServiceImpl implements ZkDBDiffService {

    private static final Logger log = LoggerFactory.getLogger(ZkDBDiffServiceImpl.class);

    private static final String NAMESPACE_NOT_EXIST_TEMPLATE = "The namespace {%s} does not exists.";

    @Resource
    private NamespaceZkClusterMapping4SqlService namespaceZkClusterMapping4SqlService;

    @Resource
    private CurrentJobConfigService configService;

    @Resource
    private JobDimensionService jobDimensionService;

    @Resource
    private RegistryCenterService registryCenterService;

    @Resource
    private CuratorRepository curatorRepository;

    @Override
    public List<JobDiffInfo> diffByCluster(String clusterKey) {
        long startTime = System.currentTimeMillis();
        List<String> namespaces = namespaceZkClusterMapping4SqlService.getAllNamespacesOfCluster(clusterKey);

        List<JobDiffInfo> resultList = Lists.newArrayList();
        for (String namespace : namespaces) {
            resultList.addAll(diffByNamespace(namespace));
        }

        log.info("Finish diff cluster:{} which cost {}ms", clusterKey, System.currentTimeMillis() - startTime);

        return resultList;
    }

    @Override
    public List<JobDiffInfo> diffByNamespace(String namespace) {
        long startTime = System.currentTimeMillis();

        List<JobDiffInfo> jobDiffInfos = Lists.newArrayList();
        CuratorRepository.CuratorFrameworkOp zkClient;
        try {
            List<CurrentJobConfig> dbJobConfigList = configService.findConfigsByNamespace(namespace);
            zkClient = initCuratorClient(namespace);

            Set<String> jobNamesInDb = getAllJobNames(dbJobConfigList);

            for (CurrentJobConfig dbJobConfig : dbJobConfigList) {
                String jobName = dbJobConfig.getJobName();
                log.info("start to diff job:{}", jobName);
                if (!checkJobIsExsitInZk(jobName, zkClient)) {
                    jobDiffInfos.add(new JobDiffInfo(namespace, jobName, JobDiffInfo.DiffType.DB_ONLY, null));
                    continue;
                }

                JobSettings zkJobConfig = jobDimensionService.getJobSettingsFromZK(jobName, zkClient);
                JobDiffInfo jobDiffInfo = diff(dbJobConfig, zkJobConfig, false);
                if (jobDiffInfo != null) {
                    jobDiffInfos.add(jobDiffInfo);
                }
            }

            List<JobDiffInfo> jobsInZkOnly = getJobNamesWhichInZKOnly(namespace, zkClient, jobNamesInDb);
            if (jobsInZkOnly != null && !jobsInZkOnly.isEmpty()) {
                jobDiffInfos.addAll(jobsInZkOnly);
            }

        } catch (Exception e) {
            log.error("exception throws during diff by namespace [{}]", namespace, e);
        }

        log.info("Finish diff namespace:{} which cost {}ms", namespace, System.currentTimeMillis() - startTime);

        return jobDiffInfos;
    }

    @Override
    public JobDiffInfo diffByJob(String namespace, String jobName) {
        CuratorRepository.CuratorFrameworkOp zkClient;
        try {
            zkClient = initCuratorClient(namespace);
            log.info("start to diff job:{}", jobName);

            CurrentJobConfig dbJobConfig = configService.findConfigByNamespaceAndJobName(namespace, jobName);
            JobSettings zkJobConfig = jobDimensionService.getJobSettingsFromZK(jobName, zkClient);

            if (dbJobConfig == null && zkJobConfig != null) {
                return new JobDiffInfo(namespace, jobName, JobDiffInfo.DiffType.ZK_ONLY, null);
            }

            if (dbJobConfig != null && zkJobConfig == null) {
                return new JobDiffInfo(namespace, jobName, JobDiffInfo.DiffType.DB_ONLY, null);
            }

            return diff(dbJobConfig, zkJobConfig, true);
        } catch (Exception e) {
            log.error("exception throws during diff by namespace [{}] and job [{}]", namespace, jobName, e);
        }

        return null;
    }

    private boolean checkJobIsExsitInZk(String jobName, CuratorRepository.CuratorFrameworkOp zkClient) {
        return zkClient.checkExists(JobNodePath.getJobNodePath(jobName));
    }

    /**
     * zk中的作业配置和db中的作业配置的对比。
     *
     * @param dbJobConfig db里面的配置。
     * @param zkJobConfig zk里面的配置
     * @param needDetail  是否需要细节；true，则需要，false，为不需要；
     * @return
     */
    private JobDiffInfo diff(CurrentJobConfig dbJobConfig, JobSettings zkJobConfig, boolean needDetail) {
        String jobName = dbJobConfig.getJobName();

        List<JobDiffInfo.ConfigDiffInfo> configDiffInfos = Lists.newArrayList();

        // jobClass
        diff("jobClass", dbJobConfig.getJobClass(), zkJobConfig.getJobClass(), configDiffInfos);
        // shardingTotalCount
        diff("shardingTotalCount", dbJobConfig.getShardingTotalCount(), zkJobConfig.getShardingTotalCount(), configDiffInfos);
        // timeZone
        diff("timeZone", dbJobConfig.getTimeZone(), zkJobConfig.getTimeZone(), configDiffInfos);
        // cron
        diff("cron", dbJobConfig.getCron(), zkJobConfig.getCron(), configDiffInfos);
        // pausePeriodDate
        diff("pausePeriodDate", dbJobConfig.getPausePeriodDate(), zkJobConfig.getPausePeriodDate(), configDiffInfos);
        // pausePeriodTime
        diff("pausePeriodTime", dbJobConfig.getPausePeriodTime(), zkJobConfig.getPausePeriodTime(), configDiffInfos);
        // shardingItemParameters
        diff("shardingItemParameters", dbJobConfig.getShardingItemParameters(), zkJobConfig.getShardingItemParameters(), configDiffInfos);
        // jobParameter
        diff("jobParameter", dbJobConfig.getJobParameter(), zkJobConfig.getJobParameter(), configDiffInfos);
        // processCountIntervalSeconds
        diff("processCountIntervalSeconds", dbJobConfig.getProcessCountIntervalSeconds(), zkJobConfig.getProcessCountIntervalSeconds(), configDiffInfos);
        // timeout4AlarmSeconds
        diff("timeout4AlarmSeconds", dbJobConfig.getTimeout4AlarmSeconds(), zkJobConfig.getTimeout4AlarmSeconds(), configDiffInfos);
        // timeoutSeconds
        diff("timeoutSeconds", dbJobConfig.getTimeoutSeconds(), zkJobConfig.getTimeoutSeconds(), configDiffInfos);
        // loadLevel
        diff("loadLevel", dbJobConfig.getLoadLevel(), zkJobConfig.getLoadLevel(), configDiffInfos);
        // jobDegree
        diff("jobDegree", dbJobConfig.getJobDegree(), zkJobConfig.getJobDegree(), configDiffInfos);
        // enabled
        diff("enabled", dbJobConfig.getEnabled(), zkJobConfig.getEnabled(), configDiffInfos);
        // preferList
        diff("preferList", dbJobConfig.getPreferList(), zkJobConfig.getPreferList(), configDiffInfos);
        // useDispreferList
        diff("useDispreferList", dbJobConfig.getUseDispreferList(), zkJobConfig.getUseDispreferList(), configDiffInfos);
        // useSerial
        diff("useSerial", dbJobConfig.getUseSerial(), zkJobConfig.getUseSerial(), configDiffInfos);
        // localMode
        diff("localMode", dbJobConfig.getLocalMode(), zkJobConfig.getLocalMode(), configDiffInfos);
        // dependencies
        diff("dependencies", dbJobConfig.getDependencies(), zkJobConfig.getDependencies(), configDiffInfos);
        // groups
        diff("groups", dbJobConfig.getGroups(), zkJobConfig.getGroups(), configDiffInfos);
        // description
        diff("description", dbJobConfig.getDescription(), zkJobConfig.getDescription(), configDiffInfos);
        // jobMode
        diff("jobMode", dbJobConfig.getJobMode(), zkJobConfig.getJobMode(), configDiffInfos);
        // queueName
        diff("queueName", dbJobConfig.getQueueName(), zkJobConfig.getQueueName(), configDiffInfos);
        // channelName
        diff("channelName", dbJobConfig.getChannelName(), zkJobConfig.getChannelName(), configDiffInfos);
        // showNormalLog
        diff("showNormalLog", dbJobConfig.getShowNormalLog(), zkJobConfig.getShowNormalLog(), configDiffInfos);
        // jobType
        diff("jobType", dbJobConfig.getJobType(), zkJobConfig.getJobType(), configDiffInfos);
        // enabledReport
        diff("enabledReport", dbJobConfig.getEnabledReport(), zkJobConfig.getEnabledReport(), configDiffInfos);
        // showNormalLog
        diff("showNormalLog", dbJobConfig.getShowNormalLog(), zkJobConfig.getShowNormalLog(), configDiffInfos);

        if (!configDiffInfos.isEmpty()) {
            if (needDetail) {
                return new JobDiffInfo(dbJobConfig.getNamespace(), jobName, JobDiffInfo.DiffType.HAS_DIFFERENCE, configDiffInfos);
            }

            return new JobDiffInfo(dbJobConfig.getNamespace(), jobName, JobDiffInfo.DiffType.HAS_DIFFERENCE, null);
        }

        return null;
    }

    private void diff(String key, Object valueInDb, Object valueInZk, List<JobDiffInfo.ConfigDiffInfo> configDiffInfos) {
        if (valueInDb == null && valueInZk == null) {
            return;
        }

        if (valueInDb == null || !valueInDb.equals(valueInZk)) {
            log.debug("key:{} has difference between zk and db", key);
            configDiffInfos.add(new JobDiffInfo.ConfigDiffInfo(key, valueInDb, valueInZk));
        }
    }

    private CuratorRepository.CuratorFrameworkOp initCuratorClient(String namespace) throws SaturnJobConsoleException {
        RegistryCenterConfiguration registryCenterConfiguration = registryCenterService
                .findConfigByNamespace(namespace);
        if (registryCenterConfiguration == null) {
            throw new SaturnJobConsoleException(String.format(NAMESPACE_NOT_EXIST_TEMPLATE, namespace));
        }

        RegistryCenterClient registryCenterClient = registryCenterService.connectByNamespace(namespace);
        if (registryCenterClient != null && registryCenterClient.isConnected()) {
            return curatorRepository.newCuratorFrameworkOp(registryCenterClient.getCuratorClient());
        } else {
            throw new SaturnJobConsoleException("Connect zookeeper failed");
        }
    }

    private List<JobDiffInfo> getJobNamesWhichInZKOnly(String namespace, CuratorRepository.CuratorFrameworkOp zkClient, Set<String> jobNamesInDb) throws SaturnJobConsoleException {
        List<JobDiffInfo> jobsOnlyInZK = Lists.newArrayList();
        List<String> jobNamesInZk = jobDimensionService.getAllJobs(zkClient);
        // 找出只有ZK有DB没有的Job
        if (jobNamesInZk.size() > jobNamesInDb.size()) {
            for (String name : jobNamesInZk) {
                if (!jobNamesInDb.contains(name)) {
                    jobsOnlyInZK.add(new JobDiffInfo(namespace, name, JobDiffInfo.DiffType.ZK_ONLY, null));
                }
            }
        }

        return jobsOnlyInZK;
    }

    private Set<String> getAllJobNames(List<CurrentJobConfig> dbJobConfigList) {
        Set<String> jobNames = Sets.newHashSet();
        for (CurrentJobConfig jobConfig : dbJobConfigList) {
            jobNames.add(jobConfig.getJobName());
        }

        return jobNames;
    }

}
