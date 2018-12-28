package com.vip.saturn.job.application;

/**
 * @author hebelala
 */
public abstract class AbstractSaturnApplication implements SaturnApplication {

	@Override
	public <J> J getJobInstance(Class<J> jobClass) {
		return null;
	}
}
