package com.vip.saturn.job.console.mybatis.repository;

import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.TemporarySharedStatus;

/**
 * 
 * @author hebelala
 *
 */
@Repository
public interface ShareStatusRepository {

	int create(TemporarySharedStatus temporarySharedStatus);

	int update(TemporarySharedStatus temporarySharedStatus);

	int delete(TemporarySharedStatus temporarySharedStatus);

	TemporarySharedStatus get(TemporarySharedStatus temporarySharedStatus);

}
