package com.vip.saturn.demo.example;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

public class DemoJob extends AbstractSaturnJavaJob {

	@Override
	public SaturnJobReturn handleJavaJob(final String jobName,final  Integer shardItem,final  String shardParam,final  SaturnJobExecutionContext context) {
		System.out.println("我会出现在运行日志里.running handleJavaJob:"+jobName+"; "+shardItem +";"+ shardParam);
		return new SaturnJobReturn("我是分片"+shardItem+"的处理结果");
	}


}
