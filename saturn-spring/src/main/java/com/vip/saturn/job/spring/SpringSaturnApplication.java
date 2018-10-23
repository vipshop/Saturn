package com.vip.saturn.job.spring;

import com.vip.saturn.job.application.SaturnApplication;

/**
 * @author hebelala
 */
public interface SpringSaturnApplication extends SaturnApplication {

	<J> J getJobInstance(Class<J> jobClass);

}
