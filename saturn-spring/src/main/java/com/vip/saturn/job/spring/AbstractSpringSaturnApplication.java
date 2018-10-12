package com.vip.saturn.job.spring;

import org.springframework.context.ApplicationContext;

/**
 * @author hebelala
 */
public abstract class AbstractSpringSaturnApplication implements SpringSaturnApplication {

	protected ApplicationContext applicationContext;

	@Override
	public <J> J getJobInstance(Class<J> jobClass) {
		return applicationContext != null ? applicationContext.getBean(jobClass) : null;
	}
}
