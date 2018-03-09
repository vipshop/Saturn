package com.vip.saturn.job.console.mybatis.entity;

import java.util.List;

/**
 * @author hebelala
 */
public class Role extends CommonFields {

	private String key;
	private String name;
	private String description;
	private List<RolePermission> rolePermissions;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<RolePermission> getRolePermissions() {
		return rolePermissions;
	}

	public void setRolePermissions(List<RolePermission> rolePermissions) {
		this.rolePermissions = rolePermissions;
	}
}
