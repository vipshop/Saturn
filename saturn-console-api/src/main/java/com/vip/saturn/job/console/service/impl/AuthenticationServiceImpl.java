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
