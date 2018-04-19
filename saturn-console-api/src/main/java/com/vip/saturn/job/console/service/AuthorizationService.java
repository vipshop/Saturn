package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.Permission;
import com.vip.saturn.job.console.mybatis.entity.Role;
import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.mybatis.entity.UserRole;

import java.util.List;

/**
 * @author hebelala
 */
public interface AuthorizationService {

	boolean isAuthorizationEnabled() throws SaturnJobConsoleException;

	void addUserRole(UserRole userRole) throws SaturnJobConsoleException;

	void deleteUserRole(UserRole userRole) throws SaturnJobConsoleException;

	void updateUserRole(UserRole pre, UserRole cur) throws SaturnJobConsoleException;

	boolean hasUserRole(UserRole userRole) throws SaturnJobConsoleException;

	List<User> getAllUsers() throws SaturnJobConsoleException;

	User getUser(String userName) throws SaturnJobConsoleException;

	List<User> getSystemAdminUsers() throws SaturnJobConsoleException;

	Role getSystemAdminRole() throws SaturnJobConsoleException;

	void assertIsPermitted(Permission permission, String userName, String namespace) throws SaturnJobConsoleException;

	void assertIsSystemAdmin(String userName) throws SaturnJobConsoleException;

}
