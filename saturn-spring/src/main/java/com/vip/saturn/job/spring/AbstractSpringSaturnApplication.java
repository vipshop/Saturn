package com.vip.saturn.job.spring;

import com.vip.saturn.job.application.AbstractSaturnApplication;
import org.springframework.context.ApplicationContext;

/**
 * @author hebelala
 */
public abstract class AbstractSpringSaturnApplication extends AbstractSaturnApplication {

	protected ApplicationContext applicationContext;

	@Override
	public <J> J getJobInstance(Class<J> jobClass) {
		return applicationContext != null ? applicationContext.getBean(jobClass) : null;
	}
}
