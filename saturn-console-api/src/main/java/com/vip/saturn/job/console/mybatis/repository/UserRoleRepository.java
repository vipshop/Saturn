package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.UserRole;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author hebelala
 */
@Repository
public interface UserRoleRepository {

	int insert(UserRole userRole);

	List<UserRole> selectAll();

	List<UserRole> selectByUserName(@Param("userName") String userName);

	List<UserRole> selectByRoleKey(@Param("roleKey") String roleKey);

	/**
	 * 如果字段为null，则不作为where语句条件
	 */
	List<UserRole> select(UserRole userRole);

	UserRole selectWithNotFilterDeleted(UserRole userRole);

	int delete(UserRole userRole);

	int update(@Param("pre") UserRole pre, @Param("cur") UserRole cur);

}
