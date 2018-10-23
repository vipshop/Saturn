package com.vip.saturn.job.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author hebelala
 */
public class GenericSpringSaturnApplication extends AbstractSpringSaturnApplication {

	private static final String[] CONFIG_LOCATIONS_DEFAULT = {"applicationContext.xml"};

	@Override
	public void init() {
		if (applicationContext != null) {
			destroy();
		}
		applicationContext = run();
	}

	@Override
	public void destroy() {
		if (applicationContext != null) {
			if (applicationContext instanceof ConfigurableApplicationContext) {
				((ConfigurableApplicationContext) applicationContext).close();
			}
			applicationContext = null;
		}
	}

	/**
	 * If the Spring container defaults aren’t to your taste, you can instead customize it
	 * @return the running ApplicationContext
	 */
	protected ApplicationContext run() {
		return new ClassPathXmlApplicationContext(getConfigLocations());
	}

	/**
	 * You can override this method, to load the custom xml files. The <code>applicationContext.xml</code> will be loaded by default.
	 * @return array of resource locations
	 */
	protected String[] getConfigLocations() {
		return CONFIG_LOCATIONS_DEFAULT;
	}

}
