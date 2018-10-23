package com.vip.saturn.demo;

import com.vip.saturn.job.spring.GenericSpringSaturnApplication;
import org.springframework.context.ApplicationContext;

public class Application extends GenericSpringSaturnApplication {

	/**
	 * If the Spring container defaults aren’t to your taste, you can instead customize it
	 * @return the running ApplicationContext
	 */
	@Override
	protected ApplicationContext run() {
		return super.run();
	}

	/**
	 * You can override this method, to load the custom xml files. The <code>applicationContext.xml</code> will be loaded by default.
	 * @return array of resource locations
	 */
	@Override
	protected String[] getConfigLocations() {
		return super.getConfigLocations();
	}

}
