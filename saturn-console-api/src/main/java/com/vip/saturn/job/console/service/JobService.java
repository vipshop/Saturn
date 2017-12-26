package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.vo.JobInfo;

import java.util.List;

/**
 * @author hebelala
 */
public interface JobService {

    List<JobInfo> jobs(String namespace) throws SaturnJobConsoleException;

    List<String> groups(String namespace) throws SaturnJobConsoleException;

}
