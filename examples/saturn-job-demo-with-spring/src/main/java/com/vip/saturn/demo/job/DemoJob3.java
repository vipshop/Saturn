package com.vip.saturn.demo.job;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.vip.saturn.demo.service.DemoService;
import com.vip.saturn.demo.utils.SpringFactory;
import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

/**
 * 方法3： 使用autowired，支持预热，仅限saturn 2.0.0以上使用!
 */
public class DemoJob3 extends AbstractSaturnJavaJob {

	@Autowired
	private DemoService demoService;

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		demoService.execute();
		System.out.println(
				new Date() + ";我会出现在运行日志里.running handleJavaJob:" + jobName + "; " + shardItem + ";" + shardParam);
		return new SaturnJobReturn("我是分片" + shardItem + "的处理结果");
	}

	/**
	 * 这是个静态方法，在executor初始化时会调用，并生成供saturn使用的实现类对象
	 */
	public static Object getObject() {
		DemoJob3 instance = (DemoJob3) SpringFactory.getInstance().getObject("demoJob3");
		return instance;
	}
}
