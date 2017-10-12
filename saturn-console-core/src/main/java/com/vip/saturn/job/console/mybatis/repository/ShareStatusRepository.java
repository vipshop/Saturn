package com.vip.saturn.job.console.mybatis.repository;

import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.ShareStatus;

/**
 * 
 * @author hebelala
 *
 */
@Repository
public interface ShareStatusRepository {

	int create(ShareStatus shareStatus);

	int update(ShareStatus shareStatus);

	int delete(ShareStatus shareStatus);

	ShareStatus get(ShareStatus shareStatus);

}
