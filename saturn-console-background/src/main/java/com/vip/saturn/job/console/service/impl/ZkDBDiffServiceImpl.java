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
import com.vip.saturn.job.console.utils.ConsoleThreadFactory;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ZkDBDiffServiceImpl implements ZkDBDiffService {

    private static final Logger log = LoggerFactory.getLogger(ZkDBDiffServiceImpl.class);

    private static final String NAMESPACE_NOT_EXIST_TEMPLATE = "The namespace {%s} does not exists.";

    private static final String ERR_MSG_SKIP_DIFF = "skip diff by namespace:{} for reason:{}";

    private static final int DIFF_THREAD_NUM = 10;

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

    private ExecutorService diffExecutorService;

    @PostConstruct
    public void init() {
        if (diffExecutorService != null) {
            diffExecutorService.shutdownNow();
        }
        diffExecutorService = Executors
                .newFixedThreadPool(DIFF_THREAD_NUM, new ConsoleThreadFactory("diff-zk-db-thread", false));
    }

    @PreDestroy
    public void destroy() {
        if (diffExecutorService != null) {
            diffExecutorService.shutdownNow();
        }
    }

    @Override
    public List<JobDiffInfo> diffByCluster(String clusterKey) throws SaturnJobConsoleException {
        List<JobDiffInfo> resultList = Lists.newArrayList();

        long startTime = System.currentTimeMillis();
        List<String> namespaces = namespaceZkClusterMapping4SqlService.getAllNamespacesOfCluster(clusterKey);

        List<Callable<List<JobDiffInfo>>> callableList = Lists.newArrayList();
        for (final String namespace : namespaces) {
            Callable<List<JobDiffInfo>> callable = new Callable<List<JobDiffInfo>>() {
                @Override
                public List<JobDiffInfo> call() throws Exception {
                    return diffByNamespace(namespace);
                }
            };
            callableList.add(callable);
        }

        try {
            List<Future<List<JobDiffInfo>>> futures = diffExecutorService.invokeAll(callableList);

            for (Future<List<JobDiffInfo>> future : futures) {
                List<JobDiffInfo> jobDiffInfos = future.get();
                if (jobDiffInfos != null && !jobDiffInfos.isEmpty()) {
                    resultList.addAll(jobDiffInfos);
                }
            }
        } catch (InterruptedException e) {
            log.warn("the thread is interrupted", e);
            throw new SaturnJobConsoleException("the diff thread is interrupted", e);
        } catch (Exception e) {
            log.error("exception happens during execute diff operation", e);
            throw new SaturnJobConsoleException(e);
        }


        log.info("Finish diff zkcluster:{}, which cost {}ms", clusterKey, System.currentTimeMillis() - startTime);

        return resultList;
    }

    @Override
    public List<JobDiffInfo> diffByNamespace(String namespace) throws SaturnJobConsoleException {
        long startTime = System.currentTimeMillis();

        List<JobDiffInfo> jobDiffInfos = Lists.newArrayList();
        CuratorRepository.CuratorFrameworkOp zkClient;
        try {
            List<CurrentJobConfig> dbJobConfigList = configService.findConfigsByNamespace(namespace);
            if (dbJobConfigList == null || dbJobConfigList.isEmpty()) {
                return jobDiffInfos;
            }

            zkClient = initCuratorClient(namespace);
            if (zkClient == null) {
                return jobDiffInfos;
            }

            Set<String> jobNamesInDb = getAllJobNames(dbJobConfigList);

            for (CurrentJobConfig dbJobConfig : dbJobConfigList) {
                String jobName = dbJobConfig.getJobName();
                log.info("start to diff job:{}@{}", jobName, namespace);
                if (!checkJobIsExsitInZk(jobName, zkClient)) {
                    jobDiffInfos.add(new JobDiffInfo(namespace, jobName, JobDiffInfo.DiffType.DB_ONLY, Lists.<JobDiffInfo.ConfigDiffInfo>newArrayList()));
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

        } catch (SaturnJobConsoleException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("exception throws during diff by namespace [{}]", namespace, e);
            throw new SaturnJobConsoleException(e);
        } finally {
            log.info("Finish diff namespace:{} which cost {}ms", namespace, System.currentTimeMillis() - startTime);
        }

        return jobDiffInfos;
    }

    @Override
    public JobDiffInfo diffByJob(String namespace, String jobName) throws SaturnJobConsoleException {
        CuratorRepository.CuratorFrameworkOp zkClient;
        try {
            zkClient = initCuratorClient(namespace);
            if (zkClient == null) {
                return null;
            }
            log.info("start to diff job:{}", jobName);

            CurrentJobConfig dbJobConfig = configService.findConfigByNamespaceAndJobName(namespace, jobName);
            JobSettings zkJobConfig = jobDimensionService.getJobSettingsFromZK(jobName, zkClient);

            if (dbJobConfig == null && zkJobConfig != null) {
                return new JobDiffInfo(namespace, jobName, JobDiffInfo.DiffType.ZK_ONLY, Lists.<JobDiffInfo.ConfigDiffInfo>newArrayList());
            }

            if (dbJobConfig != null && zkJobConfig == null) {
                return new JobDiffInfo(namespace, jobName, JobDiffInfo.DiffType.DB_ONLY, Lists.<JobDiffInfo.ConfigDiffInfo>newArrayList());
            }

            return diff(dbJobConfig, zkJobConfig, true);
        } catch (Exception e) {
            log.error("exception throws during diff by namespace [{}] and job [{}]", namespace, jobName, e);
            throw new SaturnJobConsoleException(e);
        }
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

        String jobTypeInDB = dbJobConfig.getJobType();
        // jobType
        diff("jobType", jobTypeInDB, zkJobConfig.getJobType(), configDiffInfos);
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
        // queueName
        diff("queueName", dbJobConfig.getQueueName(), zkJobConfig.getQueueName(), configDiffInfos);
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
        // channelName
        diff("channelName", dbJobConfig.getChannelName(), zkJobConfig.getChannelName(), configDiffInfos);
        // showNormalLog
        diff("showNormalLog", dbJobConfig.getShowNormalLog(), zkJobConfig.getShowNormalLog(), configDiffInfos);
        // enabledReport
        diff("enabledReport", dbJobConfig.getEnabledReport(), zkJobConfig.getEnabledReport(), configDiffInfos);
        // showNormalLog
        diff("showNormalLog", dbJobConfig.getShowNormalLog(), zkJobConfig.getShowNormalLog(), configDiffInfos);

        if (!configDiffInfos.isEmpty()) {
            if (needDetail) {
                return new JobDiffInfo(dbJobConfig.getNamespace(), jobName, JobDiffInfo.DiffType.HAS_DIFFERENCE, configDiffInfos);
            }

            return new JobDiffInfo(dbJobConfig.getNamespace(), jobName, JobDiffInfo.DiffType.HAS_DIFFERENCE, Lists.<JobDiffInfo.ConfigDiffInfo>newArrayList());
        }

        return null;
    }

    private void diff(String key, Object valueInDb, Object valueInZk, List<JobDiffInfo.ConfigDiffInfo> configDiffInfos) {
        if (valueInDb == null && valueInZk == null) {
            return;
        }

        if (valueInDb == null) {
            // 空串与null视为相等
            if (valueInZk instanceof String && StringUtils.isEmpty((String) valueInZk)) {
                return;
            }

            log.debug("key:{} has difference between zk and db", key);
            configDiffInfos.add(new JobDiffInfo.ConfigDiffInfo(key, valueInDb, valueInZk));
            return;
        }

        // 空串与null视为相等
        if (valueInDb instanceof String && StringUtils.isEmpty((String) valueInDb) && StringUtils.isEmpty((String) valueInZk)) {
            return;
        }

        if (!valueInDb.equals(valueInZk)) {
            log.debug("key:{} has difference between zk and db", key);
            configDiffInfos.add(new JobDiffInfo.ConfigDiffInfo(key, valueInDb, valueInZk));
        }
    }

    private CuratorRepository.CuratorFrameworkOp initCuratorClient(String namespace) {
        RegistryCenterConfiguration registryCenterConfiguration = registryCenterService
                .findConfigByNamespace(namespace);
        if (registryCenterConfiguration == null) {
            String errMsg = String.format(NAMESPACE_NOT_EXIST_TEMPLATE, namespace);
            log.warn(ERR_MSG_SKIP_DIFF, namespace, errMsg);
            return null;
        }

        RegistryCenterClient registryCenterClient = registryCenterService.connectByNamespace(namespace);
        if (registryCenterClient != null && registryCenterClient.isConnected()) {
            return curatorRepository.newCuratorFrameworkOp(registryCenterClient.getCuratorClient());
        }

        log.warn(ERR_MSG_SKIP_DIFF, namespace, "fail to connect to zk.");
        return null;
    }

    private List<JobDiffInfo> getJobNamesWhichInZKOnly(String namespace, CuratorRepository.CuratorFrameworkOp zkClient, Set<String> jobNamesInDb) throws SaturnJobConsoleException {
        List<JobDiffInfo> jobsOnlyInZK = Lists.newArrayList();
        List<String> jobNamesInZk = jobDimensionService.getAllJobs(zkClient);

        for (String name : jobNamesInZk) {
            if (jobNamesInDb == null || jobNamesInDb.isEmpty() || !jobNamesInDb.contains(name)) {
                jobsOnlyInZK.add(new JobDiffInfo(namespace, name, JobDiffInfo.DiffType.ZK_ONLY, Lists.<JobDiffInfo.ConfigDiffInfo>newArrayList()));
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
