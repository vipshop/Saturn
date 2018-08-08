package com.vip.saturn.job.console.mybatis.service;

import com.vip.saturn.job.console.mybatis.entity.SystemConfig;

import java.util.List;

/**
 * @author hebelala
 */
public interface SystemConfig4SqlService {

	List<SystemConfig> selectAllConfig();

	List<SystemConfig> selectByProperty(String property);

	List<SystemConfig> selectByPropertiesAndLastly(List<String> properties);

	List<SystemConfig> selectByLastly();

	List<SystemConfig> selectByPropertyPrefix(String prefix);

	Integer insert(SystemConfig systemConfig);

	Integer updateById(SystemConfig systemConfig);

}
