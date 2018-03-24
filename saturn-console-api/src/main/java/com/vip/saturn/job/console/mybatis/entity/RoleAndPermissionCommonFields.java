package com.vip.saturn.job.console.mybatis.entity;

/**
 * @author hebelala
 */
public class RoleAndPermissionCommonFields extends EntityCommonFields {

	private String key;

	private String name;

	private String description;

	public RoleAndPermissionCommonFields() {
		super();
	}

	public RoleAndPermissionCommonFields(String key) {
		super();
		this.key = key;
	}

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
}
