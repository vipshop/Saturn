/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.basic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作业注册表.
 * @author dylan.xue
 */
public class JobRegistry {

	private static Map<String, ConcurrentHashMap<String, JobScheduler>> SCHEDULER_MAP = new ConcurrentHashMap<>();

	private static ConcurrentHashMap<String,Object> JOB_BUSINESS_INSTANCE_MAP = new ConcurrentHashMap<String, Object>();

	private JobRegistry() {
	}

	public static Map<String, ConcurrentHashMap<String, JobScheduler>> getSchedulerMap() {
		return SCHEDULER_MAP;
	}
	
	/**
	 * 添加作业控制器.
	 * 
	 */
	public static void addJobScheduler(final String executorName, final String jobName, final JobScheduler jobScheduler) {
		if (SCHEDULER_MAP.containsKey(executorName)) {
			SCHEDULER_MAP.get(executorName).put(jobName, jobScheduler);
		} else {
			ConcurrentHashMap<String, JobScheduler> schedMap = new ConcurrentHashMap<>();
			schedMap.put(jobName, jobScheduler);
			SCHEDULER_MAP.put(executorName, schedMap);
		}
	}

	public static void clearExecutor(String executorName){
		SCHEDULER_MAP.remove(executorName);
	}
	
	public static void clearJob(String executorName,String jobName) {
		Map<String, JobScheduler> scedMap =  SCHEDULER_MAP.get(executorName);
		if (scedMap != null) {
			JobScheduler jobScheduler = scedMap.remove(jobName);
			if(jobScheduler != null && jobScheduler.getJob()!=null){
				jobScheduler.getJob().shutdown();
			}
		}
	}

	private static String getKey(String executorName, String jobName) {
		return new StringBuilder(100).append(executorName).append('_').append(jobName).toString();
	}

	public static void addJobBusinessInstance(String executorName, String jobName, Object jobBusinessInstance) {
		JOB_BUSINESS_INSTANCE_MAP.putIfAbsent(getKey(executorName, jobName), jobBusinessInstance);
	}

	public static Object getJobBusinessInstance(String executorName, String jobName) {
		return JOB_BUSINESS_INSTANCE_MAP.get(getKey(executorName, jobName));
	}
}
