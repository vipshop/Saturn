package com.vip.saturn.job.basic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JobTypeManager {

	private static final ConcurrentMap<String, JobType> jobTypeMap = new ConcurrentHashMap<>();

	public static void register(JobType jobType) {
		jobTypeMap.put(jobType.getName(), jobType);
	}

	public static JobType get(String name) {
		return jobTypeMap.get(name);
	}

}
