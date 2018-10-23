package com.vip.saturn.job.springboot;

import com.vip.saturn.job.spring.AbstractSpringSaturnApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

/**
 * @author hebelala
 */
public class GenericSpringBootSaturnApplication extends AbstractSpringSaturnApplication {

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
			SpringApplication.exit(applicationContext);
			applicationContext = null;
		}
	}

	/**
	 * If the SpringApplication defaults aren’t to your taste, you can instead customize it
	 * @return the running ApplicationContext
	 */
	protected ApplicationContext run() {
		return SpringApplication.run(source());
	}

	/**
	 * If you use the SpringApplication defaults, maybe you could override this method to load the source
	 * @return the source to load
	 */
	protected Object source() {
		return this.getClass();
	}

}
