package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * @author hebelala
 */
@Repository
public interface SystemConfigRepository {

	List<SystemConfig> selectByPropertiesAndLastly(List<String> properties);

	List<SystemConfig> selectByLastly();

	List<SystemConfig> selectByPropertyPrefix(String prefix);

	Integer insert(SystemConfig systemConfig);

	Integer updateById(SystemConfig systemConfig);

	List<SystemConfig> selectByProperty(String property);

	List<SystemConfig> selectAllConfig();
}