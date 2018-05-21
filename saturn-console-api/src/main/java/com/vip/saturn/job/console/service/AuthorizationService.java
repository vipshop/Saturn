package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.mybatis.entity.UserRole;

/**
 * @author hebelala
 */
public interface AuthorizationService {

	void refreshAuthCache() throws SaturnJobConsoleException;

	boolean isAuthorizationEnabled() throws SaturnJobConsoleException;

	User getUser(String userName) throws SaturnJobConsoleException;

	boolean hasUserRole(UserRole userRole) throws SaturnJobConsoleException;

	void assertIsPermitted(String permissionKey, String userName, String namespace) throws SaturnJobConsoleException;

	void assertIsSystemAdmin(String userName) throws SaturnJobConsoleException;

}
