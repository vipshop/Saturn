package com.vip.saturn.job.console.mybatis.entity;

public class UserRole extends EntityCommonFields {

	private String userName;
	private String roleKey;
	private String namespace;
	private Boolean needApproval;
	private User user;
	private Role role;

	public UserRole() {

	}

	public UserRole(String userName, String roleKey, String namespace) {
		this.userName = userName;
		this.roleKey = roleKey;
		this.namespace = namespace;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getRoleKey() {
		return roleKey;
	}

	public void setRoleKey(String roleKey) {
		this.roleKey = roleKey;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Boolean getNeedApproval() {
		return needApproval;
	}

	public void setNeedApproval(Boolean needApproval) {
		this.needApproval = needApproval;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

}
