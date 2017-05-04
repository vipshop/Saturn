package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RestApiJobInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.util.List;

/**
 * @author hebelala
 */
public interface RestApiService {

    /**
     * Get the jobs info under the namespace
     */
    List<RestApiJobInfo> getRestApiJobInfos(String namespace) throws SaturnJobConsoleException;

    /**
     * Enable the job
     *
     * @return 200, the job was disabled, and enable it success; 201, the job was already enabled; 403, the update interval time cannot less than 3 seconds; others, should throw exception.
     */
    int enableJob(String namespace, String jobName) throws SaturnJobConsoleException;

    /**
     * Disable the job
     *
     * @return 200, the job was enabled, and disable it success; 201, the job was already disabled; 403, the update interval time cannot less than 3 seconds; others, should throw exception.
     */
    int disableJob(String namespace, String jobName) throws SaturnJobConsoleException;

    /**
     * Create a new job.
     *
     * @param jobConfig construct from the request.
     *
     * @throws SaturnJobConsoleException once the exception is thrown, which means the creation is not successfully. the caller should handle the exception by itself.
     */
    void createJob(String namespace, JobConfig jobConfig) throws SaturnJobConsoleException;

}
