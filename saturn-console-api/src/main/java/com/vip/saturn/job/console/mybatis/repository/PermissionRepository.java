package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.Permission;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author hebelala
 */
@Repository
public interface PermissionRepository {

	int insert(Permission permission);

	List<Permission> selectAll();

	Permission selectByKey(String key);

}
