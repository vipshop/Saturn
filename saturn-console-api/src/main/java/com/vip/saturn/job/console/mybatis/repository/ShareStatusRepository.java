package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.TemporarySharedStatus;
import org.springframework.stereotype.Repository;

/**
 * @author hebelala
 */
@Repository
public interface ShareStatusRepository {

	int create(TemporarySharedStatus temporarySharedStatus);

	int update(TemporarySharedStatus temporarySharedStatus);

	int delete(TemporarySharedStatus temporarySharedStatus);

	TemporarySharedStatus get(TemporarySharedStatus temporarySharedStatus);

}
