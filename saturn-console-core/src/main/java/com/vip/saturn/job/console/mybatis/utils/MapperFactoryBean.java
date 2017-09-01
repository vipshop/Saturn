/**
 * 
 */
package com.vip.saturn.job.console.mybatis.utils;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobSettings;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.entity.HistoryJobConfig;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author chembo.huang
 *
 */
@Component
public class MapperFactoryBean implements FactoryBean<MapperFactory> {

	@Override
	public MapperFactory getObject() throws Exception {
		MapperFactory factory = new DefaultMapperFactory.Builder().build();
		factory.classMap(JobSettings.class, CurrentJobConfig.class).mapNulls(false).byDefault().register();
		factory.classMap(JobConfig.class, CurrentJobConfig.class).mapNulls(false).byDefault().register();
		factory.classMap(CurrentJobConfig.class, HistoryJobConfig.class).mapNulls(false).byDefault().register();
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
