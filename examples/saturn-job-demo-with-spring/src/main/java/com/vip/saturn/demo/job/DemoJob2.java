package com.vip.saturn.demo.job;

import org.springframework.beans.factory.annotation.Autowired;

import com.vip.saturn.demo.service.DemoService;
import com.vip.saturn.demo.utils.SpringFactory;
import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

/**
 * 方法2： 使用autowired，job类在spring中配置
 */
public class DemoJob2 extends AbstractSaturnJavaJob {

	@Autowired
	private DemoService demoService;

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		
		DemoJob2 instance = (DemoJob2) SpringFactory.getInstance().getObject("demoJob2");

		return instance._handleJavaJob(jobName, shardItem, shardParam, shardingContext);
	}
	
	public SaturnJobReturn _handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {

		demoService.execute();
		System.out.println("我会出现在运行日志里.running handleJavaJob:" + jobName + "; " + shardItem + ";" + shardParam);
		return new SaturnJobReturn("我是分片" + shardItem + "的处理结果");
	}

}
