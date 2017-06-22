package com.vip.saturn.job.console.mybatis.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.vip.saturn.job.console.mybatis.entity.RegistryCenterConfig;

/**
 * 
 * @author hebelala
 *
 */
@Repository
public interface RegistryCenterConfigRepository {

	RegistryCenterConfig selectByNamespace(String namespace);
	
	List<RegistryCenterConfig> selectAll();
	
	int insert(RegistryCenterConfig registryCenterConfig);

	int updateById(RegistryCenterConfig registryCenterConfig);
}
