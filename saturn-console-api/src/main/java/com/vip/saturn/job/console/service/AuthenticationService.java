package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.User;

public interface AuthenticationService {

	User authenticate(String username, String password) throws SaturnJobConsoleException;

}
