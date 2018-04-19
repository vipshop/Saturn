package com.vip.saturn.job.console.utils;

import com.vip.saturn.job.console.mybatis.entity.Role;

/**
 * @author hebelala
 */
public class Roles {

	public static final Role systemAdmin = new Role("system_admin");
	public static final Role namespaceDeveloper = new Role("namespace_developer");
	public static final Role namespaceAdmin = new Role("namespace_admin");
	public static final Role namespaceJobAdmin = new Role("namespace_job_admin");
	public static final Role namespaceExecutorAdmin = new Role("namespace_executor_admin");

}
