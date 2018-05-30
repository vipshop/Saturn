package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.mybatis.repository.UserRepository;
import com.vip.saturn.job.console.service.AuthenticationService;
import com.vip.saturn.job.console.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

public class AuthenticationServiceImpl implements AuthenticationService {

	@Autowired
	private UserRepository userRepository;

	@Value("${authentication.hash:plaintext}")
	private String hashMethod;

	@Override
	public User authenticate(String username, String password) throws SaturnJobConsoleException {
		if (StringUtils.isEmpty(password)) {
			return null;
		}

		User user = userRepository.select(username);
		if (user == null) {
			return null;
		}

		return PasswordUtils.validate(password, user.getPassword(), hashMethod) ? user : null;
	}

	public void setHashMethod(String hashMethod) {
		this.hashMethod = hashMethod;
	}
}
