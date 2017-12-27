package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * @author hebelala
 */
@Repository
public interface ZkClusterInfoRepository {

	List<ZkClusterInfo> selectAll();

	ZkClusterInfo selectByClusterKey(String clusterKey);

}
