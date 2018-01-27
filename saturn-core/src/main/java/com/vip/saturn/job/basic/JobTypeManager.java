package com.vip.saturn.job.basic;

import java.util.HashMap;
import java.util.Map;

public class JobTypeManager {

	private static JobTypeManager instance = new JobTypeManager();
	
	private Map<String, Class<? extends AbstractElasticJob>> handlerMap = new HashMap<String, Class<? extends AbstractElasticJob>>();

	private JobTypeManager() {
	}

	public static JobTypeManager getInstance() {
		return instance;
	}

	public void registerHandler(String jobType, Class<? extends AbstractElasticJob> jobClazz) {
		handlerMap.put(jobType, jobClazz);
	}

	public Class<? extends AbstractElasticJob> getHandler(String jobType) {
		return handlerMap.get(jobType);
	}
}
