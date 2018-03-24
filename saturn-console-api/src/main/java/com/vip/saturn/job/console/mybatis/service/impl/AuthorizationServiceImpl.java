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

	private String superRoleKey = "super";

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
			userRoleRepository.update(pre, cur);
		} else {
			userRoleRepository.update(userRole, cur);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public List<User> getAllUsers() throws SaturnJobConsoleException {
		List<User> allUsers = new ArrayList<>();
		List<User> users = userRepository.selectAll();
		if (users != null) {
			for (User user : users) {
				allUsers.add(getUser(user.getName()));
			}
		}
		return allUsers;
	}

	@Transactional(readOnly = true)
	@Override
	public User getUser(String userName) throws SaturnJobConsoleException {
		User user = userRepository.select(userName);
		if (user == null) {
			return null;
		}
		List<UserRole> userRoles = userRoleRepository.selectByUserName(userName);
		user.setUserRoles(userRoles);
		if (userRoles == null) {
			return user;
		}
		for (UserRole userRole : userRoles) {
			String roleKey = userRole.getRoleKey();
			Role role = roleRepository.selectByKey(roleKey);
			userRole.setRole(role);
			if (role == null) {
				continue;
			}
			List<RolePermission> rolePermissions = rolePermissionRepository.selectByRoleKey(roleKey);
			role.setRolePermissions(rolePermissions);
			if (rolePermissions == null) {
				continue;
			}
			for (RolePermission rolePermission : rolePermissions) {
				Permission permission = permissionRepository.selectByKey(rolePermission.getPermissionKey());
				rolePermission.setPermission(permission);
			}
		}
		return user;
	}

	@Transactional(readOnly = true)
	@Override
	public List<User> getSupers() throws SaturnJobConsoleException {
		List<User> superUsers = new ArrayList<>();
		List<UserRole> userRoles = userRoleRepository.selectByRoleKey(superRoleKey);
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
	public Role getSuperRole() throws SaturnJobConsoleException {
		Role role = roleRepository.selectByKey(superRoleKey);
		if (role == null) {
			return null;
		}
		List<RolePermission> rolePermissions = rolePermissionRepository.selectByRoleKey(superRoleKey);
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
	public boolean isPermitted(Permission permission, String userName, String namespace)
			throws SaturnJobConsoleException {
		List<UserRole> userRoles = userRoleRepository.selectByUserName(userName);
		if (userRoles == null) {
			return false;
		}
		for (UserRole userRole : userRoles) {
			String roleKey = userRole.getRoleKey();
			if (superRoleKey.equals(roleKey)) {
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
					Permission p = permissionRepository.selectByKey(rolePermission.getPermissionKey());
					if (p != null && permission.getKey().equals(p.getKey())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Transactional(readOnly = true)
	@Override
	public boolean isSuperRole(String userName) throws SaturnJobConsoleException {
		List<UserRole> userRoles = userRoleRepository.selectByUserName(userName);
		if (userRoles == null) {
			return false;
		}
		for (UserRole userRole : userRoles) {
			String roleKey = userRole.getRoleKey();
			if (superRoleKey.equals(roleKey)) {
				return true;
			}
		}
		return false;
	}
}
