package com.vip.saturn.job.console.service.helper;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;

/**
 * @author hebelala
 */
public interface ReuseCallBack<T> {

	T call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException;

}