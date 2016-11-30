package com.vip.saturn.it.job;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

public class SimpleJavaJob extends AbstractSaturnJavaJob {
	public static Map<String,Integer> statusMap = new HashMap<String,Integer>();
	
	private static synchronized void countInc(String key){
		Integer status = statusMap.get(key);
		int count = 0;
		if(status != null){
			count = status;
		}
		count++;
		statusMap.put(key, count);
	}
	
	
	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,  SaturnJobExecutionContext shardingContext) {
		String key = jobName+"_"+shardItem;
		System.out.println(new Date() + " running:"+jobName+"; "+shardItem +";"+ shardParam);
		countInc(key);
		return new SaturnJobReturn();
	}
}
