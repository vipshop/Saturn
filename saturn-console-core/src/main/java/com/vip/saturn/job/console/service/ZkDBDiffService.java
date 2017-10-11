package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.JobDiffInfo;

import java.util.List;

// TODO interface method throws SaturnJobException, and zk_db_diff.js zk_db_diff/diffByJob data.obj.configDiffInfos deal well with null
public interface ZkDBDiffService {

    /**
     * Diff the config data in zk and db of the same zk cluster.
     *
     * @param clusterKey zk cluster key.
     * @return The different info organized by job.
     */
    List<JobDiffInfo> diffByCluster(String clusterKey) throws InterruptedException;

    /**
     * Diff the config data in zk and db of namespace.
     *
     * @param namespace
     * @return The different info organized by job.
     */
    List<JobDiffInfo> diffByNamespace(String namespace);

    /**
     * Diff the config data in zk and db of job.
     *
     * @param jobName
     * @return The different info organized by job. If no difference, return null;
     */
    JobDiffInfo diffByJob(String namespace, String jobName);
}
