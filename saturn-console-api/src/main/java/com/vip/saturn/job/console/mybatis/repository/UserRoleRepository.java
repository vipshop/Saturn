/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
