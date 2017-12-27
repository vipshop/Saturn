package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.TemporarySharedStatus;

/**
 * @author hebelala
 */
public interface TemporarySharedStatusService {

	int delete(String statusKey);

	int create(String statusKey, String statusValue);

	int update(String statusKey, String statusValue);

	TemporarySharedStatus get(String statusKey);

}
