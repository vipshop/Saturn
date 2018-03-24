package com.vip.saturn.job.console.mybatis.service;

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

	void addUserRole(UserRole userRole) throws SaturnJobConsoleException;

	void deleteUserRole(UserRole userRole) throws SaturnJobConsoleException;

	void updateUserRole(UserRole pre, UserRole cur) throws SaturnJobConsoleException;

	List<User> getAllUsers() throws SaturnJobConsoleException;

	User getUser(String userName) throws SaturnJobConsoleException;

	List<User> getSupers() throws SaturnJobConsoleException;

	Role getSuperRole() throws SaturnJobConsoleException;
	
	boolean isPermitted(Permission permission, String userName, String namespace) throws SaturnJobConsoleException;
	
	boolean isSuperRole(String userName) throws SaturnJobConsoleException;

}
