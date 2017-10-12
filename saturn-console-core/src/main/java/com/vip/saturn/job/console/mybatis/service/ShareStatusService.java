package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.ShareStatus;

/**
 * 
 * @author hebelala
 *
 */
public interface ShareStatusService {

	int delete(String function);

	int create(String function, String data);

	int update(String function, String data);

	ShareStatus get(String function);

}
