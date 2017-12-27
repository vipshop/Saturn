package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.vo.DependencyJob;
import com.vip.saturn.job.console.vo.JobInfo;

import java.util.List;

/**
 * @author hebelala
 */
public interface JobService {

    List<JobInfo> jobs(String namespace) throws SaturnJobConsoleException;

    List<String> groups(String namespace) throws SaturnJobConsoleException;

    List<DependencyJob> dependentJobs(String namespace, String jobName) throws SaturnJobConsoleException;

    List<DependencyJob> dependedJobs(String namespace, String jobName) throws SaturnJobConsoleException;

    void enableJob(String namespace, String jobName) throws SaturnJobConsoleException;

    void disableJob(String namespace, String jobName) throws SaturnJobConsoleException;

}
