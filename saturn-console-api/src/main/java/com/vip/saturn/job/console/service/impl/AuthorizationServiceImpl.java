package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.*;
import com.vip.saturn.job.console.mybatis.repository.*;
import com.vip.saturn.job.console.service.AuthorizationService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

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

	@Value("${authorization.enabled.default}")
	private boolean authorizationEnabledDefault;

	protected String systemAdminRoleKey = "system_admin";

	@Override
	public boolean isAuthorizationEnabled() throws SaturnJobConsoleException {
		return systemConfigService
				.getBooleanValue(SystemConfigProperties.AUTHORIZATION_ENABLED, authorizationEnabledDefault);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void addUserRole(UserRole userRole) throws SaturnJobConsoleException {
		// add user first if not exists
		User user = userRepository.selectWithNotFilterDeleted(userRole.getUserName());
		if (user == null) {
			userRepository.insert(userRole.getUser());
		} else {
			userRepository.update(user);
		}
		// check role is existing
		String roleKey = userRole.getRoleKey();
		Role role = roleRepository.selectByKey(roleKey);
		if (role == null) {
			throw new SaturnJobConsoleException(String.format("该角色key(%s)不存在", roleKey));
		}
		// insert or update userRole
		UserRole pre = userRoleRepository.selectWithNotFilterDeleted(userRole);
		if (pre == null) {
			userRoleRepository.insert(userRole);
		} else {
			userRoleRepository.update(pre, userRole);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteUserRole(UserRole userRole) throws SaturnJobConsoleException {
		userRoleRepository.delete(userRole);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateUserRole(UserRole pre, UserRole cur) throws SaturnJobConsoleException {
		userRoleRepository.delete(pre);
		UserRole userRole = userRoleRepository.selectWithNotFilterDeleted(cur);
		if (userRole == null) {
			userRoleRepository.insert(cur);
		} else {
			userRoleRepository.update(userRole, cur);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public boolean hasUserRole(UserRole userRole) throws SaturnJobConsoleException {
		UserRole result = userRoleRepository.select(userRole);
		return result != null;
	}

	@Transactional(readOnly = true)
	@Override
	public List<User> getAllUsers() throws SaturnJobConsoleException {
		List<User> allUsers = new ArrayList<>();
		List<User> users = userRepository.selectAll();
		if (users != null) {
			for (User user : users) {
				allUsers.add(getUser(user.getUserName()));
			}
		}
		return allUsers;
	}

	@Transactional(readOnly = true)
	@Override
	public User getUser(String userName) throws SaturnJobConsoleException {
		User user = userRepository.select(userName);
		if (user == null) {
			user = new User();
			user.setUserName(userName);
			return user;
		}
		List<UserRole> userRoles = userRoleRepository.selectByUserName(userName);
		if (userRoles == null) {
			return user;
		}
		user.setUserRoles(userRoles);
		for (UserRole userRole : userRoles) {
			String roleKey = userRole.getRoleKey();
			Role role = roleRepository.selectByKey(roleKey);
			if (role == null) {
				continue;
			}
			userRole.setRole(role);

			List<RolePermission> rolePermissions = rolePermissionRepository.selectByRoleKey(roleKey);
			if (rolePermissions == null) {
				continue;
			}
			role.setRolePermissions(rolePermissions);

			for (RolePermission rolePermission : rolePermissions) {
				Permission permission = permissionRepository.selectByKey(rolePermission.getPermissionKey());
				rolePermission.setPermission(permission);
			}
		}
		return user;
	}

	@Transactional(readOnly = true)
	@Override
	public List<User> getSystemAdminUsers() throws SaturnJobConsoleException {
		List<User> superUsers = new ArrayList<>();
		List<UserRole> userRoles = userRoleRepository.selectByRoleKey(systemAdminRoleKey);
		if (userRoles == null) {
			return superUsers;
		}
		for (UserRole userRole : userRoles) {
			User user = userRepository.select(userRole.getUserName());
			superUsers.add(user);
		}
		return superUsers;
	}

	@Transactional(readOnly = true)
	@Override
	public Role getSystemAdminRole() throws SaturnJobConsoleException {
		Role role = roleRepository.selectByKey(systemAdminRoleKey);
		if (role == null) {
			return null;
		}
		List<RolePermission> rolePermissions = rolePermissionRepository.selectByRoleKey(systemAdminRoleKey);
		if (rolePermissions != null) {
			for (RolePermission rolePermission : rolePermissions) {
				Permission permission = permissionRepository.selectByKey(rolePermission.getPermissionKey());
				rolePermission.setPermission(permission);
			}
		}
		role.setRolePermissions(rolePermissions);
		return role;
	}

	@Transactional(readOnly = true)
	@Override
	public void assertIsPermitted(Permission permission, String userName, String namespace)
			throws SaturnJobConsoleException {
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
				if (namespace.equals(userRole.getNamespace())) {
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
						if (tmpPermission != null && tmpPermission.getPermissionKey()
								.equals(permission.getPermissionKey())) {
							return;
						}
					}
				}
			}
		}
		throw new SaturnJobConsoleException(
				String.format("您没有权限，域:%s，权限:%s", namespace, permission.getPermissionKey()));
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
