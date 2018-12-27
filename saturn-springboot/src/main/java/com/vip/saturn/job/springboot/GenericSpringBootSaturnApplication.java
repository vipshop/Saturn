package com.vip.saturn.job.springboot;

import com.vip.saturn.job.spring.AbstractSpringSaturnApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

/**
 * 通用的用于SpringBoot的SaturnApplication，默认提供启动SpringBoot的方式，即SpringApplication.run(source())。
 *
 * <p>你也可以通过重写{@link #run()}和{@link #source()}方法，来自定义启动SpringBoot。
 *
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
	 * 启动SpringBoot，默认启动方式为SpringApplication.run(source())，其中source()可查看{@link #source()}方法
	 */
	protected ApplicationContext run() {
		return SpringApplication.run(source());
	}

	/**
	 * 使用默认方式启动SpringBoot时，加载的source
	 */
	protected Object source() {
		return this.getClass();
	}

}
