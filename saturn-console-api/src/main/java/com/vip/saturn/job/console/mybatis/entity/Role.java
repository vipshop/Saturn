package com.vip.saturn.job.console.mybatis.entity;

import java.util.List;

/**
 * @author hebelala
 */
public class Role extends RoleAndPermissionCommonFields {

	private List<RolePermission> rolePermissions;

	public List<RolePermission> getRolePermissions() {
		return rolePermissions;
	}

	public void setRolePermissions(List<RolePermission> rolePermissions) {
		this.rolePermissions = rolePermissions;
	}
}
