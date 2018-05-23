package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.Role;
import com.vip.saturn.job.console.mybatis.entity.UserRole;

import java.util.List;

/**
 * @author hebelala
 */
public interface AuthorizationManageService {

	void addUserRole(UserRole userRole) throws SaturnJobConsoleException;

	void deleteUserRole(UserRole userRole) throws SaturnJobConsoleException;

	void updateUserRole(UserRole pre, UserRole cur) throws SaturnJobConsoleException;

	List<Role> getRoles() throws SaturnJobConsoleException;

	List<UserRole> getUserRoles(String userName, String roleKey, String namespace) throws SaturnJobConsoleException;
}
