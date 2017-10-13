package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.ShareStatus;

/**
 * 
 * @author hebelala
 *
 */
public interface ShareStatusService {

	int delete(String moduleName);

	int create(String moduleName, String data);

	int update(String moduleName, String data);

	ShareStatus get(String moduleName);

}
