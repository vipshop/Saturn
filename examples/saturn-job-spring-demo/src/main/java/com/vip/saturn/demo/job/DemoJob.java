package com.vip.saturn.demo.job;

import com.vip.saturn.demo.service.DemoService;
import com.vip.saturn.demo.utils.SpringFactory;
import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

/**
 * 方法1： 不使用autowired，job类不在spring中配置
 */
public class DemoJob extends AbstractSaturnJavaJob {

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {

		DemoService demoService = (DemoService) SpringFactory.getInstance().getObject("demoService");
		demoService.execute();

		System.out.println("我会出现在运行日志里.running handleJavaJob:" + jobName + "; " + shardItem + ";" + shardParam);
		return new SaturnJobReturn("我是分片" + shardItem + "的处理结果");
	}
}
