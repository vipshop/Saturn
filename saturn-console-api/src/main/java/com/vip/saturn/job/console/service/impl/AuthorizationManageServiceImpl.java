/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
		validateUser(userRole);
		// check role is existing
		String roleKey = userRole.getRoleKey();
		Role role = roleRepository.selectByKey(roleKey);
		if (role == null) {
			throw new SaturnJobConsoleException(String.format("角色key(%s)不存在", roleKey));
		}
		// insert or update userRole
		UserRole pre = userRoleRepository.selectWithNotFilterDeleted(userRole);
		if (pre == null) {
			userRoleRepository.insert(userRole);
		} else {
			userRoleRepository.update(pre, userRole);
		}
	}

	protected void validateUser(UserRole userRole) throws SaturnJobConsoleException {
		String userName = userRole.getUserName();
		User user = userRepository.selectWithNotFilterDeleted(userName);
		if (user == null) {
			throw new SaturnJobConsoleException(String.format("用户名(%s)不存在", userName));
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
