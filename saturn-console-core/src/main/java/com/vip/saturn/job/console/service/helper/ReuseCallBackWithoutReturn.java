package com.vip.saturn.job.console.service.helper;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;

/**
 * Created by kfchu on 04/05/2017.
 */
public interface ReuseCallBackWithoutReturn {

	void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException;

}
