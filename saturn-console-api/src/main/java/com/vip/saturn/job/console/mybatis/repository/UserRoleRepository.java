package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.UserRole;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author hebelala
 */
@Repository
public interface UserRoleRepository {

	int insert(UserRole userRole);

	List<UserRole> selectAll();

	List<UserRole> selectByUserName(String userName);

	List<UserRole> selectByRoleKey(String roleKey);

	UserRole select(UserRole userRole);

	UserRole selectWithNotFilterDeleted(UserRole userRole);

	int delete(UserRole userRole);

	int update(UserRole pre, UserRole cur);

}
