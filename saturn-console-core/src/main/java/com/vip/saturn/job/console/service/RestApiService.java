package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.RestApiJobInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.util.List;

/**
 * @author xiaopeng.he
 */
public interface RestApiService {

    List<RestApiJobInfo> getRestApiJobInfos(String namespace) throws SaturnJobConsoleException;

}
