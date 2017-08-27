package com.vip.saturn.demo.utils;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

public class SpringFactory {

	private static SpringFactory instance = new SpringFactory();

	public static SpringFactory getInstance() {
		return instance;
	}

	private BeanFactory factory;

	public Object getObject(String beanId) {
		return factory.getBean(beanId);
	}

	private SpringFactory() {
		List<Resource> resources = new ArrayList<Resource>();

		resources.add(new ClassPathResource("applicationContext-saturn-job.xml"));

		Resource[] resourceArrays = new Resource[resources.size()];
		try {
			ApplicationContext context = new MySpringApplicationContext(resources.toArray(resourceArrays));
			factory = (BeanFactory) context;
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}

	}

}
