package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.Role;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author hebelala
 */
@Repository
public interface RoleRepository {

	int insert(Role role);

	List<Role> selectAll();

	Role selectByKey(String key);

}
