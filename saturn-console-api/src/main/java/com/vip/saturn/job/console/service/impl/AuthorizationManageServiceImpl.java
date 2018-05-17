package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.*;
import com.vip.saturn.job.console.mybatis.repository.*;
import com.vip.saturn.job.console.service.AuthorizationManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
public class AuthorizationManageServiceImpl implements AuthorizationManageService {

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
	public List<User> getAllUsers() throws SaturnJobConsoleException {
		List<User> allUsers = new ArrayList<>();
		List<User> users = userRepository.selectAll();
		if (users != null) {
			for (User user : users) {
				User user2 = getUser(user.getUserName());
				if (user2 != null) {
					allUsers.add(user2);
				}
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

	@Override
	public List<Role> getRoles() throws SaturnJobConsoleException {
		List<Role> roles = roleRepository.selectAll();
		return roles == null ? new ArrayList<Role>() : roles;
	}
}
