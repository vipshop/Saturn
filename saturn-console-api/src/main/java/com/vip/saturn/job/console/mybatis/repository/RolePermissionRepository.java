package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.RolePermission;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author hebelala
 */
@Repository
public interface RolePermissionRepository {

	int insert(RolePermission rolePermission);

	List<RolePermission> selectAll();

	List<RolePermission> selectByRoleKey(@Param("roleKey") String roleKey);

}
