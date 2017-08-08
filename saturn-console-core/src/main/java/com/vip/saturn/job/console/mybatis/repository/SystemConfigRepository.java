package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author hebelala
 */
@Repository
public interface SystemConfigRepository {

	List<SystemConfig> selectByPropertiesAndLastly(List<String> properties);

	List<SystemConfig> selectByLastly();

	Integer insert(SystemConfig systemConfig);

	Integer updateById(SystemConfig systemConfig);

}