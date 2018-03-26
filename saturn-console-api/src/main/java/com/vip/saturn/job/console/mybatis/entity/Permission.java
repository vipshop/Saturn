package com.vip.saturn.job.console.mybatis.entity;

/**
 * @author hebelala
 */
public class Permission extends EntityCommonFields {

	private String permissionKey;
	private String permissionName;
	private String description;

	public Permission() {
		super();
	}

	public Permission(String permissionKey) {
		super();
		this.permissionKey = permissionKey;
	}

	public String getPermissionKey() {
		return permissionKey;
	}

	public void setPermissionKey(String permissionKey) {
		this.permissionKey = permissionKey;
	}

	public String getPermissionName() {
		return permissionName;
	}

	public void setPermissionName(String permissionName) {
		this.permissionName = permissionName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
