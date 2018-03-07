package com.vip.saturn.job.console.mybatis.service.impl;

import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.mybatis.repository.ShiroResitory;
import com.vip.saturn.job.console.mybatis.service.ShiroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author hebelala
 */
@Service
public class ShiroServiceImpl implements ShiroService {

	@Autowired
	private ShiroResitory shiroResitory;

	@Override
	public User getUserByName(String name) {
		return shiroResitory.getUserByName(name);
	}
}
