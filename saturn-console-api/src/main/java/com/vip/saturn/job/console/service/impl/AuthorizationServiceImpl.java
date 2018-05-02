package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.*;
import com.vip.saturn.job.console.mybatis.repository.*;
import com.vip.saturn.job.console.service.AuthorizationManageService;
import com.vip.saturn.job.console.service.AuthorizationService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
public class AuthorizationServiceImpl implements AuthorizationService {

	@Autowired
	protected PermissionRepository permissionRepository;

	@Autowired
	protected RoleRepository roleRepository;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected RolePermissionRepository rolePermissionRepository;

	@Autowired
	protected UserRoleRepository userRoleRepository;

	@Autowired
	protected SystemConfigService systemConfigService;

	@Resource
	protected AuthorizationManageService authorizationManageService;

	@Value("${authorization.enabled.default}")
	private boolean authorizationEnabledDefault;

	protected String systemAdminRoleKey = "system_admin";

	@Override
	public boolean isAuthorizationEnabled() throws SaturnJobConsoleException {
		return systemConfigService
				.getBooleanValue(SystemConfigProperties.AUTHORIZATION_ENABLED, authorizationEnabledDefault);
	}

	@Transactional(readOnly = true)
	@Override
	public User getUser(String userName) throws SaturnJobConsoleException {
		User user = null;
		if (!isAuthorizationEnabled()) {
			return getAvailableUser(user, userName);
		}
		user = authorizationManageService.getUser(userName);
		return getAvailableUser(user, userName);
	}

	protected User getAvailableUser(User user, String userName) {
		if (user == null) {
			user = new User();
			user.setUserName(userName);
		}
		if (user.getUserRoles() == null) {
			user.setUserRoles(new ArrayList<UserRole>());
		}
		return user;
	}

	@Transactional(readOnly = true)
	@Override
	public boolean hasUserRole(UserRole userRole) throws SaturnJobConsoleException {
		if (!isAuthorizationEnabled()) {
			return true;
		}
		UserRole result = userRoleRepository.select(userRole);
		return result != null;
	}

	@Transactional(readOnly = true)
	@Override
	public void assertIsPermitted(String permissionKey, String userName, String namespace)
			throws SaturnJobConsoleException {
		if (!isAuthorizationEnabled()) {
			return;
		}
		List<UserRole> userRoles = userRoleRepository.selectByUserName(userName);
		if (userRoles != null) {
			for (UserRole userRole : userRoles) {
				String roleKey = userRole.getRoleKey();
				if (namespace.equals(userRole.getNamespace()) || systemAdminRoleKey.equals(roleKey)) {
					Role role = roleRepository.selectByKey(roleKey);
					if (role == null) {
						continue;
					}
					List<RolePermission> rolePermissions = rolePermissionRepository.selectByRoleKey(roleKey);
					if (rolePermissions == null) {
						continue;
					}
					for (RolePermission rolePermission : rolePermissions) {
						Permission tmpPermission = permissionRepository.selectByKey(rolePermission.getPermissionKey());
						if (tmpPermission != null && tmpPermission.getPermissionKey().equals(permissionKey)) {
							return;
						}
					}
				}
			}
		}
		throw new SaturnJobConsoleException(String.format("您没有权限，域:%s，权限:%s", namespace, permissionKey));
	}

	@Transactional(readOnly = true)
	@Override
	public void assertIsSystemAdmin(String userName) throws SaturnJobConsoleException {
		if (!isAuthorizationEnabled()) {
			return;
		}
		List<UserRole> userRoles = userRoleRepository.selectByUserName(userName);
		if (userRoles != null) {
			for (UserRole userRole : userRoles) {
				String roleKey = userRole.getRoleKey();
				if (systemAdminRoleKey.equals(roleKey)) {
					return;
				}
			}
		}
		throw new SaturnJobConsoleException("您不是系统管理员，没有权限");
	}
}
