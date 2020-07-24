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
import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.mybatis.repository.UserRepository;
import com.vip.saturn.job.console.service.AuthenticationService;
import com.vip.saturn.job.console.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

public class AuthenticationServiceImpl implements AuthenticationService {

	@Autowired
	private UserRepository userRepository;

	@Value("${authentication.hash:plaintext}")
	private String hashMethod;

	@Transactional(readOnly = true)
	@Override
	public User authenticate(String username, String password) throws SaturnJobConsoleException {
		if (StringUtils.isEmpty(password)) {
			throw new SaturnJobConsoleException(SaturnJobConsoleException.ERROR_CODE_AUTHN_FAIL, "密码不能为空");
		}

		User user = userRepository.select(username);
		if (user == null) {
			throw new SaturnJobConsoleException(SaturnJobConsoleException.ERROR_CODE_AUTHN_FAIL, "用户名不存在");
		}

		PasswordUtils.validate(password, user.getPassword(), hashMethod);

		return user;
	}

	public void setHashMethod(String hashMethod) {
		this.hashMethod = hashMethod;
	}
}
