package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.User;
import org.springframework.stereotype.Repository;

/**
 * @author hebelala
 */
@Repository
public interface ShiroResitory {

	User getUserByName(String name);

}
