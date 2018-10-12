package com.vip.saturn.demo;

import com.vip.saturn.job.springboot.GenericSpringBootSaturnApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Application extends GenericSpringBootSaturnApplication {

	/**
	 * If the SpringApplication defaults aren’t to your taste, you can instead customize it
	 * @return the running ApplicationContext
	 */
	@Override
	protected ApplicationContext run() {
		return super.run();
	}

	/**
	 * If you use the SpringApplication defaults, maybe you could override this method to load the source
	 * @return the source to load
	 */
	@Override
	protected Object source() {
		return super.source();
	}

}
