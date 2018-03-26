package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.*;
import com.vip.saturn.job.console.mybatis.repository.*;
import com.vip.saturn.job.console.mybatis.service.AuthorizationService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
@Service
public class AuthorizationServiceImpl implements AuthorizationService {

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RolePermissionRepository rolePermissionRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private SystemConfigService systemConfigService;

	@Value("${authorization.enabled.default}")
	private boolean authorizationEnabledDefault;

	private String systemAdminRoleKey = "system_admin";

	@Override
	public boolean isAuthorizationEnabled() {
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
	public void deleteUserRole(UserRole userRole) {
		userRoleRepository.delete(userRole);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateUserRole(UserRole pre, UserRole cur) {
		userRoleRepository.delete(pre);
		UserRole userRole = userRoleRepository.selectWithNotFilterDeleted(cur);
		if (userRole == null) {
			userRoleRepository.update(pre, cur);
		} else {
			userRoleRepository.update(userRole, cur);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public List<User> getAllUsers() {
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
	public User getUser(String userName) {
		User user = userRepository.select(userName);
		if (user == null) {
			return null;
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
	public List<User> getSystemAdminUsers() {
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
	public Role getSystemAdminRole() {
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
	public boolean isPermitted(Permission permission, String userName, String namespace) {
		List<UserRole> userRoles = userRoleRepository.selectByUserName(userName);
		if (userRoles.isEmpty()) {
			return false;
		}

		for (UserRole userRole : userRoles) {
			String roleKey = userRole.getRoleKey();
			if (systemAdminRoleKey.equals(roleKey)) {
				return true;
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
						return true;
					}
				}
			}
		}
		return false;
	}

	@Transactional(readOnly = true)
	@Override
	public boolean isSystemAdminRole(String userName) {
		List<UserRole> userRoles = userRoleRepository.selectByUserName(userName);
		if (userRoles.isEmpty()) {
			return false;
		}
		for (UserRole userRole : userRoles) {
			String roleKey = userRole.getRoleKey();
			if (systemAdminRoleKey.equals(roleKey)) {
				return true;
			}
		}
		return false;
	}
}
