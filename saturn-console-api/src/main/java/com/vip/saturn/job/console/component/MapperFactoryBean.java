package com.vip.saturn.job.console.component;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobInfo;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author chembo.huang
 */
@Component
public class MapperFactoryBean implements FactoryBean<MapperFactory> {

	@Override
	public MapperFactory getObject() throws Exception {
		MapperFactory factory = new DefaultMapperFactory.Builder().build();
		factory.classMap(JobConfig.class, JobConfig4DB.class).mapNulls(false).byDefault().register();
		factory.classMap(JobConfig.class, JobConfig.class).mapNulls(false).byDefault().register();
		factory.classMap(JobConfig4DB.class, JobInfo.class).byDefault().register();
		return factory;
	}

	@Override
	public Class<?> getObjectType() {
		return MapperFactory.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
