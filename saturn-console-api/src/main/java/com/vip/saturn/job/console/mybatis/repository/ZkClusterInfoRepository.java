package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author hebelala
 */
@Repository
public interface ZkClusterInfoRepository {

	List<ZkClusterInfo> selectAll();

	ZkClusterInfo selectByClusterKey(String clusterKey);

	int insert(ZkClusterInfo zkClusterInfo);

	int update(ZkClusterInfo zkClusterInfo);

}
