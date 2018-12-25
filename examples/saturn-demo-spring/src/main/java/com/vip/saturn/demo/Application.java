package com.vip.saturn.demo;

import com.vip.saturn.job.spring.GenericSpringSaturnApplication;

/**
 * Entrance of the demo application, make sure the class name is defined in saturn.properties.
 */
public class Application extends GenericSpringSaturnApplication {

	/*
	 * Set the Spring applicationContext file locations
	 */
	@Override
	protected String[] getConfigLocations() {
		return new String[]{"classpath:customContext.xml"};
	}

}
