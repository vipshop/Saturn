package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.Role;
import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.mybatis.entity.UserRole;
import com.vip.saturn.job.console.mybatis.repository.RoleRepository;
import com.vip.saturn.job.console.mybatis.repository.UserRepository;
import com.vip.saturn.job.console.mybatis.repository.UserRoleRepository;
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
	protected RoleRepository roleRepository;

	@Autowired
	protected UserRepository userRepository;

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
	public List<Role> getRoles() throws SaturnJobConsoleException {
		List<Role> roles = roleRepository.selectAll();
		return roles == null ? new ArrayList<Role>() : roles;
	}

	@Transactional(readOnly = true)
	@Override
	public List<UserRole> getUserRoles(String userName, String roleKey, String namespace)
			throws SaturnJobConsoleException {
		List<UserRole> userRoles = userRoleRepository.select(new UserRole(userName, roleKey, namespace));
		return userRoles == null ? new ArrayList<UserRole>() : userRoles;
	}

}
